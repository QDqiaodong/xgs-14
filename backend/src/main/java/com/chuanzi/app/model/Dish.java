package com.chuanzi.app.model;

public final class Dish {
    private final long id;
    private final String name;
    private final int priceCents;
    private final String description;
    private final boolean available;
    private final int maxQuantityPerOrder;
    private final String createdAt;
    private final String updatedAt;

    public Dish(long id, String name, int priceCents, String description, boolean isAvailable, int maxQuantityPerOrder, String createdAt, String updatedAt) {
        this.id = id;
        this.name = name;
        this.priceCents = priceCents;
        this.description = description;
        this.available = isAvailable;
        this.maxQuantityPerOrder = maxQuantityPerOrder;
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

    public String createdAt() {
        return createdAt;
    }

    public String updatedAt() {
        return updatedAt;
    }
}
