package net.noscape.project.supremetags.storage;

import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.handlers.Tag;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class TagData {

    public static void createTag(Tag tag) {
        if (SupremeTags.getInstance().isMySQL() || SupremeTags.getInstance().isMaria()) {
            SupremeTags.getInstance().getMySQLTags().saveTag(tag);
        } else if (SupremeTags.getInstance().isSQLite()) {
            SupremeTags.getInstance().getSqLiteTags().saveTag(tag);
        }
        if (SupremeTags.getInstance().getRedisUpdateService() != null) {
            SupremeTags.getInstance().getRedisUpdateService().publishTagCreated(tag);
        }
    }

    public static void deleteTag(String identifier) {
        if (SupremeTags.getInstance().isMySQL() || SupremeTags.getInstance().isMaria()) {
            SupremeTags.getInstance().getMySQLTags().deleteTag(identifier);
        } else if (SupremeTags.getInstance().isSQLite()) {
            SupremeTags.getInstance().getSqLiteTags().deleteTag(identifier);
        }
        if (SupremeTags.getInstance().getRedisUpdateService() != null) {
            SupremeTags.getInstance().getRedisUpdateService().publishTagDeleted(identifier);
        }
    }

    public static void updateTag(Tag tag) {
        if (SupremeTags.getInstance().isMySQL() || SupremeTags.getInstance().isMaria()) {
            SupremeTags.getInstance().getMySQLTags().updateTag(tag);
        } else if (SupremeTags.getInstance().isSQLite()) {
            SupremeTags.getInstance().getSqLiteTags().updateTag(tag);
        }
        if (SupremeTags.getInstance().getRedisUpdateService() != null) {
            SupremeTags.getInstance().getRedisUpdateService().publishTagUpdated(tag);
        }
    }

    public static Tag getTag(String identifier) {

        return null;
    }

    public static Map<String, Tag> getAllTags() {
        if (SupremeTags.getInstance().isMySQL() || SupremeTags.getInstance().isMaria()) {
            return SupremeTags.getInstance().getMySQLTags().loadTags();
        } else if (SupremeTags.getInstance().isSQLite()) {
            return SupremeTags.getInstance().getSqLiteTags().loadTags();
        }

        return new LinkedHashMap<>();
    }

    public static long getTagDataVersion() {
        if (SupremeTags.getInstance().isMySQL() || SupremeTags.getInstance().isMaria()) {
            return SupremeTags.getInstance().getMySQLTags().getDataVersion();
        } else if (SupremeTags.getInstance().isSQLite()) {
            return SupremeTags.getInstance().getSqLiteTags().getDataVersion();
        }

        return 0L;
    }

    public static boolean isConnected() {
        try {
            if (SupremeTags.getInstance().isMySQL() || SupremeTags.getInstance().isMaria()) {
                return !SupremeTags.getMysql().getConnection().isClosed();
            } else if (SupremeTags.getInstance().isSQLite()) {
                return !SupremeTags.getSQLite().getConnection().isClosed();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return false;
    }
}
