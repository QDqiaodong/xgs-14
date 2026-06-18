package com.chuanzi.app.config;

public final class AppConfig {
    private final int appPort;
    private final String dbHost;
    private final int dbPort;
    private final String dbName;
    private final String dbUser;
    private final String dbPassword;
    private final int sessionTtlHours;
    private final String passwordSalt;
    private final String webRoot;

    public AppConfig(
        int appPort,
        String dbHost,
        int dbPort,
        String dbName,
        String dbUser,
        String dbPassword,
        int sessionTtlHours,
        String passwordSalt,
        String webRoot
    ) {
        this.appPort = appPort;
        this.dbHost = dbHost;
        this.dbPort = dbPort;
        this.dbName = dbName;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.sessionTtlHours = sessionTtlHours;
        this.passwordSalt = passwordSalt;
        this.webRoot = webRoot;
    }

    public static AppConfig fromEnv() {
        return new AppConfig(
            envInt("APP_PORT", 8080),
            envStr("DB_HOST", "127.0.0.1"),
            envInt("DB_PORT", 3306),
            envStr("DB_NAME", "chuanzi"),
            envStr("DB_USER", "root"),
            envStr("DB_PASSWORD", "root"),
            envInt("SESSION_TTL_HOURS", 24),
            envStr("APP_PASSWORD_SALT", "chuanzi-default-salt"),
            envStr("WEB_ROOT", "web")
        );
    }

    public String jdbcUrl() {
        return "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName
            + "?useUnicode=true&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true";
    }

    public int appPort() {
        return appPort;
    }

    public String dbHost() {
        return dbHost;
    }

    public int dbPort() {
        return dbPort;
    }

    public String dbName() {
        return dbName;
    }

    public String dbUser() {
        return dbUser;
    }

    public String dbPassword() {
        return dbPassword;
    }

    public int sessionTtlHours() {
        return sessionTtlHours;
    }

    public String passwordSalt() {
        return passwordSalt;
    }

    public String webRoot() {
        return webRoot;
    }

    private static String envStr(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static int envInt(String key, int defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }
}
