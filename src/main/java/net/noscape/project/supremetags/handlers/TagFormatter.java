package net.noscape.project.supremetags.handlers;

import me.clip.placeholderapi.PlaceholderAPI;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.storage.UserData;
import org.bukkit.entity.Player;

import java.util.UUID;

import static net.noscape.project.supremetags.utils.Utils.format;
import static net.noscape.project.supremetags.utils.Utils.replacePlaceholders;

public final class TagFormatter {

    public enum Context {
        TAG("tag"),
        CHAT("chat"),
        TAB("tab"),
        SCOREBOARD("scoreboard");

        private final String path;

        Context(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }
    }

    public static String getFormattedTag(Player player, Context context) {
        if (player == null) return "";

        UUID uuid = player.getUniqueId();
        SupremeTags plugin = SupremeTags.getInstance();

        String active = UserData.getActive(uuid);

        // None selected -> Custom Tag
        if (active == null || active.isBlank() || active.equalsIgnoreCase("None")) {
            String custom = UserData.getCustomTag(uuid);

            if (custom != null && !custom.isBlank()) {
                return applyFormat(player, context, custom);
            }

            return plugin.getConfig().getString(
                    "placeholders." + context.getPath() + ".none-output",
                    ""
            );
        }

        String tagText = null;

        Tag tag = plugin.getTagManager().getTag(active);

        if (tag != null) {
            tagText = tag.getCurrentTag() != null
                    ? tag.getCurrentTag()
                    : tag.getTag().getFirst();
        }

        if (tagText == null) {
            Tag personal = plugin.getPlayerManager()
                    .loadAllPlayerTags(uuid)
                    .get(active);

            if (personal != null) {
                tagText = personal.getCurrentTag() != null
                        ? personal.getCurrentTag()
                        : personal.getTag().getFirst();
            }
        }

        if (tagText == null) {
            Variant variant = plugin.getTagManager().getVariantTag(player);

            if (variant != null) {
                tagText = variant.getTag().getFirst();
            }
        }

        if (tagText == null) {
            return plugin.getConfig().getString(
                    "placeholders." + context.getPath() + ".none-output",
                    ""
            );
        }

        return applyFormat(player, context, tagText);
    }

    private static String applyFormat(Player player, Context context, String tag) {
        SupremeTags plugin = SupremeTags.getInstance();

        String format = plugin.getConfig().getString(
                "placeholders." + context.getPath() + ".format",
                "%tag%"
        );

        tag = replacePlaceholders(player, tag);

        return PlaceholderAPI.setPlaceholders(
                player,
                format(format.replace("%tag%", tag))
        );
    }
}