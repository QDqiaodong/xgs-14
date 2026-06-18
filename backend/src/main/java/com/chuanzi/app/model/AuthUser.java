package com.chuanzi.app.model;

public final class AuthUser {
    private final long id;
    private final String role;
    private final String username;
    private final String displayName;
    private final String phone;
    private final String sessionToken;

    public AuthUser(long id, String role, String username, String displayName, String phone, String sessionToken) {
        this.id = id;
        this.role = role;
        this.username = username;
        this.displayName = displayName;
        this.phone = phone;
        this.sessionToken = sessionToken;
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

    public String displayName() {
        return displayName;
    }

    public String phone() {
        return phone;
    }

    public String sessionToken() {
        return sessionToken;
    }
}
