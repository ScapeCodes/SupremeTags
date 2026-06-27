package net.noscape.project.supremetags.storage.user;

import net.noscape.project.supremetags.*;
import net.noscape.project.supremetags.storage.UserData;
import org.bukkit.*;
import org.bukkit.entity.*;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class MySQLUserData {

    public boolean exists(Player player) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        String query = "SELECT * FROM users WHERE (UUID=?)";

        try {
            connection = SupremeTags.getMysql().getConnection();

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, player.getUniqueId().toString());
            resultSet = preparedStatement.executeQuery();

            return resultSet.next();
        } catch (SQLException e) {
            SupremeTags.getInstance().getLogger().log(Level.SEVERE, "An error occurred while checking if player exists:", e);
        } finally {
            SupremeTags.getMysql().closeConnections(preparedStatement, connection, resultSet);
        }

        return false;
    }

    public void createPlayer(Player player) {
        if (exists(player)) {
            return;
        }

        String defaultTag = SupremeTags.getInstance().getConfig().getString("settings.default-tag", "None");

        Connection connection = null;
        PreparedStatement preparedStatement = null;

        String query = "INSERT INTO users (Name, UUID, Active) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE Name = ?, Active = ?";

        try {
            connection = SupremeTags.getMysql().getConnection();

            preparedStatement = connection.prepareStatement(query);

            // Set parameters
            preparedStatement.setString(1, player.getName());
            preparedStatement.setString(2, player.getUniqueId().toString());
            preparedStatement.setString(3, defaultTag);
            preparedStatement.setString(4, player.getName());
            preparedStatement.setString(5, defaultTag);

            // Execute the update
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected == 0) {
                // No rows were affected, handle this scenario if needed
            }

            if (SupremeTags.getInstance().isDataCache()) {
                SupremeTags.getInstance().getDataCache().removeFromCache(player.getUniqueId().toString());
                SupremeTags.getInstance().getDataCache().cacheData(player.getUniqueId().toString(), defaultTag);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            SupremeTags.getMysql().closeConnections(preparedStatement, connection, null);
        }
    }



    public static void setActive(OfflinePlayer player, String identifier) {
        if (SupremeTags.getInstance().isDataCache()) {
            String cachedData = SupremeTags.getInstance().getDataCache().getCachedData(player.getUniqueId().toString());

            if (cachedData != null && cachedData.equalsIgnoreCase(identifier)) {
                return;
            }
        } else {
            String cachedData = SupremeTags.getInstance().getDataCache().getCachedData(player.getUniqueId().toString());

            if (cachedData != null && cachedData.equalsIgnoreCase(identifier)) {
                return;
            }

            SupremeTags.getInstance().getDataCache().removeFromCache(player.getUniqueId().toString());
            SupremeTags.getInstance().getDataCache().cacheData(player.getUniqueId().toString(), identifier);
            return;
        }

        Connection connection = null;
        PreparedStatement preparedStatement = null;

        String query = "INSERT INTO users (Name, UUID, Active) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE Name = ?, Active = ?";

        try {
            connection = SupremeTags.getMysql().getConnection();

            preparedStatement = connection.prepareStatement(query);

            // Set parameters
            preparedStatement.setString(1, player.getName());
            preparedStatement.setString(2, player.getUniqueId().toString());
            preparedStatement.setString(3, identifier);
            preparedStatement.setString(4, player.getName());
            preparedStatement.setString(5, identifier);

            // Execute the update
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected == 0) {
                // No rows were affected, handle this scenario if needed
            }

            SupremeTags.getInstance().getDataCache().removeFromCache(player.getUniqueId().toString());
            SupremeTags.getInstance().getDataCache().cacheData(player.getUniqueId().toString(), identifier);

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            SupremeTags.getMysql().closeConnections(preparedStatement, connection, null);
        }
    }

    public static void setActiveManual(OfflinePlayer player, String identifier) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        String query = "INSERT INTO users (Name, UUID, Active) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE Name = ?, Active = ?";

        try {
            connection = SupremeTags.getMysql().getConnection();

            preparedStatement = connection.prepareStatement(query);

            // Set parameters
            preparedStatement.setString(1, player.getName());
            preparedStatement.setString(2, player.getUniqueId().toString());
            preparedStatement.setString(3, identifier);
            preparedStatement.setString(4, player.getName());
            preparedStatement.setString(5, identifier);

            // Execute the update
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected == 0) {
                // No rows were affected, handle this scenario if needed
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            SupremeTags.getMysql().closeConnections(preparedStatement, connection, null);
        }
    }

    public static String getActive(UUID uuid) {
        if (SupremeTags.getInstance().isDataCache()) {
            String cachedData = SupremeTags.getInstance().getDataCache().getCachedData(uuid.toString());

            if (cachedData != null) {
                return cachedData;
            }
        }

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        String query = "SELECT Active FROM users WHERE UUID=?";
        String value = "";

        try {
            connection = SupremeTags.getMysql().getConnection();

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, uuid.toString());
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                value = resultSet.getString("Active");

                if (SupremeTags.getInstance().isDataCache()) {
                    SupremeTags.getInstance().getDataCache().cacheData(uuid.toString(), value);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            SupremeTags.getMysql().closeConnections(preparedStatement, connection, resultSet);
        }

        return value;
    }

    public static void setCustomTag(OfflinePlayer player, String tag) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        String query = "UPDATE users SET CustomTag=? WHERE UUID=?";

        try {
            connection = SupremeTags.getMysql().getConnection();

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, tag);
            preparedStatement.setString(2, player.getUniqueId().toString());

            preparedStatement.executeUpdate();

            if (SupremeTags.getInstance().isDataCache()) {
                SupremeTags.getInstance().getDataCache().removeFromCache("customtag_" + player.getUniqueId());
                SupremeTags.getInstance().getDataCache().cacheData("customtag_" + player.getUniqueId(), tag != null ? tag : "");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            SupremeTags.getMysql().closeConnections(preparedStatement, connection, null);
        }
    }

    public static String getCustomTag(UUID uuid) {
        if (SupremeTags.getInstance().isDataCache()) {
            String cachedData = SupremeTags.getInstance().getDataCache().getCachedData("customtag_" + uuid);

            if (cachedData != null) {
                return cachedData;
            }
        }

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        String query = "SELECT CustomTag FROM users WHERE UUID=?";
        String value = "";

        try {
            connection = SupremeTags.getMysql().getConnection();

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, uuid.toString());
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                value = resultSet.getString("CustomTag");

                if (SupremeTags.getInstance().isDataCache()) {
                    SupremeTags.getInstance().getDataCache().cacheData("customtag_" + uuid, value != null ? value : "");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            SupremeTags.getMysql().closeConnections(preparedStatement, connection, resultSet);
        }

        return value;
    }


    public static List<String> getFavourites(UUID uuid) {
        if (SupremeTags.getInstance().isDataCache()) {
            String cachedData = SupremeTags.getInstance().getDataCache().getCachedData("favourites_" + uuid.toString());

            if (cachedData != null) {
                return deserializeFavourites(cachedData);
            }
        }

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        String query = "SELECT Favourites FROM users WHERE UUID=?";
        String value = "";

        try {
            connection = SupremeTags.getMysql().getConnection();

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, uuid.toString());
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                value = resultSet.getString("Favourites");

                if (SupremeTags.getInstance().isDataCache()) {
                    SupremeTags.getInstance().getDataCache().cacheData("favourites_" + uuid.toString(), value != null ? value : "");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            SupremeTags.getMysql().closeConnections(preparedStatement, connection, resultSet);
        }

        return deserializeFavourites(value);
    }

    public static void setFavourites(OfflinePlayer player, List<String> favourites) {
        String favouritesData = serializeFavourites(favourites);
        String query = "INSERT INTO users (Name, UUID, Active, Favourites) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE Favourites = ?";

        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = SupremeTags.getMysql().getConnection();

            preparedStatement = connection.prepareStatement(query);

            // Set parameters
            preparedStatement.setString(1, player.getName());
            preparedStatement.setString(2, player.getUniqueId().toString());
            preparedStatement.setString(3, UserData.getActive(player.getUniqueId()));
            preparedStatement.setString(4, favouritesData);
            preparedStatement.setString(5, favouritesData);

            // Execute the update
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected == 0) {
                // No rows were affected, handle this scenario if needed
            }

            SupremeTags.getInstance().getDataCache().removeFromCache("favourites_" + player.getUniqueId().toString());
            SupremeTags.getInstance().getDataCache().cacheData("favourites_" + player.getUniqueId().toString(), favouritesData);

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            SupremeTags.getMysql().closeConnections(preparedStatement, connection, null);
        }
    }

    private static String serializeFavourites(List<String> favourites) {
        if (favourites == null || favourites.isEmpty()) {
            return "";
        }
        return String.join(",", favourites);
    }

    private static List<String> deserializeFavourites(String data) {
        if (data == null || data.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(data.split(",")));
    }

}