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

public class DeluxeTagsImporter implements TagImporter {

    @Override
    public String getPluginName() {
        return "DeluxeTags";
    }

    @Override
    public File getConfigFile() {
        return new File(Bukkit.getWorldContainer(), "plugins/DeluxeTags/config.yml");
    }

    @Override
    public void importTags(SupremeTags plugin, CommandSender sender, boolean force) {
        File file = getConfigFile();
        if (!file.exists()) return;

        if (sender == null) sender = Bukkit.getConsoleSender();

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("deluxetags");

        if (section == null) return;

        section.getKeys(false).forEach(id -> {
            if (!force && plugin.getTagManager().getTags().containsKey(id)) return;

            String tag = config.getString("deluxetags." + id + ".tag");
            String description = config.getString("deluxetags." + id + ".description", "N/A");
            String permission = config.getString("deluxetags." + id + ".permission");

            List<String> desc = new ArrayList<>();
            desc.add(description);

            plugin.getTagManager().createTag(id, tag, desc, permission, 100);
        });

        Utils.msgPlayer(sender, "&6Merger: &7Imported tags from &6DeluxeTags&7.");
    }
}