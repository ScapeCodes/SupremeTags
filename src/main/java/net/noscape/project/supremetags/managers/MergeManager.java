package net.noscape.project.supremetags.managers;

import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.importer.TagImporter;
import net.noscape.project.supremetags.importer.types.AlonsoTagsImporter;
import net.noscape.project.supremetags.importer.types.DeluxeTagsImporter;
import net.noscape.project.supremetags.importer.types.EternalTagsImporter;
import net.noscape.project.supremetags.importer.types.FreeSupremeTagsImporter;
import net.noscape.project.supremetags.utils.Utils;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class MergeManager {

    private final SupremeTags plugin;
    private final List<TagImporter> importers = new ArrayList<>();

    public MergeManager(SupremeTags plugin) {
        this.plugin = plugin;
        registerImporters();
    }

    private void registerImporters() {
        importers.add(new DeluxeTagsImporter());
        importers.add(new EternalTagsImporter());
        importers.add(new AlonsoTagsImporter());
        importers.add(new FreeSupremeTagsImporter());
    }

    public void merge(CommandSender sender, boolean force) {
        boolean autoMerge = plugin.getConfig().getBoolean("settings.auto-merge");

        if (!autoMerge && !force) {
            Utils.msgPlayer(sender, "&6Merger: &7Auto-merge is disabled in config.yml.");
            return;
        }

        for (TagImporter importer : importers) {
            try {
                importer.importTags(plugin, sender, force);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to import from " + importer.getPluginName() + ": " + e.getMessage());
            }
        }
    }
}