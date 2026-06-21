package com.chuanzi.app.repository;

import com.chuanzi.app.db.Database;
import com.chuanzi.app.model.DishDailyQuota;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class DishDailyQuotaRepository {
    private final Database database;

    public DishDailyQuotaRepository(Database database) {
        this.database = database;
    }

    public Map<Long, DishDailyQuota> findQuotas(LocalDate date, Set<Long> dishIds) {
        if (dishIds.isEmpty()) {
            return Map.of();
        }
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < dishIds.size(); i++) {
            if (i > 0) {
                placeholders.append(",");
            }
            placeholders.append("?");
        }
        String sql = "SELECT id, dish_id, sale_date, available_quantity, sold_quantity, created_at, updated_at "
            + "FROM dish_daily_quotas WHERE sale_date = ? AND dish_id IN (" + placeholders + ")";

        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = 1;
            ps.setObject(index++, date);
            for (Long dishId : dishIds) {
                ps.setLong(index++, dishId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                Map<Long, DishDailyQuota> map = new HashMap<>();
                while (rs.next()) {
                    DishDailyQuota quota = map(rs, null);
                    map.put(quota.dishId(), quota);
                }
                return map;
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询每日可售份数失败", e);
        }
    }

    public List<DishDailyQuota> listByDate(LocalDate date) {
        String sql = "SELECT q.id, q.dish_id, d.name AS dish_name, q.sale_date, q.available_quantity, q.sold_quantity, q.created_at, q.updated_at "
            + "FROM dish_daily_quotas q JOIN dishes d ON d.id = q.dish_id WHERE q.sale_date = ? ORDER BY q.id ASC";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, date);
            try (ResultSet rs = ps.executeQuery()) {
                List<DishDailyQuota> quotas = new ArrayList<>();
                while (rs.next()) {
                    quotas.add(map(rs, "dish_name"));
                }
                return quotas;
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询每日可售份数失败", e);
        }
    }

    public Optional<DishDailyQuota> findQuota(long dishId, LocalDate date) {
        String sql = "SELECT id, dish_id, sale_date, available_quantity, sold_quantity, created_at, updated_at "
            + "FROM dish_daily_quotas WHERE dish_id = ? AND sale_date = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, dishId);
            ps.setObject(2, date);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs, null));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询每日可售份数失败", e);
        }
    }

    public void upsertAvailable(long dishId, LocalDate date, int availableQuantity) {
        String sql = "INSERT INTO dish_daily_quotas(dish_id, sale_date, available_quantity, sold_quantity, created_at, updated_at) "
            + "VALUES (?, ?, ?, 0, ?, ?) "
            + "ON DUPLICATE KEY UPDATE available_quantity = VALUES(available_quantity), updated_at = VALUES(updated_at)";
        LocalDateTime now = LocalDateTime.now();
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, dishId);
            ps.setObject(2, date);
            ps.setInt(3, availableQuantity);
            ps.setObject(4, now);
            ps.setObject(5, now);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("保存每日可售份数失败", e);
        }
    }

    public boolean deductQuantity(Connection conn, long dishId, LocalDate date, int quantity) throws SQLException {
        String updateSql = "UPDATE dish_daily_quotas SET sold_quantity = sold_quantity + ?, updated_at = ? "
            + "WHERE dish_id = ? AND sale_date = ? AND sold_quantity + ? <= available_quantity";
        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setInt(1, quantity);
            ps.setObject(2, LocalDateTime.now());
            ps.setLong(3, dishId);
            ps.setObject(4, date);
            ps.setInt(5, quantity);
            if (ps.executeUpdate() > 0) {
                return true;
            }
        }
        String existsSql = "SELECT COUNT(1) AS cnt FROM dish_daily_quotas WHERE dish_id = ? AND sale_date = ?";
        try (PreparedStatement ps = conn.prepareStatement(existsSql)) {
            ps.setLong(1, dishId);
            ps.setObject(2, date);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("cnt") == 0;
                }
            }
        }
        return true;
    }

    public void restoreQuantity(Connection conn, long dishId, LocalDate date, int quantity) throws SQLException {
        String sql = "UPDATE dish_daily_quotas SET sold_quantity = sold_quantity - ?, updated_at = ? "
            + "WHERE dish_id = ? AND sale_date = ? AND sold_quantity >= ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setObject(2, LocalDateTime.now());
            ps.setLong(3, dishId);
            ps.setObject(4, date);
            ps.setInt(5, quantity);
            ps.executeUpdate();
        }
    }

    public Map<String, Object> toDto(DishDailyQuota quota) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", quota.id());
        dto.put("dishId", quota.dishId());
        dto.put("dishName", quota.dishName());
        dto.put("saleDate", quota.saleDate() == null ? null : quota.saleDate().toString());
        dto.put("availableQuantity", quota.availableQuantity());
        dto.put("soldQuantity", quota.soldQuantity());
        dto.put("remainingQuantity", quota.remainingQuantity());
        dto.put("createdAt", quota.createdAt());
        dto.put("updatedAt", quota.updatedAt());
        return dto;
    }

    private DishDailyQuota map(ResultSet rs, String dishNameColumn) throws SQLException {
        String dishName = dishNameColumn == null ? null : rs.getString(dishNameColumn);
        return new DishDailyQuota(
            rs.getLong("id"),
            rs.getLong("dish_id"),
            dishName,
            rs.getDate("sale_date").toLocalDate(),
            rs.getInt("available_quantity"),
            rs.getInt("sold_quantity"),
            rs.getTimestamp("created_at").toLocalDateTime().toString(),
            rs.getTimestamp("updated_at").toLocalDateTime().toString()
        );
    }
}
