package net.noscape.project.supremetags.storage.user;

import net.md_5.bungee.api.ChatColor;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.handlers.Tag;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class PlayerConfig {

    public static boolean exists(UUID uuid) {
        return new File(SupremeTags.getInstance().getDataFolder(), "data/" + uuid + ".yml").exists();
    }

    public static boolean exists(OfflinePlayer player) {
        return exists(player.getUniqueId());
    }

    public static FileConfiguration get(UUID uuid) {
        File file = new File(SupremeTags.getInstance().getDataFolder(), "data/" + uuid + ".yml");
        return YamlConfiguration.loadConfiguration(file);
    }

    public static FileConfiguration get(OfflinePlayer offlinePlayer) {
        return get(offlinePlayer.getUniqueId());
    }

    public static void save(UUID uuid) {
        File file = new File(SupremeTags.getInstance().getDataFolder(), "data/" + uuid + ".yml");
        FileConfiguration configuration = YamlConfiguration.loadConfiguration(file);

        List<Tag> tags = SupremeTags.getInstance().getPlayerManager().getPlayerTags().get(uuid);

        if (tags != null && !tags.isEmpty()) {
            ConfigurationSection tagsSection = configuration.getConfigurationSection("tags");
            if (tagsSection == null) {
                tagsSection = configuration.createSection("tags");
            }

            for (Tag tag : tags) {
                ConfigurationSection tagSection = tagsSection.getConfigurationSection(tag.getIdentifier());
                if (tagSection == null) {
                    tagSection = tagsSection.createSection(tag.getIdentifier());
                }

                String tagString = ChatColor.translateAlternateColorCodes('§', tag.getTag().get(0));

                tagSection.set("tag", tagString);
                tagSection.set("description", tag.getDescription());
            }

        }

        try {
            configuration.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadPlayer(UUID uuid) {
        if (exists(uuid)) {
            File file = new File(SupremeTags.getInstance().getDataFolder(), "data/" + uuid + ".yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);

            List<Tag> tags = new ArrayList<>();

            if (config.getConfigurationSection("tags") != null) {
                for (String identifier : Objects.requireNonNull(config.getConfigurationSection("tags")).getKeys(false)) {
                    String tag = config.getString("tags." + identifier + ".tag");
                    String description = config.getString("tags." + identifier + ".description");

                    List<String> tabList = new ArrayList<>();
                    tabList.add(tag);

                    List<String> desc = new ArrayList<>();
                    desc.add(description);

                    Tag t = new Tag(identifier, tabList, "", "", desc, false, "common", null, new ArrayList<>());
                    tags.add(t);
                }

                SupremeTags.getInstance().getPlayerManager().getPlayerTags().put(uuid, tags);
            }
        } else {
            createFile(uuid);
            loadPlayer(uuid);
        }
    }

    public void loadPlayer(OfflinePlayer player) {
        loadPlayer(player.getUniqueId());
    }

    void createFile(UUID uuid) {
        File folder = new File(SupremeTags.getInstance().getDataFolder(), "data");
        File file = new File(SupremeTags.getInstance().getDataFolder(), "data/" + uuid + ".yml");

        if (!folder.exists()) {
            boolean created = folder.mkdir();
            if (!created) {
                throw new RuntimeException("Failed to create the data folder.");
            }
        }

        if (!file.exists()) {
            FileConfiguration configuration = YamlConfiguration.loadConfiguration(file);
            configuration.createSection("tags");

            try {
                configuration.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void reset(UUID uuid) {
        File file = new File(SupremeTags.getInstance().getDataFolder(), "data/" + uuid + ".yml");
        FileConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        configuration.set("tags", null);

        try {
            configuration.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reset(OfflinePlayer player) {
        reset(player.getUniqueId());
    }

    public static void resetTag(UUID uuid, String identifier) {
        File file = new File(SupremeTags.getInstance().getDataFolder(), "data/" + uuid + ".yml");
        FileConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        configuration.set("tags." + identifier, null);

        try {
            configuration.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}