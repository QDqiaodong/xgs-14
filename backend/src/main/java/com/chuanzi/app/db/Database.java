package com.chuanzi.app.db;

import com.chuanzi.app.config.AppConfig;
import org.flywaydb.core.Flyway;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
    private final AppConfig config;

    public Database(AppConfig config) {
        this.config = config;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("MySQL JDBC 驱动未找到", e);
        }
        migrate();
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(config.jdbcUrl(), config.dbUser(), config.dbPassword());
    }

    private void migrate() {
        Flyway.configure()
            .dataSource(config.jdbcUrl(), config.dbUser(), config.dbPassword())
            .baselineOnMigrate(true)
            .baselineVersion("0")
            .locations("classpath:db/migration")
            .load()
            .migrate();
    }
}
