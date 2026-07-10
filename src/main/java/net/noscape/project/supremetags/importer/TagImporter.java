package net.noscape.project.supremetags.importer;

import net.noscape.project.supremetags.SupremeTags;
import org.bukkit.command.CommandSender;

import java.io.File;

public interface TagImporter {

    /**
     * The display name of the tag plugin (e.g., "DeluxeTags").
     */
    String getPluginName();

    /**
     * The configuration file of the tag plugin.
     */
    File getConfigFile();

    /**
     * Imports tags into SupremeTags.
     *
     * @param plugin SupremeTags instance
     * @param sender Command sender for messages
     * @param force Whether to override existing tags
     */
    void importTags(SupremeTags plugin, CommandSender sender, boolean force);
}