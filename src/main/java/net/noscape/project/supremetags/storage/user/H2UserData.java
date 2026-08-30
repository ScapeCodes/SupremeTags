package net.noscape.project.supremetags.storage.user;

import net.noscape.project.supremetags.*;
import org.bukkit.*;
import org.bukkit.entity.*;

import java.sql.*;
import java.util.*;

public class H2UserData {

    public boolean exists(Player player) {
        try {
            PreparedStatement statement = SupremeTags.getH2Database().getConnection().prepareStatement("SELECT * FROM `users` WHERE (UUID=?)");
            statement.setString(1, player.getUniqueId().toString());
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void createPlayer(Player player) {
        if (exists(player)) {
            return;
        }

        String defaultTag = SupremeTags.getInstance().getConfig().getString("settings.default-tag", "None");
        long startingCredits = SupremeTags.getInstance().getConfig().getLong("settings.personal-tags.credits.starting-balance", 0L);

        try (PreparedStatement statement = SupremeTags.getH2Database().getConnection().prepareStatement(
                "MERGE INTO users (Name, UUID, Active, TagCredits) KEY(UUID) VALUES (?,?,?,?)")) {
            statement.setString(1, player.getName());
            statement.setString(2, player.getUniqueId().toString());
            statement.setString(3, defaultTag);
            statement.setLong(4, startingCredits);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (SupremeTags.getInstance().isDataCache()) {
            SupremeTags.getInstance().getDataCache().removeFromCache(player.getUniqueId().toString());
            SupremeTags.getInstance().getDataCache().cacheData(player.getUniqueId().toString(), defaultTag);
        }
    }

    public static void setActive(OfflinePlayer player, String identifier) {
        String sql = "UPDATE `users` SET Active=? WHERE (UUID=?)";

        try (PreparedStatement statement = SupremeTags.getH2Database().getConnection().prepareStatement(sql)) {
            statement.setString(1, identifier);
            statement.setString(2, player.getUniqueId().toString());
            statement.executeUpdate();

            SupremeTags.getInstance().getDataCache().removeFromCache(player.getUniqueId().toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String getActive(UUID uuid) {

        String cachedData = SupremeTags.getInstance().getDataCache().getCachedData(uuid.toString());

        if (cachedData != null) {
            return cachedData;
        }

        String query = "SELECT Active FROM users WHERE UUID=?";
        String value = "";

        try (Connection connection = SupremeTags.getH2Database().getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, uuid.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    value = resultSet.getString("Active");

                    SupremeTags.getInstance().getDataCache().cacheData(uuid.toString(), value);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return value;
    }

    public static List<String> getFavourites(UUID uuid) {

        String cachedData = SupremeTags.getInstance().getDataCache().getCachedData("favourites_" + uuid.toString());

        if (cachedData != null) {

            return deserializeFavourites(cachedData);
        }

        String query = "SELECT Favourites FROM users WHERE UUID=?";
        String value = "";

        try (Connection connection = SupremeTags.getH2Database().getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, uuid.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    value = resultSet.getString("Favourites");

                    SupremeTags.getInstance().getDataCache().cacheData("favourites_" + uuid.toString(), value != null ? value : "");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return deserializeFavourites(value);
    }

    public static void setFavourites(OfflinePlayer player, List<String> favourites) {
        String favouritesData = serializeFavourites(favourites);
        String sql = "UPDATE `users` SET Favourites=? WHERE (UUID=?)";

        try (PreparedStatement statement = SupremeTags.getH2Database().getConnection().prepareStatement(sql)) {
            statement.setString(1, favouritesData);
            statement.setString(2, player.getUniqueId().toString());
            statement.executeUpdate();

            SupremeTags.getInstance().getDataCache().removeFromCache("favourites_" + player.getUniqueId().toString());
            SupremeTags.getInstance().getDataCache().cacheData("favourites_" + player.getUniqueId().toString(), favouritesData);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<String> getUnlockedTags(UUID uuid) {
        String cacheKey = "unlockedtags_" + uuid;
        String cachedData = SupremeTags.getInstance().getDataCache().getCachedData(cacheKey);

        if (cachedData != null) {
            return deserializeList(cachedData);
        }

        String query = "SELECT UnlockedTags FROM users WHERE UUID=?";
        String value = "";

        try (Connection connection = SupremeTags.getH2Database().getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, uuid.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    value = resultSet.getString("UnlockedTags");
                    SupremeTags.getInstance().getDataCache().cacheData(cacheKey, value != null ? value : "");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return deserializeList(value);
    }

    public static void setUnlockedTags(OfflinePlayer player, List<String> unlockedTags) {
        String unlockedTagsData = serializeList(unlockedTags);
        String sql = "UPDATE `users` SET UnlockedTags=? WHERE (UUID=?)";
        String cacheKey = "unlockedtags_" + player.getUniqueId();

        try (PreparedStatement statement = SupremeTags.getH2Database().getConnection().prepareStatement(sql)) {
            statement.setString(1, unlockedTagsData);
            statement.setString(2, player.getUniqueId().toString());
            statement.executeUpdate();

            SupremeTags.getInstance().getDataCache().removeFromCache(cacheKey);
            SupremeTags.getInstance().getDataCache().cacheData(cacheKey, unlockedTagsData);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void setCustomTag(OfflinePlayer player, String tag) {
        String sql = "UPDATE `users` SET CustomTag=? WHERE (UUID=?)";

        String cacheKey = "customtag_" + player.getUniqueId().toString();

        try (PreparedStatement statement = SupremeTags.getH2Database().getConnection().prepareStatement(sql)) {
            statement.setString(1, tag);
            statement.setString(2, player.getUniqueId().toString());
            statement.executeUpdate();

            SupremeTags.getInstance().getDataCache().removeFromCache(cacheKey);
            SupremeTags.getInstance().getDataCache().cacheData(cacheKey, tag != null ? tag : "");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String getCustomTag(UUID uuid) {

        String cacheKey = "customtag_" + uuid.toString();
        String cachedData = SupremeTags.getInstance().getDataCache().getCachedData(cacheKey);

        if (cachedData != null) {

            return cachedData;
        }

        String query = "SELECT CustomTag FROM users WHERE UUID=?";
        String value = "";

        try (Connection connection = SupremeTags.getH2Database().getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, uuid.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    value = resultSet.getString("CustomTag");

                    SupremeTags.getInstance().getDataCache().cacheData(cacheKey, value);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return value;
    }

    public static long getTagCredits(UUID uuid) {
        String cacheKey = "tagcredits_" + uuid;
        String cachedData = SupremeTags.getInstance().getDataCache().getCachedData(cacheKey);

        if (cachedData != null) {
            try {
                return Long.parseLong(cachedData);
            } catch (NumberFormatException ignored) {
            }
        }

        String query = "SELECT TagCredits FROM users WHERE UUID=?";
        long value = 0L;

        try (Connection connection = SupremeTags.getH2Database().getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, uuid.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    value = resultSet.getLong("TagCredits");
                    SupremeTags.getInstance().getDataCache().cacheData(cacheKey, String.valueOf(value));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return value;
    }

    public static void setTagCredits(OfflinePlayer player, long credits) {
        String sql = "UPDATE users SET TagCredits=? WHERE (UUID=?)";
        String cacheKey = "tagcredits_" + player.getUniqueId();

        try (PreparedStatement statement = SupremeTags.getH2Database().getConnection().prepareStatement(sql)) {
            statement.setLong(1, credits);
            statement.setString(2, player.getUniqueId().toString());
            statement.executeUpdate();

            SupremeTags.getInstance().getDataCache().removeFromCache(cacheKey);
            SupremeTags.getInstance().getDataCache().cacheData(cacheKey, String.valueOf(credits));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static String serializeFavourites(List<String> favourites) {
        return serializeList(favourites);
    }

    private static List<String> deserializeFavourites(String data) {
        return deserializeList(data);
    }

    private static String serializeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return String.join(",", values);
    }

    private static List<String> deserializeList(String data) {
        if (data == null || data.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(data.split(",")));
    }

}
