package com.chuanzi.app.model;

public final class OrderItemView {
    private final long id;
    private final long orderId;
    private final long dishId;
    private final String dishNameSnapshot;
    private final int priceCentsSnapshot;
    private final int quantity;

    public OrderItemView(long id, long orderId, long dishId, String dishNameSnapshot, int priceCentsSnapshot, int quantity) {
        this.id = id;
        this.orderId = orderId;
        this.dishId = dishId;
        this.dishNameSnapshot = dishNameSnapshot;
        this.priceCentsSnapshot = priceCentsSnapshot;
        this.quantity = quantity;
    }

    public long id() {
        return id;
    }

    public long orderId() {
        return orderId;
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
