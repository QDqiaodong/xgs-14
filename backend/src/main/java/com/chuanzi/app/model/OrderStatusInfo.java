package com.chuanzi.app.model;

public final class OrderStatusInfo {
    private final long userId;
    private final String status;

    public OrderStatusInfo(long userId, String status) {
        this.userId = userId;
        this.status = status;
    }

    public long userId() {
        return userId;
    }

    public String status() {
        return status;
    }
}
