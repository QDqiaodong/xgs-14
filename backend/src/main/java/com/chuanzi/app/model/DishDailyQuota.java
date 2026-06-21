package com.chuanzi.app.model;

import java.time.LocalDate;

public final class DishDailyQuota {
    private final long id;
    private final long dishId;
    private final String dishName;
    private final LocalDate saleDate;
    private final int availableQuantity;
    private final int soldQuantity;
    private final String createdAt;
    private final String updatedAt;

    public DishDailyQuota(
        long id,
        long dishId,
        String dishName,
        LocalDate saleDate,
        int availableQuantity,
        int soldQuantity,
        String createdAt,
        String updatedAt
    ) {
        this.id = id;
        this.dishId = dishId;
        this.dishName = dishName;
        this.saleDate = saleDate;
        this.availableQuantity = availableQuantity;
        this.soldQuantity = soldQuantity;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long id() {
        return id;
    }

    public long dishId() {
        return dishId;
    }

    public String dishName() {
        return dishName;
    }

    public LocalDate saleDate() {
        return saleDate;
    }

    public int availableQuantity() {
        return availableQuantity;
    }

    public int soldQuantity() {
        return soldQuantity;
    }

    public int remainingQuantity() {
        return availableQuantity - soldQuantity;
    }

    public String createdAt() {
        return createdAt;
    }

    public String updatedAt() {
        return updatedAt;
    }
}
