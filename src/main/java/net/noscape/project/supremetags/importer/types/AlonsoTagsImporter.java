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

public class AlonsoTagsImporter implements TagImporter {

    @Override
    public String getPluginName() {
        return "AlonsoTags";
    }

    @Override
    public File getConfigFile() {
        return new File(Bukkit.getWorldContainer(), "plugins/AlonsoTags/tags.yml");
    }

    @Override
    public void importTags(SupremeTags plugin, CommandSender sender, boolean force) {
        File file = getConfigFile();
        if (!file.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("Tags");

        if (section == null) return;

        section.getKeys(false).forEach(id -> {
            if (!force && plugin.getTagManager().getTags().containsKey(id)) return;

            String tag = config.getString("Tags." + id + ".tag");
            String permission = config.getString("Tags." + id + ".Permission");
            String material = config.getString("Tags." + id + ".Material", "NAME_TAG");
            int modelData = config.getInt("Tags." + id + ".Custom-model-data", 0);
            int cost = config.getInt("Tags." + id + ".Price", 100);

            List<String> desc = new ArrayList<>();
            desc.add("N/A");

            plugin.getTagManager().createTag(id, material, tag, desc, permission, cost, modelData);
        });

        Utils.msgPlayer(sender, "&6Merger: &7Imported tags from &6AlonsoTags&7.");
    }
}