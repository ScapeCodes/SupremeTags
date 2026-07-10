package net.noscape.project.supremetags.handlers.hooks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.handlers.TagFormatter;
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

import static net.noscape.project.supremetags.utils.Utils.format;
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

        // Basic placeholder checks
        switch (params.toLowerCase()) {
            case "hastag_selected":
                return String.valueOf(
                        activeTagId != null &&
                                !activeTagId.isBlank() &&
                                !activeTagId.equalsIgnoreCase("None")
                );

            case "player_track_unlocked": {
                int count = 0;
                if (!player.isOp()) {
                    for (Tag tag : SupremeTags.getInstance().getTagManager().getTags().values()) {
                        if (Objects.requireNonNull(player.getPlayer()).hasPermission(tag.getPermission())) {
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

            // Tag display placeholders
            case "tag":
                return TagFormatter.getFormattedTag(player.getPlayer(), TagFormatter.Context.TAG);

            case "chattag":
                return TagFormatter.getFormattedTag(player.getPlayer(), TagFormatter.Context.CHAT);

            case "tabtag":
                return TagFormatter.getFormattedTag(player.getPlayer(), TagFormatter.Context.TAB);

            case "scoreboardtag":
                return TagFormatter.getFormattedTag(player.getPlayer(), TagFormatter.Context.SCOREBOARD);
        }

        // Dynamic placeholders
        if (params.startsWith("has_access_")) {
            String identifier = params.substring("has_access_".length());
            Tag tag = SupremeTags.getInstance().getTagManager().getTag(identifier);
            return tag != null
                    ? String.valueOf(Objects.requireNonNull(player.getPlayer()).hasPermission(tag.getPermission()))
                    : "false";
        }

        if (params.startsWith("track_unlocked_")) {
            String identifier = params.substring("track_unlocked_".length());
            return String.valueOf(TagManager.tagUnlockCounts.getOrDefault(identifier, 0));
        }

        if (params.startsWith("tag_custom-placeholder_")) {
            String placeholder = params.substring("tag_custom-placeholder_".length());

            Tag tag = tags.get(activeTagId);

            if (tag == null) {
                tag = SupremeTags.getInstance()
                        .getPlayerManager()
                        .loadAllPlayerTags(uuid)
                        .get(activeTagId);
            }

            if (tag == null && SupremeTags.getInstance().getTagManager().isVariant(activeTagId)) {
                Variant variant = SupremeTags.getInstance().getTagManager().getVariant(activeTagId);
                if (variant != null) {
                    tag = variant.getSisterTag();
                }
            }

            return tag != null
                    ? tag.getCustomPlaceholder(tag.getIdentifier(), placeholder)
                    : "";
        }

        // Resolve tag for metadata placeholders
        Tag tag = tags.get(activeTagId);

        if (tag == null) {
            tag = SupremeTags.getInstance()
                    .getPlayerManager()
                    .loadAllPlayerTags(uuid)
                    .get(activeTagId);
        }

        if (tag == null) {
            Variant variant = SupremeTags.getInstance().getTagManager().getVariantTag(player);
            if (variant != null) {
                tag = variant.getSisterTag();
            }
        }

        if (tag == null) {
            return "";
        }

        switch (params.toLowerCase()) {
            case "identifier":
                return tag.getIdentifier();

            case "description":
                return format(tag.getDescription().stream()
                        .map(Utils::format)
                        .collect(Collectors.joining("\n")));

            case "permission":
                return tag.getPermission();

            case "rarity":
                return SupremeTags.getInstance()
                        .getRarityManager()
                        .getRarity(tag.getRarity())
                        .getDisplayname();

            case "rarity_raw":
                return tag.getRarity();

            case "category":
                return tag.getCategory();

            case "cost":
                return String.valueOf(tag.getEconomy().getAmount());

            case "cost_formatted":
                return "$" + formatNumber(tag.getEconomy().getAmount());

            case "cost_formatted_raw":
                return formatNumber(tag.getEconomy().getAmount());

            default:
                return "";
        }
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