package com.chuanzi.app.service;

import com.chuanzi.app.infra.ApiException;
import com.chuanzi.app.model.AuthUser;
import com.chuanzi.app.model.DishCategory;
import com.chuanzi.app.repository.DishCategoryRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DishCategoryService {
    private final DishCategoryRepository categoryRepository;

    public DishCategoryService(DishCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Map<String, Object>> listCategories(AuthUser authUser, String isActive) {
        Boolean filter = parseActiveFilter(isActive);
        if (!"MERCHANT".equals(authUser.role())) {
            filter = true;
        }
        List<DishCategory> categories = categoryRepository.listCategories(filter);
        List<Map<String, Object>> result = new ArrayList<>();
        for (DishCategory category : categories) {
            result.add(categoryRepository.toDto(category));
        }
        return result;
    }

    public long createCategory(String code, String name, Integer sortOrder, Boolean isActive) {
        String normalizedCode = requireCode(code);
        String normalizedName = requireName(name);
        int normalizedSort = requireSortOrder(sortOrder);
        boolean active = isActive == null || isActive;
        if (categoryRepository.findByCode(normalizedCode).isPresent()) {
            throw ApiException.conflict("分类编码已存在");
        }
        return categoryRepository.create(normalizedCode, normalizedName, normalizedSort, active);
    }

    public void updateCategory(long id, String code, String name, Integer sortOrder, Boolean isActive) {
        String normalizedCode = requireCode(code);
        String normalizedName = requireName(name);
        int normalizedSort = requireSortOrder(sortOrder);
        boolean active = isActive != null && isActive;
        DishCategory existing = categoryRepository.findById(id)
            .orElseThrow(() -> ApiException.notFound("菜品分类不存在"));
        if (!existing.code().equals(normalizedCode) && categoryRepository.findByCode(normalizedCode).isPresent()) {
            throw ApiException.conflict("分类编码已存在");
        }
        boolean updated = categoryRepository.update(id, normalizedCode, normalizedName, normalizedSort, active);
        if (!updated) {
            throw ApiException.notFound("菜品分类不存在");
        }
    }

    public void deleteCategory(long id) {
        if (categoryRepository.hasDishes(id)) {
            throw ApiException.conflict("分类下存在菜品，无法删除，请先解除绑定");
        }
        boolean deleted = categoryRepository.delete(id);
        if (!deleted) {
            throw ApiException.notFound("菜品分类不存在");
        }
    }

    private String requireCode(String code) {
        if (code == null || !code.matches("^[A-Za-z0-9_-]{1,64}$")) {
            throw ApiException.badRequest("分类编码需为 1-64 位字母数字下划线或连字符");
        }
        return code;
    }

    private String requireName(String name) {
        if (name == null || name.isBlank() || name.length() > 128) {
            throw ApiException.badRequest("分类名称不能为空且长度不超过 128");
        }
        return name.trim();
    }

    private int requireSortOrder(Integer sortOrder) {
        if (sortOrder == null) {
            return 0;
        }
        if (sortOrder < 0 || sortOrder > 9999) {
            throw ApiException.badRequest("排序值需在 0-9999");
        }
        return sortOrder;
    }

    private Boolean parseActiveFilter(String isActive) {
        if (isActive == null || isActive.isBlank() || "all".equalsIgnoreCase(isActive)) {
            return null;
        }
        if ("true".equalsIgnoreCase(isActive) || "1".equals(isActive)) {
            return true;
        }
        if ("false".equalsIgnoreCase(isActive) || "0".equals(isActive)) {
            return false;
        }
        throw ApiException.badRequest("isActive 仅支持 true/false/all");
    }
}
