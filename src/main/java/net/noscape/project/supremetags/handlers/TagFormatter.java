package net.noscape.project.supremetags.handlers;

import net.kyori.adventure.text.Component;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.storage.UserData;
import org.bukkit.entity.Player;

import java.util.UUID;

import static net.noscape.project.supremetags.utils.Utils.deformat;
import static net.noscape.project.supremetags.utils.Utils.formatComponent;
import static net.noscape.project.supremetags.utils.Utils.replacePlaceholders;
import static net.noscape.project.supremetags.utils.Utils.toMiniMessage;

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
        String formatted = getFormattedTagText(player, context);
        if (formatted.isEmpty()) return "";

        return applyOutput(player, context, formatted);
    }

    public static Component getFormattedTagComponent(Player player, Context context) {
        return formatComponent(getFormattedTagText(player, context));
    }

    public static String getFormattedTagPlaceholder(Player player, Context context) {
        String formatted = getFormattedTagText(player, context);
        if (formatted.isEmpty()) return "";

        String output = SupremeTags.getInstance().getConfig().getString(
                "placeholders." + context.getPath() + ".output",
                "minimessage"
        );

        if (output != null && output.toLowerCase().startsWith("minimessage")) {
            return toMiniMessage(formatted).replace("<reset>", "");
        }

        return applyOutput(player, context, formatted);
    }

    private static String getFormattedTagText(Player player, Context context) {
        if (player == null) return "";

        UUID uuid = player.getUniqueId();
        SupremeTags plugin = SupremeTags.getInstance();

        String active = UserData.getActive(uuid);

        if (active == null || active.isBlank() || active.equalsIgnoreCase("None")) {
            String custom = UserData.getCustomTag(uuid);

            if (custom != null && !custom.isBlank()) {
                return applyFormatText(player, context, custom);
            }

            return replacePlaceholders(player, plugin.getConfig().getString(
                    "placeholders." + context.getPath() + ".none-output",
                    ""
            ));
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
            return replacePlaceholders(player, plugin.getConfig().getString(
                    "placeholders." + context.getPath() + ".none-output",
                    ""
            ));
        }

        return applyFormatText(player, context, tagText);
    }

    private static String applyFormatText(Player player, Context context, String tag) {
        SupremeTags plugin = SupremeTags.getInstance();

        String format = plugin.getConfig().getString(
                "placeholders." + context.getPath() + ".format",
                "%tag%"
        );

        String formatted = format.replace("%tag%", replacePlaceholders(player, tag));
        return replacePlaceholders(player, formatted);
    }

    private static String applyOutput(Player player, Context context, String formatted) {
        SupremeTags plugin = SupremeTags.getInstance();
        formatted = replacePlaceholders(player, formatted);

        String output = plugin.getConfig().getString(
                "placeholders." + context.getPath() + ".output",
                "minimessage"
        );

        if (output == null) {
            return toMiniMessage(formatted);
        }

        return switch (output.toLowerCase()) {
            case "raw" -> formatted;
            case "plain" -> deformat(formatted);
            case "minimessage-text" -> toMiniMessage(formatted);
            case "minimessage" -> toMiniMessage(formatted);
            default -> toMiniMessage(formatted);
        };
    }

}
