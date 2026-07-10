package net.noscape.project.supremetags.storage;

import net.noscape.project.supremetags.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SQLiteDatabase {

    protected final String ConnectionURL;
    private Connection connection;
    private final Object connectionLock = new Object();

    public SQLiteDatabase(String ConnectionURL) {
        this.ConnectionURL = ConnectionURL;
        this.initialiseDatabase();
    }

    public Connection getConnection() {
        synchronized (connectionLock) {
            try {
                if (connection == null || connection.isClosed()) {
                    Class.forName("org.sqlite.JDBC");
                    connection = DriverManager.getConnection(ConnectionURL);
                }
            } catch (SQLException | ClassNotFoundException throwables) {
                throwables.printStackTrace();
                SupremeTags.getInstance().getLogger().info("------------------------------");
                SupremeTags.getInstance().getLogger().info("SQLite: Something wrong with connecting to SQLite database for SupremeTags, contact the developer if you see this.");
                SupremeTags.getInstance().getLogger().info("------------------------------");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return connection;
        }
    }

    public void initialiseDatabase() {
        try (Connection connection = getConnection()) {

            if (connection == null) {
                SupremeTags.getInstance().getLogger().warning("Failed to initialize SQLite: Connection is null");
                return;
            }

            // USERS TABLE
            String userTable = "CREATE TABLE IF NOT EXISTS `users` (Name TEXT NOT NULL, UUID TEXT NOT NULL, Active TEXT NOT NULL, Favourites TEXT, CustomTag TEXT DEFAULT '', PRIMARY KEY (UUID))";
            connection.prepareStatement(userTable).executeUpdate();
            ensureActiveColumn(connection);
            ensureCTColumn(connection);

            SupremeTags.getInstance().getLogger().info("SQLite tables initialized successfully.");

        } catch (SQLException e) {
            e.printStackTrace();
            SupremeTags.getInstance().getLogger().warning("Error while initializing SQLite tables: " + e.getMessage());
        }
    }

    private void ensureActiveColumn(Connection connection) throws SQLException {
        boolean hasActiveColumn = false;

        try (ResultSet columns = connection.getMetaData().getColumns(null, null, "users", "Active")) {
            hasActiveColumn = columns.next();
        }

        if (!hasActiveColumn) {
            try (PreparedStatement statement = connection.prepareStatement("ALTER TABLE `users` ADD COLUMN Active TEXT NOT NULL DEFAULT ''")) {
                statement.executeUpdate();
            }
        }
    }

    private void ensureCTColumn(Connection connection) throws SQLException {
        boolean hasCTColumn = false;

        try (ResultSet columns = connection.getMetaData().getColumns(null, null, "USERS", "CustomTag")) {
            hasCTColumn = columns.next();
        }

        if (!hasCTColumn) {
            try (PreparedStatement statement = connection.prepareStatement("ALTER TABLE users ADD COLUMN CustomTag Text DEFAULT ''")) {
                statement.executeUpdate();
            }
        }
    }

    public void disconnect() {
        synchronized (connectionLock) {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String getConnectionURL() {
        return ConnectionURL;
    }
}
