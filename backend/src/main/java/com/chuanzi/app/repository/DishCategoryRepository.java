package com.chuanzi.app.repository;

import com.chuanzi.app.db.Database;
import com.chuanzi.app.model.DishCategory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DishCategoryRepository {
    private final Database database;

    public DishCategoryRepository(Database database) {
        this.database = database;
    }

    public List<DishCategory> listCategories(Boolean isActive) {
        StringBuilder sql = new StringBuilder(
            "SELECT id, code, name, sort_order, is_active, created_at, updated_at FROM dish_categories WHERE 1 = 1"
        );
        List<Object> params = new ArrayList<>();
        if (isActive != null) {
            sql.append(" AND is_active = ?");
            params.add(isActive ? 1 : 0);
        }
        sql.append(" ORDER BY sort_order ASC, id ASC");

        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<DishCategory> categories = new ArrayList<>();
                while (rs.next()) {
                    categories.add(map(rs));
                }
                return categories;
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询菜品分类失败", e);
        }
    }

    public Optional<DishCategory> findById(long id) {
        String sql = "SELECT id, code, name, sort_order, is_active, created_at, updated_at FROM dish_categories WHERE id = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询菜品分类失败", e);
        }
    }

    public Optional<DishCategory> findByCode(String code) {
        String sql = "SELECT id, code, name, sort_order, is_active, created_at, updated_at FROM dish_categories WHERE code = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询菜品分类失败", e);
        }
    }

    public long create(String code, String name, int sortOrder, boolean isActive) {
        String sql = "INSERT INTO dish_categories(code, name, sort_order, is_active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)";
        LocalDateTime now = LocalDateTime.now();
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, code);
            ps.setString(2, name);
            ps.setInt(3, sortOrder);
            ps.setInt(4, isActive ? 1 : 0);
            ps.setObject(5, now);
            ps.setObject(6, now);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
            throw new RuntimeException("新增菜品分类失败，未返回主键");
        } catch (SQLException e) {
            throw new RuntimeException("新增菜品分类失败", e);
        }
    }

    public boolean update(long id, String code, String name, int sortOrder, boolean isActive) {
        String sql = "UPDATE dish_categories SET code = ?, name = ?, sort_order = ?, is_active = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setString(2, name);
            ps.setInt(3, sortOrder);
            ps.setInt(4, isActive ? 1 : 0);
            ps.setObject(5, LocalDateTime.now());
            ps.setLong(6, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("更新菜品分类失败", e);
        }
    }

    public boolean hasDishes(long categoryId) {
        String sql = "SELECT COUNT(1) AS cnt FROM dishes WHERE category_id = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("cnt") > 0;
                }
                return false;
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询分类菜品引用失败", e);
        }
    }

    public boolean delete(long id) {
        String sql = "DELETE FROM dish_categories WHERE id = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("删除菜品分类失败", e);
        }
    }

    public Map<String, Object> toDto(DishCategory category) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", category.id());
        dto.put("code", category.code());
        dto.put("name", category.name());
        dto.put("sortOrder", category.sortOrder());
        dto.put("isActive", category.isActive());
        dto.put("createdAt", category.createdAt());
        dto.put("updatedAt", category.updatedAt());
        return dto;
    }

    private DishCategory map(ResultSet rs) throws SQLException {
        return new DishCategory(
            rs.getLong("id"),
            rs.getString("code"),
            rs.getString("name"),
            rs.getInt("sort_order"),
            rs.getInt("is_active") == 1,
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
