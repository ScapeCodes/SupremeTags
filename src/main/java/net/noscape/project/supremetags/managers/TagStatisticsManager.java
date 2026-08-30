package net.noscape.project.supremetags.managers;

import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.handlers.Tag;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class TagStatisticsManager {

    private static final String NONE = "none";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final SupremeTags plugin;
    private FileConfiguration data;

    public TagStatisticsManager(SupremeTags plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        plugin.getConfigManager().reloadConfig("statistics.yml");
        this.data = plugin.getConfigManager().getConfig("statistics.yml").get();
    }

    public void save() {
        if (data == null) return;
        plugin.getConfigManager().saveConfig("statistics.yml");
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("statistics.enabled", true);
    }

    public void recordActiveChange(OfflinePlayer player, String previousIdentifier, String newIdentifier) {
        if (!isEnabled() || player == null) return;
        if (!isNone(previousIdentifier) && previousIdentifier.equalsIgnoreCase(newIdentifier)) return;

        String previous = normalize(previousIdentifier);

        if (!isNone(previous)) {
            data.set("tags." + previous + ".active-users", Math.max(0, getTagActiveUsers(previous) - 1));
        }

        if (isNone(newIdentifier)) {
            save();
            return;
        }

        String identifier = normalize(newIdentifier);
        long now = System.currentTimeMillis();
        UUID uuid = player.getUniqueId();

        String tagPath = "tags." + identifier;
        String playerPath = "players." + uuid + ".tags." + identifier;
        String playerRoot = "players." + uuid;

        data.set(tagPath + ".selections", getTagSelections(identifier) + 1);
        data.set(tagPath + ".active-users", getTagActiveUsers(identifier) + 1);
        if (data.getLong(tagPath + ".first-selected", 0L) <= 0L) data.set(tagPath + ".first-selected", now);
        data.set(tagPath + ".last-selected", now);

        List<String> uniqueUsers = data.getStringList(tagPath + ".unique-users");
        String uuidString = uuid.toString();
        if (uniqueUsers.stream().noneMatch(uuidString::equalsIgnoreCase)) {
            uniqueUsers.add(uuidString);
            data.set(tagPath + ".unique-users", uniqueUsers);
        }

        data.set(playerPath + ".selections", getPlayerTagSelections(uuid, identifier) + 1);
        if (data.getLong(playerPath + ".first-selected", 0L) <= 0L) data.set(playerPath + ".first-selected", now);
        data.set(playerPath + ".last-selected", now);
        data.set(playerRoot + ".total-selections", getPlayerTotalSelections(uuid) + 1);
        data.set(playerRoot + ".last-selected", identifier);
        data.set("totals.selections", getTotalSelections() + 1);

        save();
    }

    public int getTagSelections(String identifier) {
        return data.getInt("tags." + normalize(identifier) + ".selections", 0);
    }

    public int getTagUniqueUsers(String identifier) {
        return data.getStringList("tags." + normalize(identifier) + ".unique-users").size();
    }

    public int getTagActiveUsers(String identifier) {
        return data.getInt("tags." + normalize(identifier) + ".active-users", 0);
    }

    public long getTagFirstSelected(String identifier) {
        return data.getLong("tags." + normalize(identifier) + ".first-selected", 0L);
    }

    public long getTagLastSelected(String identifier) {
        return data.getLong("tags." + normalize(identifier) + ".last-selected", 0L);
    }

    public int getPlayerTotalSelections(UUID uuid) {
        return data.getInt("players." + uuid + ".total-selections", 0);
    }

    public int getPlayerTagSelections(UUID uuid, String identifier) {
        return data.getInt("players." + uuid + ".tags." + normalize(identifier) + ".selections", 0);
    }

    public long getPlayerTagFirstSelected(UUID uuid, String identifier) {
        return data.getLong("players." + uuid + ".tags." + normalize(identifier) + ".first-selected", 0L);
    }

    public long getPlayerTagLastSelected(UUID uuid, String identifier) {
        return data.getLong("players." + uuid + ".tags." + normalize(identifier) + ".last-selected", 0L);
    }

    public String getPlayerLastSelected(UUID uuid) {
        return data.getString("players." + uuid + ".last-selected", "");
    }

    public String getPlayerMostUsed(UUID uuid) {
        ConfigurationSection section = data.getConfigurationSection("players." + uuid + ".tags");
        if (section == null) return "";
        String best = "";
        int bestCount = -1;
        for (String identifier : section.getKeys(false)) {
            int count = getPlayerTagSelections(uuid, identifier);
            if (count > bestCount) {
                best = identifier;
                bestCount = count;
            }
        }
        return best;
    }

    public int getTotalSelections() {
        return data.getInt("totals.selections", 0);
    }

    public String getTopTag() {
        return getTopTags(1).stream().findFirst().map(Tag::getIdentifier).orElse("");
    }

    public List<Tag> getTopTags(int limit) {
        List<Tag> tags = new ArrayList<>(plugin.getTagManager().getTags().values());
        tags.sort(Comparator.comparingInt((Tag tag) -> getTagSelections(tag.getIdentifier())).reversed().thenComparing(Tag::getIdentifier));
        return tags.stream().limit(Math.max(0, limit)).toList();
    }

    public int getTagRank(String identifier) {
        String normalized = normalize(identifier);
        List<Tag> sorted = getTopTags(Integer.MAX_VALUE);
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getIdentifier().equalsIgnoreCase(normalized)) return i + 1;
        }
        return 0;
    }

    public String replaceTagPlaceholders(OfflinePlayer player, String line, String identifier) {
        if (line == null) return "";
        UUID uuid = player == null ? null : player.getUniqueId();
        String normalized = normalize(identifier);
        line = line.replace("%stats_global_selections%", String.valueOf(getTagSelections(normalized)));
        line = line.replace("%stats_unique_users%", String.valueOf(getTagUniqueUsers(normalized)));
        line = line.replace("%stats_active_users%", String.valueOf(getTagActiveUsers(normalized)));
        line = line.replace("%stats_rank%", String.valueOf(getTagRank(normalized)));
        line = line.replace("%stats_first_selected%", formatTimestamp(getTagFirstSelected(normalized)));
        line = line.replace("%stats_last_selected%", formatTimestamp(getTagLastSelected(normalized)));
        if (uuid != null) {
            line = line.replace("%stats_player_selections%", String.valueOf(getPlayerTagSelections(uuid, normalized)));
            line = line.replace("%stats_player_first_selected%", formatTimestamp(getPlayerTagFirstSelected(uuid, normalized)));
            line = line.replace("%stats_player_last_selected%", formatTimestamp(getPlayerTagLastSelected(uuid, normalized)));
        }
        return line;
    }

    public String formatTimestamp(long timestamp) {
        if (timestamp <= 0L) return "Never";
        return DATE_FORMATTER.format(Instant.ofEpochMilli(timestamp));
    }

    private boolean isNone(String identifier) {
        return identifier == null || identifier.isBlank() || identifier.equalsIgnoreCase(NONE);
    }

    private String normalize(String identifier) {
        return identifier == null ? NONE : identifier.toLowerCase(Locale.ROOT);
    }
}
