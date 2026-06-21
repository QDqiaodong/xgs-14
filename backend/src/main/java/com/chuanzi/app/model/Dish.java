package com.chuanzi.app.model;

public final class Dish {
    private final long id;
    private final String name;
    private final int priceCents;
    private final String description;
    private final boolean available;
    private final int maxQuantityPerOrder;
    private final Long categoryId;
    private final String categoryCode;
    private final String categoryName;
    private final Integer categorySortOrder;
    private final String createdAt;
    private final String updatedAt;

    public Dish(
        long id,
        String name,
        int priceCents,
        String description,
        boolean isAvailable,
        int maxQuantityPerOrder,
        Long categoryId,
        String categoryCode,
        String categoryName,
        Integer categorySortOrder,
        String createdAt,
        String updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.priceCents = priceCents;
        this.description = description;
        this.available = isAvailable;
        this.maxQuantityPerOrder = maxQuantityPerOrder;
        this.categoryId = categoryId;
        this.categoryCode = categoryCode;
        this.categoryName = categoryName;
        this.categorySortOrder = categorySortOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public int priceCents() {
        return priceCents;
    }

    public String description() {
        return description;
    }

    public boolean isAvailable() {
        return available;
    }

    public int maxQuantityPerOrder() {
        return maxQuantityPerOrder;
    }

    public Long categoryId() {
        return categoryId;
    }

    public String categoryCode() {
        return categoryCode;
    }

    public String categoryName() {
        return categoryName;
    }

    public Integer categorySortOrder() {
        return categorySortOrder;
    }

    public String createdAt() {
        return createdAt;
    }

    public String updatedAt() {
        return updatedAt;
    }
}
