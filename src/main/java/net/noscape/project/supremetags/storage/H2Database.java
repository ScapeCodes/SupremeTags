package net.noscape.project.supremetags.storage;

import net.noscape.project.supremetags.SupremeTags;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class H2Database {

    private final String ConnectionURL;

    public H2Database(String connectionURL) {
        this.ConnectionURL = connectionURL;
        this.initialiseDatabase();
    }

    public Connection getConnection() {
        Connection connection = null;

        try {
            Class.forName("org.h2.Driver");
            connection = DriverManager.getConnection(SupremeTags.getConnectionURL());
        } catch (SQLException | ClassNotFoundException throwables) {
            throwables.printStackTrace();
            SupremeTags.getInstance().getLogger().warning("------------------------------");
            SupremeTags.getInstance().getLogger().warning("H2: Could not connect to database.");
            SupremeTags.getInstance().getLogger().warning("------------------------------");
        }

        return connection;
    }

    public void initialiseDatabase() {
        try (Connection connection = getConnection()) {

            if (connection == null) {
                SupremeTags.getInstance().getLogger().warning("Failed to initialize H2: Connection is null");
                return;
            }

            // USERS TABLE (unchanged)
            String userTable = "CREATE TABLE IF NOT EXISTS users (" +
                    "Name VARCHAR(255) NOT NULL, " +
                    "UUID VARCHAR(255) NOT NULL, " +
                    "Active VARCHAR(255) NOT NULL, " +
                    "PRIMARY KEY (UUID)" +
                    ")";
            connection.prepareStatement(userTable).executeUpdate();

            // SINGLE TAGS TABLE
            String tagsTable = "CREATE TABLE IF NOT EXISTS tags (" +
                    "identifier VARCHAR(255) PRIMARY KEY, " +
                    "tag_text TEXT, " +
                    "category VARCHAR(255), " +
                    "permission VARCHAR(255), " +
                    "description TEXT, " +
                    "rarity VARCHAR(100), " +
                    "order_num INT, " +
                    "is_withdrawable BOOLEAN, " +
                    "eco_enabled BOOLEAN, " +
                    "eco_type VARCHAR(100), " +
                    "eco_amount DOUBLE, " +
                    "effects TEXT, " +
                    "variants TEXT, " +
                    "custom_placeholders TEXT, " +
                    "economy TEXT" +
                    ")";
            connection.prepareStatement(tagsTable).executeUpdate();

            SupremeTags.getInstance().getLogger().info("✅ H2 tables (users, tags) initialized successfully.");

        } catch (SQLException e) {
            e.printStackTrace();
            SupremeTags.getInstance().getLogger().warning("⚠️ Error while initializing H2 tables: " + e.getMessage());
        }
    }

    public String getConnectionURL() {
        return ConnectionURL;
    }
}
