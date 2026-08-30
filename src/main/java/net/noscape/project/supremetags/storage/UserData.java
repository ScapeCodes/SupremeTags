package net.noscape.project.supremetags.storage;

import net.noscape.project.supremetags.*;
import net.noscape.project.supremetags.redis.RedisUpdateService;
import net.noscape.project.supremetags.storage.user.H2UserData;
import net.noscape.project.supremetags.storage.user.MySQLUserData;
import net.noscape.project.supremetags.storage.user.SQLiteUserData;
import net.noscape.project.supremetags.utils.Utils;
import org.bukkit.*;
import org.bukkit.entity.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

public class UserData {

    public static void createPlayer(Player player) {
        if (SupremeTags.getInstance().isH2()) {
            SupremeTags.getInstance().getUserData().createPlayer(player);
        } else if (SupremeTags.getInstance().isMySQL() || SupremeTags.getInstance().isMaria()) {
            SupremeTags.getInstance().getUser().createPlayer(player);
        } else if (SupremeTags.getInstance().isSQLite()) {
            SupremeTags.getInstance().getSQLiteUser().createPlayer(player);
        }
    }

    public static void setActive(OfflinePlayer player, String identifier) {
        final String activeIdentifier = normalizeActive(identifier);
        String previous = getActive(player.getUniqueId());
        if (SupremeTags.getInstance().isH2()) {
            H2UserData.setActive(player, activeIdentifier);
        } else if (SupremeTags.getInstance().isMySQL() || SupremeTags.getInstance().isMaria()) {
            MySQLUserData.setActive(player, activeIdentifier);
        } else if (SupremeTags.getInstance().isSQLite()) {
            SQLiteUserData.setActive(player, activeIdentifier);
        }

        if (SupremeTags.getInstance().getTagStatisticsManager() != null) {
            SupremeTags.getInstance().getTagStatisticsManager().recordActiveChange(player, previous, activeIdentifier);
        }

        publishRedisUpdate(redis -> redis.publishPlayerActive(player.getUniqueId(), activeIdentifier));

        if (player.isOnline() && player.getPlayer() != null && SupremeTags.getInstance().getAutoApplyManager() != null) {
            Player onlinePlayer = player.getPlayer();
            Utils.runMain(() -> SupremeTags.getInstance().getAutoApplyManager().apply(onlinePlayer, activeIdentifier));
        }
    }

    public static void setActiveManual(OfflinePlayer player, String identifier) {
        String activeIdentifier = normalizeActive(identifier);
        if (SupremeTags.getInstance().isMySQL() || SupremeTags.getInstance().isMaria()) {
            MySQLUserData.setActiveManual(player, activeIdentifier);
        }
        publishRedisUpdate(redis -> redis.publishPlayerActive(player.getUniqueId(), activeIdentifier));
    }

    public static int setActiveForEveryone(String identifier) {
        final String activeIdentifier = normalizeActive(identifier);
        int updated = 0;

        if (SupremeTags.getInstance().isH2()) {
            updated = updateAllActive(SupremeTags.getH2Database().getConnection(), activeIdentifier);
        } else if (SupremeTags.getInstance().isMySQL() || SupremeTags.getInstance().isMaria()) {
            try (Connection connection = SupremeTags.getMysql().getConnection()) {
                updated = updateAllActive(connection, activeIdentifier);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else if (SupremeTags.getInstance().isSQLite()) {
            updated = updateAllActive(SupremeTags.getSQLite().getConnection(), activeIdentifier);
        }

        if (SupremeTags.getInstance().getDataCache() != null) {
            SupremeTags.getInstance().getDataCache().clearCache();
        }

        if (SupremeTags.getInstance().getAutoApplyManager() != null) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                Utils.runMain(() -> SupremeTags.getInstance().getAutoApplyManager().apply(onlinePlayer, activeIdentifier));
            }
        }

        publishRedisUpdate(redis -> redis.publishPlayerActiveForEveryone(activeIdentifier));

        return updated;
    }

    private static int updateAllActive(Connection connection, String identifier) {
        if (connection == null) {
            return 0;
        }

        try (PreparedStatement statement = connection.prepareStatement("UPDATE `users` SET Active=?")) {
            statement.setString(1, identifier);
            return statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static boolean isConnected() {
        if (SupremeTags.getInstance().isH2()) {
            try {
                if (!SupremeTags.getH2Database().getConnection().isClosed()) {
                    return true;
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } else if (SupremeTags.getInstance().isMySQL() || SupremeTags.getInstance().isMaria()) {
            try {
                if (!SupremeTags.getMysql().getConnection().isClosed()) {
                    return true;
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } else if (SupremeTags.getInstance().isSQLite()) {
            try {
                if (!SupremeTags.getSQLite().getConnection().isClosed()) {
                    return true;
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        return false;
    }

    public static void setCustomTag(OfflinePlayer player, String tag) {
        if (SupremeTags.getInstance().isH2()) {
            H2UserData.setCustomTag(player, tag);
        } else if (SupremeTags.getInstance().isMySQL() || SupremeTags.getInstance().isMaria()) {
            MySQLUserData.setCustomTag(player, tag);
        } else if (SupremeTags.getInstance().isSQLite()) {
            SQLiteUserData.setCustomTag(player, tag);
        }
        publishRedisUpdate(redis -> redis.publishPlayerCustomTag(player.getUniqueId(), tag));
    }

    public static String getCustomTag(UUID uuid) {

        if (SupremeTags.getInstance().isH2()) {
            return H2UserData.getCustomTag(uuid);
        } else if (SupremeTags.getInstance().isMySQL() || SupremeTags.getInstance().isMaria()) {
            return MySQLUserData.getCustomTag(uuid);
        } else if (SupremeTags.getInstance().isSQLite()) {
            return SQLiteUserData.getCustomTag(uuid);
        }

        return "";
    }

    public static String getActive(UUID uuid) {
        String active = "";

        if (SupremeTags.getInstance().isH2()) {
            active = H2UserData.getActive(uuid);
        } else if (SupremeTags.getInstance().isMySQL() || SupremeTags.getInstance().isMaria()) {
            active = MySQLUserData.getActive(uuid);
        } else if (SupremeTags.getInstance().isSQLite()) {
            active = SQLiteUserData.getActive(uuid);
        }

        return normalizeActive(active);
    }

    public static List<String> getFavourites(UUID uuid) {
        if (SupremeTags.getInstance().isH2()) {
            return H2UserData.getFavourites(uuid);
        } else if (SupremeTags.getInstance().isMySQL() || SupremeTags.getInstance().isMaria()) {
            return MySQLUserData.getFavourites(uuid);
        } else if (SupremeTags.getInstance().isSQLite()) {
            return SQLiteUserData.getFavourites(uuid);
        }

        return new ArrayList<>();
    }

    public static void setFavourites(OfflinePlayer player, List<String> favourites) {
        if (SupremeTags.getInstance().isH2()) {
            H2UserData.setFavourites(player, favourites);
        } else if (SupremeTags.getInstance().isMySQL() || SupremeTags.getInstance().isMaria()) {
            MySQLUserData.setFavourites(player, favourites);
        } else if (SupremeTags.getInstance().isSQLite()) {
            SQLiteUserData.setFavourites(player, favourites);
        }
        publishRedisUpdate(redis -> redis.publishPlayerFavourites(player.getUniqueId(), favourites));
    }

    public static List<String> getUnlockedTags(UUID uuid) {
        if (SupremeTags.getInstance().isH2()) {
            return H2UserData.getUnlockedTags(uuid);
        } else if (SupremeTags.getInstance().isMySQL() || SupremeTags.getInstance().isMaria()) {
            return MySQLUserData.getUnlockedTags(uuid);
        } else if (SupremeTags.getInstance().isSQLite()) {
            return SQLiteUserData.getUnlockedTags(uuid);
        }

        return new ArrayList<>();
    }

    public static void setUnlockedTags(OfflinePlayer player, List<String> unlockedTags) {
        if (SupremeTags.getInstance().isH2()) {
            H2UserData.setUnlockedTags(player, unlockedTags);
        } else if (SupremeTags.getInstance().isMySQL() || SupremeTags.getInstance().isMaria()) {
            MySQLUserData.setUnlockedTags(player, unlockedTags);
        } else if (SupremeTags.getInstance().isSQLite()) {
            SQLiteUserData.setUnlockedTags(player, unlockedTags);
        }
        publishRedisUpdate(redis -> redis.publishPlayerUnlockedTags(player.getUniqueId(), unlockedTags));
    }

    public static boolean hasUnlockedTag(UUID uuid, String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return false;
        }

        return getUnlockedTags(uuid).stream().anyMatch(unlocked -> unlocked.equalsIgnoreCase(identifier));
    }

    public static void addUnlockedTag(OfflinePlayer player, String identifier) {
        if (player == null || identifier == null || identifier.isBlank() || hasUnlockedTag(player.getUniqueId(), identifier)) {
            return;
        }

        List<String> unlockedTags = getUnlockedTags(player.getUniqueId());
        unlockedTags.add(identifier);
        setUnlockedTags(player, unlockedTags);
    }

    public static long getTagCredits(UUID uuid) {
        long credits = 0L;

        if (SupremeTags.getInstance().isH2()) {
            credits = H2UserData.getTagCredits(uuid);
        } else if (SupremeTags.getInstance().isMySQL() || SupremeTags.getInstance().isMaria()) {
            credits = MySQLUserData.getTagCredits(uuid);
        } else if (SupremeTags.getInstance().isSQLite()) {
            credits = SQLiteUserData.getTagCredits(uuid);
        }

        if (uuid != null && SupremeTags.getInstance().getDataCache() != null) {
            SupremeTags.getInstance().getDataCache().cacheData("tagcredits_" + uuid, String.valueOf(credits));
        }

        return credits;
    }

    public static long getDisplayTagCredits(UUID uuid) {
        if (uuid == null) {
            return 0L;
        }

        if (Bukkit.isPrimaryThread() && (SupremeTags.getInstance().isMySQL() || SupremeTags.getInstance().isMaria())) {
            String cachedData = SupremeTags.getInstance().getDataCache() != null
                    ? SupremeTags.getInstance().getDataCache().getCachedData("tagcredits_" + uuid)
                    : null;

            if (cachedData != null) {
                try {
                    return Long.parseLong(cachedData);
                } catch (NumberFormatException ignored) {
                }
            }

            return SupremeTags.getInstance().getConfig()
                    .getLong("settings.personal-tags.credits.starting-balance", 0L);
        }

        return getTagCredits(uuid);
    }

    public static void setTagCredits(OfflinePlayer player, long credits) {
        long safeCredits = Math.max(0L, credits);
        if (SupremeTags.getInstance().isH2()) {
            H2UserData.setTagCredits(player, safeCredits);
        } else if (SupremeTags.getInstance().isMySQL() || SupremeTags.getInstance().isMaria()) {
            MySQLUserData.setTagCredits(player, safeCredits);
        } else if (SupremeTags.getInstance().isSQLite()) {
            SQLiteUserData.setTagCredits(player, safeCredits);
        }
        if (SupremeTags.getInstance().getDataCache() != null) {
            SupremeTags.getInstance().getDataCache().cacheData("tagcredits_" + player.getUniqueId(), String.valueOf(safeCredits));
        }
        publishRedisUpdate(redis -> redis.publishPlayerTagCredits(player.getUniqueId(), safeCredits));
    }

    public static void addTagCredits(OfflinePlayer player, long credits) {
        if (credits <= 0L) return;
        setTagCredits(player, getTagCredits(player.getUniqueId()) + credits);
    }

    public static boolean takeTagCredits(OfflinePlayer player, long credits) {
        if (credits <= 0L) return true;
        long currentCredits = getTagCredits(player.getUniqueId());
        if (currentCredits < credits) return false;
        setTagCredits(player, currentCredits - credits);
        return true;
    }

    private static String normalizeActive(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return "None";
        }

        return identifier;
    }

    private static void publishRedisUpdate(java.util.function.Consumer<RedisUpdateService> publisher) {
        RedisUpdateService redis = SupremeTags.getInstance().getRedisUpdateService();
        if (redis != null) {
            publisher.accept(redis);
        }
    }
}
