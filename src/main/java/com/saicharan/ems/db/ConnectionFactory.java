package com.saicharan.ems.db;

import com.saicharan.ems.exception.DataAccessException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Creates JDBC connections. Settings come from db.properties and can be
 * overridden with the DB_URL, DB_USER and DB_PASSWORD environment variables.
 */
public class ConnectionFactory {

    private final String url;
    private final String user;
    private final String password;

    public ConnectionFactory(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public static ConnectionFactory fromEnvironment() {
        Properties props = new Properties();
        try (InputStream in = ConnectionFactory.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new DataAccessException("Could not read db.properties", e);
        }
        return new ConnectionFactory(
                env("DB_URL", props.getProperty("db.url")),
                env("DB_USER", props.getProperty("db.user")),
                env("DB_PASSWORD", props.getProperty("db.password", "")));
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public String getUrl() {
        return url;
    }

    /** Runs schema.sql so the employees table exists. Safe to call repeatedly. */
    public void initSchema() {
        String sql;
        try (InputStream in = ConnectionFactory.class.getClassLoader().getResourceAsStream("schema.sql")) {
            if (in == null) {
                throw new DataAccessException("schema.sql not found on classpath", null);
            }
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DataAccessException("Could not read schema.sql", e);
        }
        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            for (String statement : sql.split(";")) {
                if (!statement.isBlank()) {
                    st.execute(statement);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not initialise the database schema", e);
        }
    }
}
