package net.noscape.project.supremetags.importer.types;

import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.importer.TagImporter;
import net.noscape.project.supremetags.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class EternalTagsImporter implements TagImporter {

    @Override
    public String getPluginName() {
        return "EternalTags";
    }

    @Override
    public File getConfigFile() {
        return new File(Bukkit.getWorldContainer(), "plugins/EternalTags/tags.yml");
    }

    @Override
    public void importTags(SupremeTags plugin, CommandSender sender, boolean force) {
        File file = getConfigFile();
        if (!file.exists()) return;

        if (sender == null) sender = Bukkit.getConsoleSender();

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("tags");

        if (section == null) return;

        section.getKeys(false).forEach(id -> {
            if (!force && plugin.getTagManager().getTags().containsKey(id)) return;

            String tag = config.getString("tags." + id + ".tag");
            String descText = config.getString("tags." + id + ".description", "N/A");
            String permission = config.getString("tags." + id + ".permission");
            String material = config.getString("tags." + id + ".icon.material", "NAME_TAG");

            List<String> desc = new ArrayList<>();
            desc.add(descText);

            plugin.getTagManager().createTag(id, material, tag, desc, permission, 100);
        });

        Utils.msgPlayer(sender, "&6Merger: &7Imported tags from &6EternalTags&7.");
    }
}