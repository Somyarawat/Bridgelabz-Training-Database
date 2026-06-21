package com.lms.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages JDBC connections to the PostgreSQL database used by the LMS application.
 *
 * Database: lms_db
 * Host: localhost:5432
 * Username: postgres
 *
 * NOTE: Update DB_PASSWORD below (and create the lms_db database) before running the app
 * if your local PostgreSQL setup differs from the assignment defaults.
 */
public class DatabaseConnection {

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/lmsproject_db";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "260124";

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("PostgreSQL JDBC Driver not found on classpath.", e);
        }
    }

    private DatabaseConnection() {
        // Utility class - prevent instantiation
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
}
