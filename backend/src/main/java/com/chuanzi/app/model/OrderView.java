package com.chuanzi.app.model;

import java.util.List;

public final class OrderView {
    private final long id;
    private final long userId;
    private final String customerUsername;
    private final String customerDisplayName;
    private final int totalCents;
    private final String status;
    private final String createdAt;
    private final String updatedAt;
    private final String confirmedAt;
    private final String doneAt;
    private final String cancelledAt;
    private final List<OrderItemView> items;

    public OrderView(
        long id,
        long userId,
        String customerUsername,
        String customerDisplayName,
        int totalCents,
        String status,
        String createdAt,
        String updatedAt,
        String confirmedAt,
        String doneAt,
        String cancelledAt,
        List<OrderItemView> items
    ) {
        this.id = id;
        this.userId = userId;
        this.customerUsername = customerUsername;
        this.customerDisplayName = customerDisplayName;
        this.totalCents = totalCents;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.confirmedAt = confirmedAt;
        this.doneAt = doneAt;
        this.cancelledAt = cancelledAt;
        this.items = items;
    }

    public long id() {
        return id;
    }

    public long userId() {
        return userId;
    }

    public String customerUsername() {
        return customerUsername;
    }

    public String customerDisplayName() {
        return customerDisplayName;
    }

    public int totalCents() {
        return totalCents;
    }

    public String status() {
        return status;
    }

    public String createdAt() {
        return createdAt;
    }

    public String updatedAt() {
        return updatedAt;
    }

    public String confirmedAt() {
        return confirmedAt;
    }

    public String doneAt() {
        return doneAt;
    }

    public String cancelledAt() {
        return cancelledAt;
    }

    public List<OrderItemView> items() {
        return items;
    }
}
