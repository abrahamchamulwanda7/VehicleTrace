package com.vehicletrace;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class Database {

    private static final String URL;
    private static final String USER;
    private static final String PASSWORD;

    // Reads db.properties once when the class is first used
    static {
        Properties props = new Properties();
        try (InputStream in = Database.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new RuntimeException("db.properties not found in src/main/resources");
            }
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Could not read db.properties", e);
        }
        URL = props.getProperty("db.url");
        USER = props.getProperty("db.user");
        PASSWORD = props.getProperty("db.password");
    }

    // Opens a new connection to MariaDB
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}