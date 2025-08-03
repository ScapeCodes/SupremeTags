package net.noscape.project.supremetags.handlers.hooks;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.handlers.Variant;
import net.noscape.project.supremetags.managers.TagManager;
import net.noscape.project.supremetags.storage.UserData;
import net.noscape.project.supremetags.utils.Utils;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static net.noscape.project.supremetags.utils.Utils.formatNumber;

public class PAPI extends PlaceholderExpansion {

    private final Map<String, Tag> tags;

    public PAPI(SupremeTags plugin) {
        tags = plugin.getTagManager().getTags();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "supremetags";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Scape";
    }

    @Override
    public @NotNull String getVersion() {
        return SupremeTags.getInstance().getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        UUID uuid = player.getUniqueId();
        String activeTagId = UserData.getActive(uuid);
        String text = "";

        // Basic placeholder checks
        switch (params.toLowerCase()) {
            case "hastag_selected":
                return String.valueOf(!activeTagId.equalsIgnoreCase("None"));

            case "player_track_unlocked": {
                int count = 0;
                if (!player.isOp()) {
                    for (Tag tag : SupremeTags.getInstance().getTagManager().getTags().values()) {
                        if (player.getPlayer().hasPermission(tag.getPermission())) {
                            count++;
                        }
                    }
                } else {
                    count = SupremeTags.getInstance().getTagManager().getTags().size();
                }
                return String.valueOf(count);
            }

            case "hastag_tags":
                return String.valueOf(hasTags(player.getPlayer()));

            case "tags_amount":
            case "tags_total":
                return String.valueOf(SupremeTags.getInstance().getTagManager().getTags().size());
        }

        // Dynamic placeholders
        if (params.startsWith("has_access_")) {
            String identifier = params.substring("has_access_".length());
            Tag tag = SupremeTags.getInstance().getTagManager().getTag(identifier);
            if (tag != null) {
                return String.valueOf(Objects.requireNonNull(player.getPlayer()).hasPermission(tag.getPermission()));
            } else {
                return "false";
            }
        }

        if (params.startsWith("track_unlocked_")) {
            String identifier = params.substring("track_unlocked_".length());
            return String.valueOf(TagManager.tagUnlockCounts.getOrDefault(identifier, 0));
        }

        if (params.startsWith("tag_custom-placeholder_")) {
            String placeholder = params.substring("tag_custom-placeholder_".length());
            Tag tag = SupremeTags.getInstance().getTagManager().getTag(activeTagId);

            if (tag == null && SupremeTags.getInstance().getTagManager().isVariant(activeTagId)) {
                tag = SupremeTags.getInstance().getTagManager().getVariant(activeTagId).getSisterTag();
            }

            return (tag != null) ? tag.getCustomPlaceholder(tag.getIdentifier(), placeholder) : "";
        }

        // Tag and variant handling
        Tag tag = tags.get(activeTagId);

        if (tag == null) {
            tag = SupremeTags.getInstance().getPlayerManager().loadAllPlayerTags(player.getUniqueId()).get(activeTagId);
        }

        if (tag != null) {
            text = getTagInfo(tag, params, player, activeTagId);
        } else {
            Variant variant = SupremeTags.getInstance().getTagManager().getVariantTag(player);
            if (variant != null) {
                text = getVariantInfo(variant, params, player, activeTagId);
            } else {
                if (params.equalsIgnoreCase("tag")) {
                    if (activeTagId.equalsIgnoreCase("None")) {
                        return SupremeTags.getInstance().getConfig().getString("placeholders.tag.none-output", "");
                    }
                }

                if (params.equalsIgnoreCase("chattag")) {
                    if (activeTagId.equalsIgnoreCase("None")) {
                        return SupremeTags.getInstance().getConfig().getString("placeholders.chat.none-output", "");
                    }
                }

                if (params.equalsIgnoreCase("tabtag")) {
                    if (activeTagId.equalsIgnoreCase("None")) {
                        return SupremeTags.getInstance().getConfig().getString("placeholders.tab.none-output", "");
                    }
                }

                if (params.equalsIgnoreCase("scoreboardtag")) {
                    if (activeTagId.equalsIgnoreCase("None")) {
                        return SupremeTags.getInstance().getConfig().getString("placeholders.scoreboard.none-output", "");
                    }
                }
            }
        }

        return text;
    }

    // Extracted helper for tag info
    private String getTagInfo(Tag t, String params, OfflinePlayer player, String activeTagId) {
        switch (params.toLowerCase()) {
            case "tag":
                if (activeTagId.equalsIgnoreCase("None")) {
                    return params.equalsIgnoreCase("tag") ?
                            SupremeTags.getInstance().getConfig().getString("placeholders.tag.none-output") : "";
                }
                String tagStr1 = (t.getCurrentTag() != null) ? t.getCurrentTag() : t.getTag().get(0);

                String formatted = SupremeTags.getInstance().getConfig().getString("placeholders.tag.format");
                formatted = formatted.replace("%tag%", tagStr1);

                return PlaceholderAPI.setPlaceholders(player, formatted);
            case "chattag":
                if (activeTagId.equalsIgnoreCase("None")) {
                    return SupremeTags.getInstance().getConfig().getString("placeholders.chat.none-output", "");
                }

                String tagStr2 = (t.getCurrentTag() != null) ? t.getCurrentTag() : t.getTag().get(0);
                String formatted2 = SupremeTags.getInstance().getConfig().getString("placeholders.chat.format");
                formatted2 = formatted2.replace("%tag%", tagStr2);

                return PlaceholderAPI.setPlaceholders(player, formatted2);
            case "scoreboardtag":
                if (activeTagId.equalsIgnoreCase("None")) {
                    return SupremeTags.getInstance().getConfig().getString("placeholders.scoreboard.none-output", "");
                }

                String sbStr = (t.getCurrentTag() != null) ? t.getCurrentTag() : t.getTag().get(0);
                String sbformatted = SupremeTags.getInstance().getConfig().getString("placeholders.scoreboard.format");
                sbformatted = sbformatted.replace("%tag%", sbStr);

                return PlaceholderAPI.setPlaceholders(player, sbformatted);
            case "tabtag":
                if (activeTagId.equalsIgnoreCase("None")) {
                    return SupremeTags.getInstance().getConfig().getString("placeholders.tab.none-output", "");
                }

                String tabStr = (t.getCurrentTag() != null) ? t.getCurrentTag() : t.getTag().get(0);
                String tabformatted = SupremeTags.getInstance().getConfig().getString("placeholders.tab.format");
                tabformatted = tabformatted.replace("%tag%", tabStr);

                return PlaceholderAPI.setPlaceholders(player, tabformatted);
            case "identifier":
                return t.getIdentifier();
            case "description":
                return t.getDescription().stream().map(Utils::format).collect(Collectors.joining("\n"));
            case "permission":
                return t.getPermission();
            case "rarity":
                return SupremeTags.getInstance().getRarityManager().getRarity(t.getRarity()).getDisplayname();
            case "rarity_raw":
                return t.getRarity();
            case "category":
                return t.getCategory();
            case "cost":
                return String.valueOf(t.getEconomy().getAmount());
            case "cost_formatted":
                return "$" + formatNumber(t.getEconomy().getAmount());
            case "cost_formatted_raw":
                return formatNumber(t.getEconomy().getAmount());
            default:
                return "";
        }
    }

    // Extracted helper for variant info
    private String getVariantInfo(Variant v, String params, OfflinePlayer player, String activeTagId) {
        String text = "";

        if (params.equalsIgnoreCase("tag")) {
            if (activeTagId.equalsIgnoreCase("None")) {
                text = SupremeTags.getInstance().getConfig().getString("placeholders.tag.none-output", "");
                return text;
            }

            String tag = v.getTag().get(0);
            String formatted = SupremeTags.getInstance().getConfig().getString("placeholders.tag.format");
            formatted = formatted.replace("%tag%", tag);

            text = PlaceholderAPI.setPlaceholders(player, formatted);
        }

        if (params.equalsIgnoreCase("chattag")) {
            if (activeTagId.equalsIgnoreCase("None")) {
                text = SupremeTags.getInstance().getConfig().getString("placeholders.chat.none-output", "");
                return text;
            }

            String tag = v.getTag().get(0);
            String formatted = SupremeTags.getInstance().getConfig().getString("placeholders.chat.format");
            formatted = formatted.replace("%tag%", tag);

            text = PlaceholderAPI.setPlaceholders(player, formatted);
        }

        if (params.equalsIgnoreCase("tabtag")) {
            if (activeTagId.equalsIgnoreCase("None")) {
                text = SupremeTags.getInstance().getConfig().getString("placeholders.tab.none-output", "");
                return text;
            }

            String tag = v.getTag().get(0);
            String formatted = SupremeTags.getInstance().getConfig().getString("placeholders.tab.format");
            formatted = formatted.replace("%tag%", tag);

            text = PlaceholderAPI.setPlaceholders(player, formatted);
        }

        if (params.equalsIgnoreCase("scoreboardtag")) {
            if (activeTagId.equalsIgnoreCase("None")) {
                text = SupremeTags.getInstance().getConfig().getString("placeholders.scoreboard.none-output", "");
                return text;
            }

            String tag = v.getTag().get(0);
            String formatted = SupremeTags.getInstance().getConfig().getString("placeholders.scoreboard.format");
            formatted = formatted.replace("%tag%", tag);

            text = PlaceholderAPI.setPlaceholders(player, formatted);
        }

        if (params.equalsIgnoreCase("description")) {
            text = v.getDescription().stream().map(Utils::format).collect(Collectors.joining("\n"));
            return text;
        }

        if (params.equalsIgnoreCase("rarity")) {
            return SupremeTags.getInstance().getRarityManager().getRarity(v.getRarity()).getDisplayname();
        }

        return text;
    }

    public boolean hasTags(Player player) {
        for (Tag tag : SupremeTags.getInstance().getTagManager().getTags().values()) {
            if (player.hasPermission(tag.getPermission())) {
                return true;
            }
        }
        return false;
    }
}