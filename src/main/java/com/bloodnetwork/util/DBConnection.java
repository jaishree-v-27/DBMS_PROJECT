package com.bloodnetwork.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Singleton class responsible for managing the JDBC database connection.
 * 
 * It reads configuration properties (URL, username, password) dynamically
 * from the 'db.properties' file so credentials are never hardcoded.
 */
public class DBConnection {

    // The single instance of DBConnection (Singleton pattern)
    private static DBConnection instance;

    // Database connection parameters loaded from properties file
    private String url;
    private String username;
    private String password;

    /**
     * Private constructor to enforce Singleton pattern.
     * Loads database credentials from db.properties.
     */
    private DBConnection() {
        loadProperties();
    }

    /**
     * Loads database connection settings from 'db.properties'.
     * Searches both the application classpath and the file system.
     */
    private void loadProperties() {
        Properties props = new Properties();
        InputStream input = null;

        try {
            // 1. First, attempt to load from classpath (standard for Maven / packaged apps)
            input = getClass().getClassLoader().getResourceAsStream("db.properties");

            // 2. If not found on classpath, attempt relative file paths (for IDE or javac runs)
            if (input == null) {
                File file = new File("src/main/resources/db.properties");
                if (file.exists()) {
                    input = new FileInputStream(file);
                } else {
                    File rootFile = new File("db.properties");
                    if (rootFile.exists()) {
                        input = new FileInputStream(rootFile);
                    }
                }
            }

            if (input == null) {
                throw new RuntimeException("Configuration file 'db.properties' not found! "
                        + "Please ensure 'src/main/resources/db.properties' exists.");
            }

            props.load(input);

            this.url = props.getProperty("db.url");
            this.username = props.getProperty("db.user");
            this.password = props.getProperty("db.password");

            // Ensure MySQL JDBC driver is registered
            Class.forName("com.mysql.cj.jdbc.Driver");

        } catch (IOException e) {
            throw new RuntimeException("Failed to read db.properties file: " + e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found. Please ensure mysql-connector-j JAR is in the classpath.", e);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ignored) {}
            }
        }
    }

    /**
     * Returns the singleton instance of DBConnection.
     * Uses synchronized keyword to ensure thread-safe lazy initialization.
     *
     * @return DBConnection singleton instance
     */
    public static synchronized DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        }
        return instance;
    }

    /**
     * Obtains an active JDBC Connection using the loaded credentials.
     * Each call returns a fresh connection suitable for independent operations
     * or dedicated transaction boundaries.
     *
     * @return java.sql.Connection object
     * @throws SQLException if a database access error occurs or credentials are wrong
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }
}
