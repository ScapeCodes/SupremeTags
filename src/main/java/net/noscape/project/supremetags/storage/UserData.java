package net.noscape.project.supremetags.storage;

import net.noscape.project.supremetags.*;
import net.noscape.project.supremetags.storage.user.H2UserData;
import net.noscape.project.supremetags.storage.user.MySQLUserData;
import net.noscape.project.supremetags.storage.user.SQLiteUserData;
import org.bukkit.*;
import org.bukkit.entity.*;

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
        if (SupremeTags.getInstance().isH2()) {
            H2UserData.setActive(player, identifier);
        } else if (SupremeTags.getInstance().isMySQL() || SupremeTags.getInstance().isMaria()) {
            MySQLUserData.setActive(player, identifier);
        } else if (SupremeTags.getInstance().isSQLite()) {
            SQLiteUserData.setActive(player, identifier);
        }
    }

    public static void setActiveManual(OfflinePlayer player, String identifier) {
        if (SupremeTags.getInstance().isMySQL() || SupremeTags.getInstance().isMaria()) {
            MySQLUserData.setActiveManual(player, identifier);
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

        if (SupremeTags.getInstance().isH2()) {
            return H2UserData.getActive(uuid);
        } else if (SupremeTags.getInstance().isMySQL() || SupremeTags.getInstance().isMaria()) {
            return MySQLUserData.getActive(uuid);
        } else if (SupremeTags.getInstance().isSQLite()) {
            return SQLiteUserData.getActive(uuid);
        }

        return "";
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
    }
}
