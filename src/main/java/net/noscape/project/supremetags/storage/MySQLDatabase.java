package net.noscape.project.supremetags.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.noscape.project.supremetags.SupremeTags;
import org.jetbrains.annotations.NotNull;

import java.sql.*;

public class MySQLDatabase {

    protected final HikariConfig config = new HikariConfig();
    protected final HikariDataSource ds;

    private boolean isConnected = false;
    private volatile boolean userTableSchemaEnsured = false;

    public MySQLDatabase(String host, int port, String database, String username, String password, boolean useSSL) {
        config.setIdleTimeout(SupremeTags.getInstance().getConfigManager().getConfig("data.yml").get().getInt("data.mysql-pool-settings.timeouts.idle"));
        config.setMaxLifetime(SupremeTags.getInstance().getConfigManager().getConfig("data.yml").get().getInt("data.mysql-pool-settings.timeouts.max-lifetime"));
        config.setConnectionTimeout(SupremeTags.getInstance().getConfigManager().getConfig("data.yml").get().getInt("data.mysql-pool-settings.timeouts.connection"));
        config.setMinimumIdle(SupremeTags.getInstance().getConfigManager().getConfig("data.yml").get().getInt("data.mysql-pool-settings.minimum-idle"));
        config.setRegisterMbeans(true);
        config.setMaximumPoolSize(SupremeTags.getInstance().getConfigManager().getConfig("data.yml").get().getInt("data.mysql-pool-settings.maximum-pool-size"));

        config.setConnectionTestQuery("SELECT 1");
        config.setDataSourceClassName("com.mysql.cj.jdbc.MysqlDataSource");
        config.addDataSourceProperty("serverName", host);
        config.addDataSourceProperty("port", port);
        config.addDataSourceProperty("databaseName", database);
        config.addDataSourceProperty("user", username);
        config.addDataSourceProperty("password", password);

        config.addDataSourceProperty("characterEncoding", "utf8");

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useSSL", useSSL);
        config.addDataSourceProperty("allowPublicKeyRetrieval", "true");

        ds = new HikariDataSource(this.config);

        this.connect();
        this.createTable();
    }

    public void executeQuery(String query, Object... parameters) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = ds.getConnection();

            preparedStatement = prepareStatement(query, parameters);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeConnections(preparedStatement, connection, null);
        }
    }

    public void closeConnections(PreparedStatement preparedStatement, Connection connection, ResultSet resultSet) {
        try {
            if (resultSet != null) {
                resultSet.close();
            }

            if (preparedStatement != null) {
                preparedStatement.close();
            }

            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void openConnection() {
        try (Connection connection = this.getConnection()) {
            if (!connection.isClosed() && connection.isValid(2)) {
                this.isConnected = true;
            }
        } catch (SQLException e) {
            SupremeTags.getInstance().getLogger().warning("MYSQL: Something went wrong with connecting to the MySQL database.\n" + e);
        }
    }

    public void connect() {
        openConnection();
    }

    public void createTable() {
        String userTable = "CREATE TABLE IF NOT EXISTS `users` (Name VARCHAR(255) NOT NULL, UUID VARCHAR(255) NOT NULL, Active VARCHAR(255) NOT NULL, Favourites TEXT, CustomTag TEXT, UnlockedTags TEXT, TagCredits BIGINT NOT NULL DEFAULT 0, PRIMARY KEY (UUID))";

        try (Connection connection = getConnection();
             Statement stmt = connection.createStatement()) {
            stmt.execute(userTable);
            ensureUserTableSchema(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void ensureUserTableSchema() {
        if (userTableSchemaEnsured) {
            return;
        }

        try (Connection connection = getConnection()) {
            ensureUserTableSchema(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private synchronized void ensureUserTableSchema(Connection connection) throws SQLException {
        if (userTableSchemaEnsured) {
            return;
        }

        ensureActiveColumn(connection);
        ensureCTColumn(connection);
        ensureUnlockedTagsColumn(connection);
        ensureTagCreditsColumn(connection);
        userTableSchemaEnsured = true;
    }

    private void ensureActiveColumn(Connection connection) throws SQLException {
        if (hasColumn(connection, "Active")) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement("ALTER TABLE `users` ADD COLUMN Active VARCHAR(255) NOT NULL DEFAULT ''")) {
            statement.executeUpdate();
        }
    }

    private void ensureCTColumn(Connection connection) throws SQLException {
        if (hasColumn(connection, "CustomTag")) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement("ALTER TABLE `users` ADD COLUMN CustomTag TEXT")) {
            statement.executeUpdate();
        }
    }

    private void ensureUnlockedTagsColumn(Connection connection) throws SQLException {
        if (hasColumn(connection, "UnlockedTags")) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement("ALTER TABLE `users` ADD COLUMN UnlockedTags TEXT")) {
            statement.executeUpdate();
        }
    }

    private void ensureTagCreditsColumn(Connection connection) throws SQLException {
        if (hasColumn(connection, "TagCredits")) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement("ALTER TABLE `users` ADD COLUMN TagCredits BIGINT NOT NULL DEFAULT 0")) {
            statement.executeUpdate();
        }
    }

    private boolean hasColumn(Connection connection, String columnName) throws SQLException {
        String query = "SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() " +
                "AND LOWER(TABLE_NAME) = LOWER(?) " +
                "AND LOWER(COLUMN_NAME) = LOWER(?) " +
                "LIMIT 1";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, "users");
            statement.setString(2, columnName);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public void disconnect() {
        if (!ds.isClosed()) {
            ds.close();
        }

        this.isConnected = false;
    }

    private PreparedStatement prepareStatement(String query, Object... parameters) throws SQLException {
        PreparedStatement preparedStatement = getConnection().prepareStatement(query);
        for (int i = 0; i < parameters.length; i++) {
            preparedStatement.setObject(i + 1, parameters[i]);
        }
        return preparedStatement;
    }

    @NotNull
    public final Connection getConnection() throws SQLException {
        return this.ds.getConnection();
    }
}
