package com.chuanzi.app.service;

import com.chuanzi.app.infra.ApiException;
import com.chuanzi.app.model.AuthUser;
import com.chuanzi.app.model.Dish;
import com.chuanzi.app.model.DishCategory;
import com.chuanzi.app.repository.DishCategoryRepository;
import com.chuanzi.app.repository.DishRepository;
import com.chuanzi.app.util.ValidationUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DishService {
    private final DishRepository dishRepository;
    private final DishCategoryRepository categoryRepository;

    public DishService(DishRepository dishRepository, DishCategoryRepository categoryRepository) {
        this.dishRepository = dishRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<Map<String, Object>> listDishes(AuthUser authUser, String scope, String keyword, String isAvailable) {
        boolean includeUnavailable = "MERCHANT".equals(authUser.role()) && "all".equalsIgnoreCase(scope);
        Boolean availabilityFilter = parseAvailabilityFilter(isAvailable, includeUnavailable);
        List<Dish> dishes = dishRepository.listDishes(includeUnavailable, normalizeKeyword(keyword), availabilityFilter);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Dish dish : dishes) {
            result.add(dishRepository.toDishDto(dish));
        }
        return result;
    }

    public long createDish(String name, Integer priceCents, String description, Boolean isAvailable, Integer maxQuantityPerOrder, Long categoryId) {
        String normalizedName = requireDishName(name);
        int normalizedPrice = ValidationUtil.requirePriceCents(priceCents);
        String normalizedDesc = normalizeDescription(description);
        boolean available = isAvailable != null && isAvailable;
        int normalizedMaxQty = requireMaxQuantityPerOrder(maxQuantityPerOrder);
        Long normalizedCategoryId = normalizeCategoryId(categoryId);
        return dishRepository.createDish(normalizedName, normalizedPrice, normalizedDesc, available, normalizedMaxQty, normalizedCategoryId);
    }

    public void updateDish(long dishId, String name, Integer priceCents, String description, Boolean isAvailable, Integer maxQuantityPerOrder, Long categoryId) {
        String normalizedName = requireDishName(name);
        int normalizedPrice = ValidationUtil.requirePriceCents(priceCents);
        String normalizedDesc = normalizeDescription(description);
        boolean available = isAvailable != null && isAvailable;
        int normalizedMaxQty = requireMaxQuantityPerOrder(maxQuantityPerOrder);
        Long normalizedCategoryId = normalizeCategoryId(categoryId);
        boolean updated = dishRepository.updateDish(dishId, normalizedName, normalizedPrice, normalizedDesc, available, normalizedMaxQty, normalizedCategoryId);
        if (!updated) {
            throw ApiException.notFound("菜品不存在");
        }
    }

    public void deleteDish(long dishId) {
        if (dishRepository.hasOrderItems(dishId)) {
            throw ApiException.conflict("菜品已有历史订单关联，无法删除，请改为下架");
        }
        boolean deleted = dishRepository.deleteDish(dishId);
        if (!deleted) {
            throw ApiException.notFound("菜品不存在");
        }
    }

    public List<Map<String, Object>> listCustomerMenu() {
        List<DishCategory> categories = categoryRepository.listCategories(true);
        List<Dish> dishes = dishRepository.listDishes(false, "", null);

        Map<Long, List<Dish>> byCategory = new LinkedHashMap<>();
        List<Dish> uncategorized = new ArrayList<>();
        for (Dish dish : dishes) {
            if (dish.categoryId() == null) {
                uncategorized.add(dish);
            } else {
                byCategory.computeIfAbsent(dish.categoryId(), ignored -> new ArrayList<>()).add(dish);
            }
        }

        List<Map<String, Object>> groups = new ArrayList<>();
        for (DishCategory category : categories) {
            List<Dish> items = byCategory.getOrDefault(category.id(), List.of());
            groups.add(buildGroup(category.id(), category.code(), category.name(), category.sortOrder(), items));
        }
        if (!uncategorized.isEmpty()) {
            groups.add(buildGroup(null, "", "未分类", Integer.MAX_VALUE, uncategorized));
        }
        return groups;
    }

    private Map<String, Object> buildGroup(Long categoryId, String code, String name, int sortOrder, List<Dish> dishes) {
        Map<String, Object> group = new LinkedHashMap<>();
        group.put("categoryId", categoryId);
        group.put("categoryCode", code);
        group.put("categoryName", name);
        group.put("sortOrder", sortOrder);
        List<Map<String, Object>> dishDtos = new ArrayList<>();
        for (Dish dish : dishes) {
            dishDtos.add(dishRepository.toDishDto(dish));
        }
        group.put("dishes", dishDtos);
        return group;
    }

    private Long normalizeCategoryId(Long categoryId) {
        if (categoryId == null || categoryId <= 0) {
            return null;
        }
        if (categoryRepository.findById(categoryId).isEmpty()) {
            throw ApiException.badRequest("菜品分类不存在");
        }
        return categoryId;
    }

    private String requireDishName(String name) {
        if (name == null || name.isBlank() || name.length() > 128) {
            throw ApiException.badRequest("菜品名称不能为空且长度不超过 128");
        }
        return name.trim();
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return "";
        }
        String normalized = description.trim();
        if (normalized.length() > 512) {
            throw ApiException.badRequest("描述长度不能超过 512");
        }
        return normalized;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return "";
        }
        return keyword.trim();
    }

    private int requireMaxQuantityPerOrder(Integer maxQuantityPerOrder) {
        int defaultValue = 10;
        if (maxQuantityPerOrder == null) {
            return defaultValue;
        }
        int value = maxQuantityPerOrder;
        if (value <= 0) {
            throw ApiException.badRequest("每单最大份数必须大于 0");
        }
        if (value > 999) {
            throw ApiException.badRequest("每单最大份数不能超过 999");
        }
        return value;
    }

    private Boolean parseAvailabilityFilter(String isAvailable, boolean includeUnavailable) {
        if (!includeUnavailable || isAvailable == null || isAvailable.isBlank() || "all".equalsIgnoreCase(isAvailable)) {
            return null;
        }
        if ("true".equalsIgnoreCase(isAvailable) || "1".equals(isAvailable)) {
            return true;
        }
        if ("false".equalsIgnoreCase(isAvailable) || "0".equals(isAvailable)) {
            return false;
        }
        throw ApiException.badRequest("isAvailable 仅支持 true/false/all");
    }
}
