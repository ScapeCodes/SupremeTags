package net.noscape.project.supremetags.managers;

import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.handlers.Variant;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static net.noscape.project.supremetags.utils.Utils.format;

public class AutoApplyManager {

    private final SupremeTags plugin;
    private final Map<UUID, String> originalDisplayNames = new ConcurrentHashMap<>();
    private final Map<UUID, String> originalTabNames = new ConcurrentHashMap<>();

    public AutoApplyManager(SupremeTags plugin) {
        this.plugin = plugin;
    }

    public void apply(Player player, String identifier) {
        if (player == null) {
            return;
        }

        applyDisplayName(player, identifier);
        applyTabName(player, identifier);
    }

    public void applyAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            String identifier = net.noscape.project.supremetags.storage.UserData.getActive(player.getUniqueId());
            apply(player, identifier);
        }
    }

    public void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            String identifier = net.noscape.project.supremetags.storage.UserData.getActive(player.getUniqueId());
            restore(player);
            apply(player, identifier);
        }
    }

    public void restore(Player player) {
        if (player == null) {
            return;
        }

        restoreDisplayName(player);
        restoreTabName(player);
    }

    public void restoreAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            restore(player);
        }
    }

    private void applyDisplayName(Player player, String identifier) {
        UUID uuid = player.getUniqueId();
        originalDisplayNames.putIfAbsent(uuid, player.getDisplayName());

        if (!plugin.getConfig().getBoolean("settings.auto-apply.displayname", false) || isNoTag(identifier)) {
            restoreDisplayName(player);
            return;
        }

        String tagText = getTagText(player, identifier);
        if (tagText.isEmpty()) {
            restoreDisplayName(player);
            return;
        }

        String baseName = originalDisplayNames.getOrDefault(uuid, player.getName());
        String nameFormat = plugin.getConfig().getString("settings.auto-apply.displayname-format", "%displayname% %tag%");
        String appliedName = format(applyPlaceholders(nameFormat, player, identifier, baseName, tagText, false));
        player.setDisplayName(appliedName);
        player.setCustomName(appliedName);
    }

    private void applyTabName(Player player, String identifier) {
        UUID uuid = player.getUniqueId();
        originalTabNames.putIfAbsent(uuid, player.getPlayerListName());

        if (!plugin.getConfig().getBoolean("settings.auto-apply.tab", false) || isNoTag(identifier)) {
            restoreTabName(player);
            return;
        }

        String tagText = getTagText(player, identifier);
        if (tagText.isEmpty()) {
            restoreTabName(player);
            return;
        }

        String baseName = originalTabNames.getOrDefault(uuid, player.getName());
        String nameFormat = plugin.getConfig().getString("settings.auto-apply.tab-format", "%playerlistname% %tag%");
        player.setPlayerListName(format(applyPlaceholders(nameFormat, player, identifier, baseName, tagText, true)));
    }

    private void restoreDisplayName(Player player) {
        String original = originalDisplayNames.remove(player.getUniqueId());
        if (original != null) {
            player.setDisplayName(original);
            player.setCustomName(original);
        }
    }

    private void restoreTabName(Player player) {
        String original = originalTabNames.remove(player.getUniqueId());
        if (original != null) {
            player.setPlayerListName(original);
        }
    }

    private String applyPlaceholders(String value, Player player, String identifier, String baseName, String tagText, boolean tab) {
        return value
                .replace("%player%", player.getName())
                .replace("%identifier%", identifier == null ? "None" : identifier)
                .replace("%tag%", tagText)
                .replace(tab ? "%playerlistname%" : "%displayname%", baseName)
                .replace("%playerlistname%", originalTabNames.getOrDefault(player.getUniqueId(), player.getName()))
                .replace("%displayname%", originalDisplayNames.getOrDefault(player.getUniqueId(), player.getName()));
    }

    private String getTagText(Player player, String identifier) {
        if (isNoTag(identifier)) {
            return "";
        }

        Tag tag = plugin.getTagManager().getTag(identifier);
        if (tag != null) {
            return tag.getCurrentTag();
        }

        Variant variant = plugin.getTagManager().getVariant(identifier);
        if (variant != null) {
            return variant.getCurrentTag();
        }

        Tag personalTag = plugin.getPlayerManager().getTag(player.getUniqueId(), identifier);
        if (personalTag != null) {
            return personalTag.getCurrentTag();
        }

        return "";
    }

    private boolean isNoTag(String identifier) {
        return identifier == null || identifier.isBlank() || identifier.equalsIgnoreCase("none");
    }
}
