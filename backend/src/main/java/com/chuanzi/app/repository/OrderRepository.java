package com.chuanzi.app.repository;

import com.chuanzi.app.db.Database;
import com.chuanzi.app.infra.ApiException;
import com.chuanzi.app.model.OrderCreateItem;
import com.chuanzi.app.model.OrderItemView;
import com.chuanzi.app.model.OrderStatusInfo;
import com.chuanzi.app.model.OrderView;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OrderRepository {
    private final Database database;
    private final DishDailyQuotaRepository quotaRepository;

    public OrderRepository(Database database, DishDailyQuotaRepository quotaRepository) {
        this.database = database;
        this.quotaRepository = quotaRepository;
    }

    public long createOrderWithItems(long userId, List<OrderCreateItem> items, int totalCents, LocalDate saleDate) {
        String orderSql = "INSERT INTO orders(user_id, total_cents, sale_date, status, created_at, updated_at) VALUES (?, ?, ?, 'NEW', ?, ?)";
        String itemSql = "INSERT INTO order_items(order_id, dish_id, dish_name_snapshot, price_cents_snapshot, quantity) VALUES (?, ?, ?, ?, ?)";
        LocalDateTime now = LocalDateTime.now();

        try (Connection conn = database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long orderId;
                try (PreparedStatement orderPs = conn.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)) {
                    orderPs.setLong(1, userId);
                    orderPs.setInt(2, totalCents);
                    orderPs.setObject(3, saleDate);
                    orderPs.setObject(4, now);
                    orderPs.setObject(5, now);
                    orderPs.executeUpdate();
                    try (ResultSet rs = orderPs.getGeneratedKeys()) {
                        if (!rs.next()) {
                            throw new SQLException("创建订单失败，未返回主键");
                        }
                        orderId = rs.getLong(1);
                    }
                }

                try (PreparedStatement itemPs = conn.prepareStatement(itemSql)) {
                    for (OrderCreateItem item : items) {
                        itemPs.setLong(1, orderId);
                        itemPs.setLong(2, item.dishId());
                        itemPs.setString(3, item.dishNameSnapshot());
                        itemPs.setInt(4, item.priceCentsSnapshot());
                        itemPs.setInt(5, item.quantity());
                        itemPs.addBatch();
                    }
                    itemPs.executeBatch();
                }

                for (OrderCreateItem item : items) {
                    boolean deducted = quotaRepository.deductQuantity(conn, item.dishId(), saleDate, item.quantity());
                    if (!deducted) {
                        throw ApiException.conflict("菜品「" + item.dishNameSnapshot() + "」当日可售份数不足");
                    }
                }

                conn.commit();
                return orderId;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("创建订单失败", e);
        }
    }

    public boolean cancelOrderAndRestoreQuota(long orderId) {
        try (Connection conn = database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                LocalDate orderDate;
                try (PreparedStatement ps = conn.prepareStatement("SELECT sale_date FROM orders WHERE id = ?")) {
                    ps.setLong(1, orderId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }
                        orderDate = rs.getDate("sale_date").toLocalDate();
                    }
                }

                boolean updated;
                LocalDateTime now = LocalDateTime.now();
                try (PreparedStatement ps = conn.prepareStatement("UPDATE orders SET status = 'CANCELLED', updated_at = ?, cancelled_at = ? WHERE id = ? AND status = 'NEW'")) {
                    ps.setObject(1, now);
                    ps.setObject(2, now);
                    ps.setLong(3, orderId);
                    updated = ps.executeUpdate() > 0;
                }
                if (!updated) {
                    conn.rollback();
                    return false;
                }

                List<OrderItemView> items = findItems(conn, orderId);
                for (OrderItemView item : items) {
                    quotaRepository.restoreQuantity(conn, item.dishId(), orderDate, item.quantity());
                }

                conn.commit();
                return true;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("取消订单失败", e);
        }
    }

    public List<OrderView> listOrdersForCustomer(long userId) {
        String sql = "SELECT id, user_id, total_cents, status, created_at, updated_at, confirmed_at, done_at, cancelled_at FROM orders WHERE user_id = ? ORDER BY id DESC";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<OrderView> orders = new ArrayList<>();
                List<Long> orderIds = new ArrayList<>();
                while (rs.next()) {
                    long orderId = rs.getLong("id");
                    orderIds.add(orderId);
                    orders.add(new OrderView(
                        orderId,
                        rs.getLong("user_id"),
                        null,
                        null,
                        rs.getInt("total_cents"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at").toLocalDateTime().toString(),
                        rs.getTimestamp("updated_at").toLocalDateTime().toString(),
                        timestampToString(rs.getTimestamp("confirmed_at")),
                        timestampToString(rs.getTimestamp("done_at")),
                        timestampToString(rs.getTimestamp("cancelled_at")),
                        List.of()
                    ));
                }
                return attachItems(conn, orders, orderIds);
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询顾客订单失败", e);
        }
    }

    public List<OrderView> listOrdersForMerchant(String status) {
        StringBuilder sql = new StringBuilder(
            "SELECT o.id, o.user_id, o.total_cents, o.status, o.created_at, o.updated_at, o.confirmed_at, o.done_at, o.cancelled_at, u.username, u.display_name "
                + "FROM orders o JOIN users u ON u.id = o.user_id"
        );
        boolean filterByStatus = status != null && !status.isBlank();
        if (filterByStatus) {
            sql.append(" WHERE o.status = ?");
        }
        sql.append(" ORDER BY o.id DESC");

        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            if (filterByStatus) {
                ps.setString(1, status);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<OrderView> orders = new ArrayList<>();
                List<Long> orderIds = new ArrayList<>();
                while (rs.next()) {
                    long orderId = rs.getLong("id");
                    orderIds.add(orderId);
                    orders.add(new OrderView(
                        orderId,
                        rs.getLong("user_id"),
                        rs.getString("username"),
                        rs.getString("display_name"),
                        rs.getInt("total_cents"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at").toLocalDateTime().toString(),
                        rs.getTimestamp("updated_at").toLocalDateTime().toString(),
                        timestampToString(rs.getTimestamp("confirmed_at")),
                        timestampToString(rs.getTimestamp("done_at")),
                        timestampToString(rs.getTimestamp("cancelled_at")),
                        List.of()
                    ));
                }
                return attachItems(conn, orders, orderIds);
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询商家订单失败", e);
        }
    }

    public Optional<String> findOrderStatus(long orderId) {
        String sql = "SELECT status FROM orders WHERE id = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getString("status"));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询订单状态失败", e);
        }
    }

    public Optional<OrderStatusInfo> findOrderStatusInfo(long orderId) {
        String sql = "SELECT user_id, status FROM orders WHERE id = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new OrderStatusInfo(
                        rs.getLong("user_id"),
                        rs.getString("status")
                    ));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询订单归属失败", e);
        }
    }

    public boolean updateOrderStatus(long orderId, String status) {
        LocalDateTime now = LocalDateTime.now();
        StringBuilder sql = new StringBuilder("UPDATE orders SET status = ?, updated_at = ?");
        if ("CONFIRMED".equals(status)) {
            sql.append(", confirmed_at = ?");
        } else if ("DONE".equals(status)) {
            sql.append(", done_at = ?");
        } else if ("CANCELLED".equals(status)) {
            sql.append(", cancelled_at = ?");
        }
        sql.append(" WHERE id = ?");

        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setString(1, status);
            ps.setObject(2, now);
            int idx = 3;
            if ("CONFIRMED".equals(status) || "DONE".equals(status) || "CANCELLED".equals(status)) {
                ps.setObject(idx++, now);
            }
            ps.setLong(idx, orderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("更新订单状态失败", e);
        }
    }

    public long countOrdersByUserId(long userId) {
        String sql = "SELECT COUNT(1) AS cnt FROM orders WHERE user_id = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("cnt");
                }
                return 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("统计订单失败", e);
        }
    }

    public Map<String, Object> toOrderDto(OrderView order, boolean includeCustomer) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", order.id());
        dto.put("userId", order.userId());
        if (includeCustomer) {
            dto.put("customerUsername", order.customerUsername());
            dto.put("customerDisplayName", order.customerDisplayName());
        }
        dto.put("totalCents", order.totalCents());
        dto.put("status", order.status());
        dto.put("createdAt", order.createdAt());
        dto.put("updatedAt", order.updatedAt());
        dto.put("confirmedAt", order.confirmedAt());
        dto.put("doneAt", order.doneAt());
        dto.put("cancelledAt", order.cancelledAt());

        List<Map<String, Object>> itemDtos = new ArrayList<>();
        for (OrderItemView item : order.items()) {
            Map<String, Object> itemDto = new LinkedHashMap<>();
            itemDto.put("id", item.id());
            itemDto.put("orderId", item.orderId());
            itemDto.put("dishId", item.dishId());
            itemDto.put("dishNameSnapshot", item.dishNameSnapshot());
            itemDto.put("priceCentsSnapshot", item.priceCentsSnapshot());
            itemDto.put("quantity", item.quantity());
            itemDtos.add(itemDto);
        }
        dto.put("items", itemDtos);
        return dto;
    }

    private String timestampToString(Timestamp ts) {
        if (ts == null) {
            return null;
        }
        return ts.toLocalDateTime().toString();
    }

    private List<OrderItemView> findItems(Connection conn, long orderId) throws SQLException {
        String sql = "SELECT id, order_id, dish_id, dish_name_snapshot, price_cents_snapshot, quantity FROM order_items WHERE order_id = ? ORDER BY id ASC";
        List<OrderItemView> items = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new OrderItemView(
                        rs.getLong("id"),
                        rs.getLong("order_id"),
                        rs.getLong("dish_id"),
                        rs.getString("dish_name_snapshot"),
                        rs.getInt("price_cents_snapshot"),
                        rs.getInt("quantity")
                    ));
                }
            }
        }
        return items;
    }

    private List<OrderView> attachItems(Connection conn, List<OrderView> orders, List<Long> orderIds) throws SQLException {
        if (orderIds.isEmpty()) {
            return orders;
        }
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < orderIds.size(); i++) {
            if (i > 0) {
                placeholders.append(",");
            }
            placeholders.append("?");
        }
        String sql = "SELECT id, order_id, dish_id, dish_name_snapshot, price_cents_snapshot, quantity FROM order_items WHERE order_id IN ("
            + placeholders + ") ORDER BY id ASC";
        Map<Long, List<OrderItemView>> itemsMap = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            for (Long orderId : orderIds) {
                ps.setLong(idx++, orderId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderItemView item = new OrderItemView(
                        rs.getLong("id"),
                        rs.getLong("order_id"),
                        rs.getLong("dish_id"),
                        rs.getString("dish_name_snapshot"),
                        rs.getInt("price_cents_snapshot"),
                        rs.getInt("quantity")
                    );
                    itemsMap.computeIfAbsent(item.orderId(), ignored -> new ArrayList<>()).add(item);
                }
            }
        }

        List<OrderView> merged = new ArrayList<>();
        for (OrderView order : orders) {
            merged.add(new OrderView(
                order.id(),
                order.userId(),
                order.customerUsername(),
                order.customerDisplayName(),
                order.totalCents(),
                order.status(),
                order.createdAt(),
                order.updatedAt(),
                order.confirmedAt(),
                order.doneAt(),
                order.cancelledAt(),
                itemsMap.getOrDefault(order.id(), List.of())
            ));
        }
        return merged;
    }
}
