package net.noscape.project.supremetags.managers;

import net.noscape.project.supremetags.SupremeTags;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static net.noscape.project.supremetags.utils.Utils.msgPlayer;

public class MergeManager {

    /*
     * SUPPORT TAG PLUGINS:
     * - DeluxeTags
     * - ExternalTags
     */

    public void merge(Logger log) {
        File deluxetagsFile = new File(Bukkit.getServer().getWorldContainer().getAbsolutePath() + "/plugins/DeluxeTags/config.yml"); // First we will load the file.
        FileConfiguration deluxetagsConfig = YamlConfiguration.loadConfiguration(deluxetagsFile); // Now we will load the file into a FileConfiguration.

        File eternaltagsFile = new File(Bukkit.getServer().getWorldContainer().getAbsolutePath() + "/plugins/EternalTags/tags.yml"); // First we will load the file.
        FileConfiguration eternaltagsConfig = YamlConfiguration.loadConfiguration(eternaltagsFile); // Now we will load the file into a FileConfiguration.

        File alonsoTagsFile = new File(Bukkit.getServer().getWorldContainer().getAbsolutePath() + "/plugins/AlonsoTags/tags.yml"); // First we will load the file.
        FileConfiguration alonsoTagsConfig = YamlConfiguration.loadConfiguration(alonsoTagsFile); // Now we will load the file into a FileConfiguration.

        if (SupremeTags.getInstance().getConfig().getBoolean("settings.auto-merge")) {
            ConfigurationSection eternaltagsSection = eternaltagsConfig.getConfigurationSection("tags");
            ConfigurationSection deluxeTagsSection = deluxetagsConfig.getConfigurationSection("deluxetags");
            ConfigurationSection alonsoTagsSection = alonsoTagsConfig.getConfigurationSection("Tags");

            if (deluxeTagsSection == null && eternaltagsSection == null && alonsoTagsSection == null) {
                log.info("&6Merger: &7Supremetags only supports DeluxeTags, EternalTags & AlonsoTags.");
                return;
            }

            if (alonsoTagsSection != null) {
                for (String identifier : alonsoTagsSection.getKeys(false)) {
                    if (!SupremeTags.getInstance().getTagManager().getTags().containsKey(identifier)) {
                        String tag = alonsoTagsSection.getString(String.format("Tags.%s.tag", identifier));
                        String permission = alonsoTagsSection.getString(String.format("Tags.%s.Permission", identifier));
                        String material = alonsoTagsSection.getString(String.format("Tags.%s.Material", identifier));
                        int custom_model_data = alonsoTagsSection.getInt(String.format("Tags.%s.Custom-model-data", identifier));
                        int cost = alonsoTagsSection.getInt(String.format("Tags.%s.Price", identifier));

                        List<String> desc = new ArrayList<>();
                        desc.add("N/A");

                        SupremeTags.getInstance().getTagManager().createTag(identifier, material, tag, desc, permission, cost, custom_model_data);
                    }
                }
                log.info("&6Merger: &7Added all new tags from &6AlonsoTags&7 were added, any existing tags with the same name won't be added.");
            }

            if (eternaltagsSection != null) {
                for (String identifier : eternaltagsSection.getKeys(false)) {
                    if (!SupremeTags.getInstance().getTagManager().getTags().containsKey(identifier)) {
                        String tag = eternaltagsConfig.getString(String.format("tags.%s.tag", identifier));
                        String description = eternaltagsConfig.getString(String.format("tags.%s.description", identifier));
                        String permission = eternaltagsConfig.getString(String.format("tags.%s.permission", identifier));
                        String material = eternaltagsConfig.getString(String.format("tags.%s.icon.material", identifier));

                        List<String> desc = new ArrayList<>();
                        desc.add(description);


                        SupremeTags.getInstance().getTagManager().createTag(identifier, material, tag, desc, permission, 100);
                    }
                }
                log.info("Merger: Added all new tags from EternalTags, any existing tags with the same name were not added.");
            }

            if (deluxeTagsSection != null) {
                for (String identifier : deluxeTagsSection.getKeys(false)) {
                    if (!SupremeTags.getInstance().getTagManager().getTags().containsKey(identifier)) {
                        String tag = deluxetagsConfig.getString(String.format("deluxetags.%s.tag", identifier));
                        String description = deluxetagsConfig.getString(String.format("deluxetags.%s.description", identifier));
                        String permission = deluxetagsConfig.getString(String.format("deluxetags.%s.permission", identifier));

                        List<String> desc = new ArrayList<>();
                        desc.add(description);

                        SupremeTags.getInstance().getTagManager().createTag(identifier, tag, desc, permission, 100);
                    }
                }
                log.info("Merger: Added all new tags from DeluxeTags, any existing tags with the same name were not added.");
            }
        }
    }

    public void mergeFromFree(CommandSender sender) {
        File freeSupremetagsFile = new File(Bukkit.getServer().getWorldContainer().getAbsolutePath() + "/plugins/SupremeTags/free-tags.yml"); // First we will load the file.
        FileConfiguration freeSupremetagsConfig = YamlConfiguration.loadConfiguration(freeSupremetagsFile); // Now we will load the file into a FileConfiguration.

        ConfigurationSection freeSupremetagsSection = freeSupremetagsConfig.getConfigurationSection("tags");

        if (freeSupremetagsSection == null) {
            msgPlayer(sender, "&6Merger: &7Please rename the old ST-free config to 'free-config.yml', with it in the supremetags folder.");
            return;
        }

        for (String identifier : freeSupremetagsSection.getKeys(false)) {
            if (!SupremeTags.getInstance().getTagManager().getTags().containsKey(identifier)) {
                String tag = freeSupremetagsConfig.getString(String.format("tags.%s.tag", identifier));
                String description = freeSupremetagsConfig.getString(String.format("tags.%s.description", identifier));
                String permission = freeSupremetagsConfig.getString(String.format("tags.%s.permission", identifier));
                String material = freeSupremetagsConfig.getString(String.format("tags.%s.icon.material", identifier));

                List<String> desc = new ArrayList<>();
                desc.add(description);

                SupremeTags.getInstance().getTagManager().createTag(identifier, material, tag, desc, permission, 100);
            }
        }
        msgPlayer(sender, "&6Merger: &7Added all new tags from &6Free SupremeTags&7 were added, any existing tags with the same name won't be added.");
    }

    public void mergeForced(CommandSender player) {
        File deluxetagsFile = new File(Bukkit.getServer().getWorldContainer().getAbsolutePath() + "/plugins/DeluxeTags/config.yml"); // First we will load the file.
        FileConfiguration deluxetagsConfig = YamlConfiguration.loadConfiguration(deluxetagsFile); // Now we will load the file into a FileConfiguration.

        File eternaltagsFile = new File(Bukkit.getServer().getWorldContainer().getAbsolutePath() + "/plugins/EternalTags/tags.yml"); // First we will load the file.
        FileConfiguration eternaltagsConfig = YamlConfiguration.loadConfiguration(eternaltagsFile); // Now we will load the file into a FileConfiguration.

        File alonsoTagsFile = new File(Bukkit.getServer().getWorldContainer().getAbsolutePath() + "/plugins/AlonsoTags/tags.yml"); // First we will load the file.
        FileConfiguration alonsoTagsConfig = YamlConfiguration.loadConfiguration(alonsoTagsFile); // Now we will load the file into a FileConfiguration.

        ConfigurationSection eternaltagsSection = eternaltagsConfig.getConfigurationSection("tags");
        ConfigurationSection deluxeTagsSection = deluxetagsConfig.getConfigurationSection("deluxetags");
        ConfigurationSection alonsoTagsSection = alonsoTagsConfig.getConfigurationSection("Tags");

        if (deluxeTagsSection == null && eternaltagsSection == null && alonsoTagsSection == null) {
            msgPlayer(player, "&6Merger: &7Supremetags only supports DeluxeTags, EternalTags & AlonsoTags.");
            return;
        }

        if (alonsoTagsSection != null) {
            for (String identifier : alonsoTagsSection.getKeys(false)) {
                if (!SupremeTags.getInstance().getTagManager().getTags().containsKey(identifier)) {
                    String tag = alonsoTagsSection.getString(String.format("Tags.%s.tag", identifier));
                    String permission = alonsoTagsSection.getString(String.format("Tags.%s.Permission", identifier));
                    String material = alonsoTagsSection.getString(String.format("Tags.%s.Material", identifier));
                    int custom_model_data = alonsoTagsSection.getInt(String.format("Tags.%s.Custom-model-data", identifier));
                    int cost = alonsoTagsSection.getInt(String.format("Tags.%s.Price", identifier));

                    List<String> desc = new ArrayList<>();
                    desc.add("N/A");

                    SupremeTags.getInstance().getTagManager().createTag(identifier, material, tag, desc, permission, cost, custom_model_data);
                }
            }
            msgPlayer(player, "&6Merger: &7Added all new tags from &6AlonsoTags&7 were added, any existing tags with the same name won't be added.");
        }

        if (eternaltagsSection != null) {
            for (String identifier : eternaltagsSection.getKeys(false)) {
                if (!SupremeTags.getInstance().getTagManager().getTags().containsKey(identifier)) {
                    String tag = eternaltagsConfig.getString(String.format("tags.%s.tag", identifier));
                    String description = eternaltagsConfig.getString(String.format("tags.%s.description", identifier));
                    String permission = eternaltagsConfig.getString(String.format("tags.%s.permission", identifier));
                    String material = eternaltagsConfig.getString(String.format("tags.%s.icon.material", identifier));

                    List<String> desc = new ArrayList<>();
                    desc.add(description);

                    SupremeTags.getInstance().getTagManager().createTag(identifier, material, tag, desc, permission, 100);
                }
            }
            msgPlayer(player, "&6Merger: &7Added all new tags from &6EternalTags&7 were added, any existing tags with the same name won't be added.");
        }

        if (deluxeTagsSection != null) {
            for (String identifier : deluxeTagsSection.getKeys(false)) {
                if (!SupremeTags.getInstance().getTagManager().getTags().containsKey(identifier)) {
                    String tag = deluxetagsConfig.getString(String.format("deluxetags.%s.tag", identifier));
                    String description = deluxetagsConfig.getString(String.format("deluxetags.%s.description", identifier));
                    String permission = deluxetagsConfig.getString(String.format("deluxetags.%s.permission", identifier));

                    List<String> desc = new ArrayList<>();
                    desc.add(description);

                    SupremeTags.getInstance().getTagManager().createTag(identifier, tag, desc, permission, 100);
                }
            }
            msgPlayer(player, "&6Merger: &7Added all new tags from &6DeluxeTags&7 were added, any existing tags with the same name won't be added.");
        }
    }

    public void merge(CommandSender player) {
        File deluxetagsFile = new File(Bukkit.getServer().getWorldContainer().getAbsolutePath() + "/plugins/DeluxeTags/config.yml"); // First we will load the file.
        FileConfiguration deluxetagsConfig = YamlConfiguration.loadConfiguration(deluxetagsFile); // Now we will load the file into a FileConfiguration.

        File eternaltagsFile = new File(Bukkit.getServer().getWorldContainer().getAbsolutePath() + "/plugins/EternalTags/tags.yml"); // First we will load the file.
        FileConfiguration eternaltagsConfig = YamlConfiguration.loadConfiguration(eternaltagsFile); // Now we will load the file into a FileConfiguration.

        File alonsoTagsFile = new File(Bukkit.getServer().getWorldContainer().getAbsolutePath() + "/plugins/AlonsoTags/tags.yml"); // First we will load the file.
        FileConfiguration alonsoTagsConfig = YamlConfiguration.loadConfiguration(alonsoTagsFile); // Now we will load the file into a FileConfiguration.

        if (SupremeTags.getInstance().getConfig().getBoolean("settings.auto-merge")) {
            ConfigurationSection eternaltagsSection = eternaltagsConfig.getConfigurationSection("tags");
            ConfigurationSection deluxeTagsSection = deluxetagsConfig.getConfigurationSection("deluxetags");
            ConfigurationSection alonsoTagsSection = alonsoTagsConfig.getConfigurationSection("Tags");

            if (deluxeTagsSection == null && eternaltagsSection == null && alonsoTagsSection == null) {
                msgPlayer(player, "&6Merger: &7Supremetags only supports DeluxeTags, EternalTags & AlonsoTags.");
                return;
            }

            if (alonsoTagsSection != null) {
                for (String identifier : alonsoTagsSection.getKeys(false)) {
                    if (!SupremeTags.getInstance().getTagManager().getTags().containsKey(identifier)) {
                        String tag = alonsoTagsSection.getString(String.format("Tags.%s.tag", identifier));
                        String permission = alonsoTagsSection.getString(String.format("Tags.%s.Permission", identifier));
                        String material = alonsoTagsSection.getString(String.format("Tags.%s.Material", identifier));
                        int custom_model_data = alonsoTagsSection.getInt(String.format("Tags.%s.Custom-model-data", identifier));
                        int cost = alonsoTagsSection.getInt(String.format("Tags.%s.Price", identifier));

                        List<String> desc = new ArrayList<>();
                        desc.add("N/A");

                        SupremeTags.getInstance().getTagManager().createTag(identifier, material, tag, desc, permission, cost, custom_model_data);
                    }
                }
                msgPlayer(player, "&6Merger: &7Added all new tags from &6AlonsoTags&7 were added, any existing tags with the same name won't be added.");
            }

            if (eternaltagsSection != null) {
                for (String identifier : eternaltagsSection.getKeys(false)) {
                    if (!SupremeTags.getInstance().getTagManager().getTags().containsKey(identifier)) {
                        String tag = eternaltagsConfig.getString(String.format("tags.%s.tag", identifier));
                        String description = eternaltagsConfig.getString(String.format("tags.%s.description", identifier));
                        String permission = eternaltagsConfig.getString(String.format("tags.%s.permission", identifier));
                        String material = eternaltagsConfig.getString(String.format("tags.%s.icon.material", identifier));

                        List<String> desc = new ArrayList<>();
                        desc.add(description);

                        SupremeTags.getInstance().getTagManager().createTag(identifier, material, tag, desc, permission, 100);
                    }
                }
                msgPlayer(player, "&6Merger: &7Added all new tags from &6EternalTags&7 were added, any existing tags with the same name won't be added.");
            }

            if (deluxeTagsSection != null) {
                for (String identifier : deluxeTagsSection.getKeys(false)) {
                    if (!SupremeTags.getInstance().getTagManager().getTags().containsKey(identifier)) {
                        String tag = deluxetagsConfig.getString(String.format("deluxetags.%s.tag", identifier));
                        String description = deluxetagsConfig.getString(String.format("deluxetags.%s.description", identifier));
                        String permission = deluxetagsConfig.getString(String.format("deluxetags.%s.permission", identifier));

                        List<String> desc = new ArrayList<>();
                        desc.add(description);

                        SupremeTags.getInstance().getTagManager().createTag(identifier, tag, desc, permission, 100);
                    }
                }
                msgPlayer(player, "&6Merger: &7Added all new tags from &6DeluxeTags&7 were added, any existing tags with the same name won't be added.");
            }
        }
    }
}
