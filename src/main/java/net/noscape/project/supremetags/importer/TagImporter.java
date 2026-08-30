package net.noscape.project.supremetags.importer;

import net.noscape.project.supremetags.SupremeTags;
import org.bukkit.command.CommandSender;

import java.io.File;

public interface TagImporter {

    String getPluginName();

    File getConfigFile();

    void importTags(SupremeTags plugin, CommandSender sender, boolean force);
}
