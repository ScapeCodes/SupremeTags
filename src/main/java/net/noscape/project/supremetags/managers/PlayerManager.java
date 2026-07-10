package net.noscape.project.supremetags.managers;

import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.storage.user.PlayerConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager {

    private final Map<UUID, List<Tag>> playerTags = new ConcurrentHashMap<>();

    public PlayerManager() {}

    public List<Tag> getPlayerTags(UUID uuid) {
        return playerTags.getOrDefault(uuid, Collections.emptyList());
    }

    public Tag getTag(UUID uuid, String identifier) {
        if (getPlayerTags(uuid) != null) {
            for (Tag t : getPlayerTags(uuid)) {
                if (t.getIdentifier().equalsIgnoreCase(identifier)) {
                    return t;
                }
            }
        }

        return null;
    }

    public boolean doesTagExist(UUID uuid, String identifier) {
        return getTag(uuid, identifier) != null;
    }

    public void load(Player player) {
        playerTags.remove(player.getUniqueId());

        List<Tag> tags = new ArrayList<>();

        if (PlayerConfig.get(player).getConfigurationSection("tags") != null) {
            for (String identifier : Objects.requireNonNull(PlayerConfig.get(player).getConfigurationSection("tags")).getKeys(false)) {
                String tag = PlayerConfig.get(player).getString("tags." + identifier + ".tag");
                String description = PlayerConfig.get(player).getString("tags." + identifier + ".description");

                List<String> desc = new ArrayList<>();
                desc.add(description);

                List<String> tagList = new ArrayList<>();
                tagList.add(tag);

                Tag t = new Tag(identifier, tagList, desc);
                tags.add(t);
            }
        }

        playerTags.put(player.getUniqueId(), tags);
    }

    public Map<UUID, List<Tag>> getPlayerTags() {
        return playerTags;
    }

    public List<String> listAllStringTags(UUID uuid) {
        List<String> allTags = new ArrayList<>();

        if (getPlayerTags().get(uuid) == null) {
            return allTags;
        }

        for (Tag tag : getPlayerTags().get(uuid)) {
            allTags.add(tag.getIdentifier());
        }

        return allTags;
    }

    public Map<String, Tag> loadAllPlayerTags(UUID uuid) {
        Map<String, Tag> pt = new HashMap<>();

        if (playerTags.containsKey(uuid)) {
            for (Tag t : playerTags.get(uuid)) {
                String identifier = t.getIdentifier();
                pt.put(identifier, t);
            }
        }

        return pt;
    }

    public void addTag(UUID uuid, Tag tag) {
        getPlayerTags().computeIfAbsent(uuid, k -> new ArrayList<>()).add(tag);
    }
    public void addTag(Player player, Tag tag) {
        addTag(player.getUniqueId(), tag);
    }

    public void removeTag(UUID uuid, Tag tag) {
        List<Tag> tags = getPlayerTags().get(uuid);
        if (tags != null) {
            tags.remove(tag);
        }
    }

    public void removeTag(Player player, Tag tag) {
        removeTag(player.getUniqueId(), tag);
    }

    public void delete(UUID uuid, String identifier) {
        // Remove the tag from memory
        Tag tagToRemove = getTag(uuid, identifier);
        if (tagToRemove != null) {
            removeTag(uuid, tagToRemove);
        }

        // Remove from config
        PlayerConfig.resetTag(uuid, identifier);
    }

    public void delete(Player player, String identifier) {
        delete(player.getUniqueId(), identifier);
    }

    public void save(Tag tag, Player player) {
        FileConfiguration playerConfig = PlayerConfig.get(player);

        ConfigurationSection tagsSection = playerConfig.getConfigurationSection("tags");
        if (tagsSection == null) {
            tagsSection = playerConfig.createSection("tags");
        }

        ConfigurationSection tagSection = tagsSection.getConfigurationSection(tag.getIdentifier());
        if (tagSection == null) {
            tagSection = tagsSection.createSection(tag.getIdentifier());
        }

        tagSection.set("tag", tag.getCurrentTag());
        tagSection.set("description", tag.getDescription());

        PlayerConfig.save(player.getUniqueId()); // Save the player's configuration to the file
    }
}