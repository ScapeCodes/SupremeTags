package net.noscape.project.supremetags.managers;

import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.storage.user.PlayerConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class PlayerManager {

    private final Map<UUID, List<Tag>> playerTags = new HashMap<>();

    public PlayerManager() {}

    public List<Tag> getPlayerTags(UUID uuid) {
        return playerTags.get(uuid);
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

    public void addTag(Player player, Tag tag) {
        if (getPlayerTags().containsKey(player.getUniqueId())) {
            getPlayerTags().get(player.getUniqueId()).add(tag);
        } else {
            List<Tag> tags = new ArrayList<>();
            tags.add(tag);
            getPlayerTags().put(player.getUniqueId(), tags);
        }
    }

    public void removeTag(Player player, Tag tag) {
        getPlayerTags().get(player.getUniqueId()).remove(tag);
    }

    public void delete(Player player, String identifier) {
        UUID uuid = player.getUniqueId();

        // Remove the tag from memory
        Tag tagToRemove = getTag(uuid, identifier);
        if (tagToRemove != null) {
            removeTag(player, tagToRemove);
        }

        // Remove from config
        PlayerConfig.resetTag(player, identifier);
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

        PlayerConfig.save(player); // Save the player's configuration to the file
    }
}