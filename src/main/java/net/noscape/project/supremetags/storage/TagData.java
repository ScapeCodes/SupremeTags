package net.noscape.project.supremetags.storage;

import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.handlers.Tag;

import java.sql.SQLException;
import java.util.List;

public class TagData {

    // -------------------------------------------------------
    // CREATE TAG
    // -------------------------------------------------------
    public static void createTag(Tag tag) {
        if (SupremeTags.getInstance().isMySQL() || SupremeTags.getInstance().isMaria()) {
            SupremeTags.getInstance().getMySQLTags().saveTag(tag);
        } else if (SupremeTags.getInstance().isSQLite()) {
            SupremeTags.getInstance().getSqLiteTags().saveTag(tag);
        }
    }

    // -------------------------------------------------------
    // DELETE TAG
    // -------------------------------------------------------
    public static void deleteTag(String identifier) {
        if (SupremeTags.getInstance().isMySQL() || SupremeTags.getInstance().isMaria()) {
            SupremeTags.getInstance().getSqLiteTags().deleteTag(identifier);
        } else if (SupremeTags.getInstance().isSQLite()) {
            SupremeTags.getInstance().getSqLiteTags().deleteTag(identifier);
        }
    }

    // -------------------------------------------------------
    // UPDATE TAG
    // -------------------------------------------------------
    public static void updateTag(Tag tag) {
        if (SupremeTags.getInstance().isMySQL() || SupremeTags.getInstance().isMaria()) {
            SupremeTags.getInstance().getSqLiteTags().updateTag(tag);
        } else if (SupremeTags.getInstance().isSQLite()) {
            SupremeTags.getInstance().getSqLiteTags().updateTag(tag);
        }
    }

    // -------------------------------------------------------
    // GET TAG (ONE)
    // -------------------------------------------------------
    public static Tag getTag(String identifier) {
        //if (SupremeTags.getInstance().isMySQL() || SupremeTags.getInstance().isMaria()) {
        //    return SupremeTags.getInstance().getSqLiteTags().getTag(identifier);
        //} else if (SupremeTags.getInstance().isSQLite()) {
        //    return SupremeTags.getInstance().getSqLiteTags().getTag(identifier);
        //}

        return null;
    }

    // -------------------------------------------------------
    // GET ALL TAGS
    // -------------------------------------------------------
    public static void getAllTags() {
        if (SupremeTags.getInstance().isMySQL() || SupremeTags.getInstance().isMaria()) {
            SupremeTags.getInstance().getTagManager().setTagsMap(SupremeTags.getInstance().getMySQLTags().loadTags());
        } else if (SupremeTags.getInstance().isSQLite()) {
            SupremeTags.getInstance().getTagManager().setTagsMap(SupremeTags.getInstance().getSqLiteTags().loadTags());
        }
    }

    // -------------------------------------------------------
    // IS CONNECTED
    // -------------------------------------------------------
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
