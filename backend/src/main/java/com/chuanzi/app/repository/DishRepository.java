package com.chuanzi.app.repository;

import com.chuanzi.app.db.Database;
import com.chuanzi.app.model.Dish;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DishRepository {
    private static final String SELECT_COLUMNS =
        "d.id, d.name, d.price_cents, d.description, d.is_available, d.max_quantity_per_order, " +
        "d.category_id, c.code AS category_code, c.name AS category_name, c.sort_order AS category_sort_order, " +
        "d.created_at, d.updated_at";

    private final Database database;

    public DishRepository(Database database) {
        this.database = database;
    }

    public List<Dish> listDishes(boolean includeUnavailable, String keyword, Boolean isAvailable) {
        StringBuilder sql = new StringBuilder(
            "SELECT " + SELECT_COLUMNS + " FROM dishes d LEFT JOIN dish_categories c ON c.id = d.category_id WHERE 1 = 1"
        );
        List<Object> params = new ArrayList<>();
        if (!includeUnavailable) {
            sql.append(" AND d.is_available = 1");
        } else if (isAvailable != null) {
            sql.append(" AND d.is_available = ?");
            params.add(isAvailable ? 1 : 0);
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (d.name LIKE ? OR d.description LIKE ?)");
            String likeKeyword = "%" + keyword.trim() + "%";
            params.add(likeKeyword);
            params.add(likeKeyword);
        }
        sql.append(" ORDER BY d.id DESC");

        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<Dish> dishes = new ArrayList<>();
                while (rs.next()) {
                    dishes.add(mapDish(rs));
                }
                return dishes;
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询菜品失败", e);
        }
    }

    public Map<Long, Dish> findByIds(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                placeholders.append(",");
            }
            placeholders.append("?");
        }
        String sql = "SELECT " + SELECT_COLUMNS + " FROM dishes d LEFT JOIN dish_categories c ON c.id = d.category_id WHERE d.id IN (" + placeholders + ")";

        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = 1;
            for (Long id : ids) {
                ps.setLong(index++, id);
            }
            try (ResultSet rs = ps.executeQuery()) {
                Map<Long, Dish> map = new HashMap<>();
                while (rs.next()) {
                    Dish dish = mapDish(rs);
                    map.put(dish.id(), dish);
                }
                return map;
            }
        } catch (SQLException e) {
            throw new RuntimeException("批量查询菜品失败", e);
        }
    }

    public long createDish(String name, int priceCents, String description, boolean isAvailable, int maxQuantityPerOrder, Long categoryId) {
        String sql = "INSERT INTO dishes(name, price_cents, description, is_available, max_quantity_per_order, category_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        LocalDateTime now = LocalDateTime.now();
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setInt(2, priceCents);
            ps.setString(3, description);
            ps.setInt(4, isAvailable ? 1 : 0);
            ps.setInt(5, maxQuantityPerOrder);
            if (categoryId == null) {
                ps.setNull(6, java.sql.Types.BIGINT);
            } else {
                ps.setLong(6, categoryId);
            }
            ps.setObject(7, now);
            ps.setObject(8, now);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
            throw new RuntimeException("新增菜品失败，未返回主键");
        } catch (SQLException e) {
            throw new RuntimeException("新增菜品失败", e);
        }
    }

    public boolean updateDish(long dishId, String name, int priceCents, String description, boolean isAvailable, int maxQuantityPerOrder, Long categoryId) {
        String sql = "UPDATE dishes SET name = ?, price_cents = ?, description = ?, is_available = ?, max_quantity_per_order = ?, category_id = COALESCE(?, category_id), updated_at = ? WHERE id = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, priceCents);
            ps.setString(3, description);
            ps.setInt(4, isAvailable ? 1 : 0);
            ps.setInt(5, maxQuantityPerOrder);
            if (categoryId == null) {
                ps.setNull(6, java.sql.Types.BIGINT);
            } else {
                ps.setLong(6, categoryId);
            }
            ps.setObject(7, LocalDateTime.now());
            ps.setLong(8, dishId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("更新菜品失败", e);
        }
    }

    public boolean hasOrderItems(long dishId) {
        String sql = "SELECT COUNT(1) AS cnt FROM order_items WHERE dish_id = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, dishId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("cnt") > 0;
                }
                return false;
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询菜品订单引用失败", e);
        }
    }

    public boolean deleteDish(long dishId) {
        String sql = "DELETE FROM dishes WHERE id = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, dishId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("删除菜品失败", e);
        }
    }

    public Map<String, Object> toDishDto(Dish dish) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", dish.id());
        dto.put("name", dish.name());
        dto.put("priceCents", dish.priceCents());
        dto.put("description", dish.description());
        dto.put("isAvailable", dish.isAvailable());
        dto.put("maxQuantityPerOrder", dish.maxQuantityPerOrder());
        dto.put("categoryId", dish.categoryId());
        dto.put("categoryCode", dish.categoryCode());
        dto.put("categoryName", dish.categoryName());
        dto.put("categorySortOrder", dish.categorySortOrder());
        dto.put("createdAt", dish.createdAt());
        dto.put("updatedAt", dish.updatedAt());
        return dto;
    }

    private Dish mapDish(ResultSet rs) throws SQLException {
        return new Dish(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getInt("price_cents"),
            rs.getString("description"),
            rs.getInt("is_available") == 1,
            rs.getInt("max_quantity_per_order"),
            (Long) rs.getObject("category_id"),
            rs.getString("category_code"),
            rs.getString("category_name"),
            (Integer) rs.getObject("category_sort_order"),
            rs.getTimestamp("created_at").toLocalDateTime().toString(),
            rs.getTimestamp("updated_at").toLocalDateTime().toString()
        );
    }

    private void bindParams(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }
}
