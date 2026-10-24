package com.accountpro.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * MySQL connection helper.
 * Set ACCOUNTPro_DB_URL, ACCOUNTPro_DB_USER, and ACCOUNTPro_DB_PASSWORD
 * environment variables, or edit the defaults below for local development.
 */
public final class DB {
    private static final String URL = env("ACCOUNTPRO_DB_URL",
            "jdbc:mysql://localhost:3306/accountpro?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Manila");
    private static final String USER = env("ACCOUNTPRO_DB_USER", "root");
    private static final String PASSWORD = env("ACCOUNTPRO_DB_PASSWORD", "Sealthiel_2006");

    private DB() {
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
