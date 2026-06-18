package com.chuanzi.app.model;

public final class OrderCreateItem {
    private final long dishId;
    private final String dishNameSnapshot;
    private final int priceCentsSnapshot;
    private final int quantity;

    public OrderCreateItem(long dishId, String dishNameSnapshot, int priceCentsSnapshot, int quantity) {
        this.dishId = dishId;
        this.dishNameSnapshot = dishNameSnapshot;
        this.priceCentsSnapshot = priceCentsSnapshot;
        this.quantity = quantity;
    }

    public long dishId() {
        return dishId;
    }

    public String dishNameSnapshot() {
        return dishNameSnapshot;
    }

    public int priceCentsSnapshot() {
        return priceCentsSnapshot;
    }

    public int quantity() {
        return quantity;
    }
}
