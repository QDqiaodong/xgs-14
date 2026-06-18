package com.chuanzi.app.model;

public final class User {
    private final long id;
    private final String role;
    private final String username;
    private final String passwordHash;
    private final String displayName;
    private final String phone;
    private final String createdAt;
    private final String updatedAt;

    public User(
        long id,
        String role,
        String username,
        String passwordHash,
        String displayName,
        String phone,
        String createdAt,
        String updatedAt
    ) {
        this.id = id;
        this.role = role;
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.phone = phone;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long id() {
        return id;
    }

    public String role() {
        return role;
    }

    public String username() {
        return username;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public String displayName() {
        return displayName;
    }

    public String phone() {
        return phone;
    }

    public String createdAt() {
        return createdAt;
    }

    public String updatedAt() {
        return updatedAt;
    }
}
