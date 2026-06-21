package com.chuanzi.app.model;

public final class DishCategory {
    private final long id;
    private final String code;
    private final String name;
    private final int sortOrder;
    private final boolean active;
    private final String createdAt;
    private final String updatedAt;

    public DishCategory(long id, String code, String name, int sortOrder, boolean isActive, String createdAt, String updatedAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.sortOrder = sortOrder;
        this.active = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long id() {
        return id;
    }

    public String code() {
        return code;
    }

    public String name() {
        return name;
    }

    public int sortOrder() {
        return sortOrder;
    }

    public boolean isActive() {
        return active;
    }

    public String createdAt() {
        return createdAt;
    }

    public String updatedAt() {
        return updatedAt;
    }
}
