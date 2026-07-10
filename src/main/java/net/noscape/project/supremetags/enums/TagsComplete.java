package net.noscape.project.supremetags.enums;

import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.utils.commands.BukkitCompleter;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TagsComplete extends BukkitCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (cmd.getName().equalsIgnoreCase("tags")) {
            if (args.length == 1) {
                if (sender.hasPermission("supremetags.admin")) {
                    completions.addAll(Arrays.asList("create", "settag", "givevoucher", "setcategory", "set", "reload", "removetagp", "help", "config", "list", "editor", "merge", "delete", "debug", "reset", "withdraw"));
                } else {
                    completions.addAll(Arrays.asList("withdraw", "help"));
                }

                } else if (args.length == 2) {
                if (sender.hasPermission("supremetags.admin")) {
                    if (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("settag") || args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("reset")) {
                        // Retrieve available tag names
                        completions.addAll(SupremeTags.getInstance().getTagManager().getTags().keySet());
                    } else if (args[0].equalsIgnoreCase("set")) {
                        // Add code here to retrieve available player names
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            completions.add(player.getName());
                        }
                    } else if (args[0].equalsIgnoreCase("setcategory")) {
                        // Retrieve available category names
                        if (args.length == 0) {
                            completions.addAll(SupremeTags.getInstance().getTagManager().getTags().keySet());
                        } else if (args.length == 1) {
                            completions.addAll(SupremeTags.getInstance().getCategoryManager().getCatorgies());
                        }
                    }
                }
            } else if (args.length == 3) {
                if (sender.hasPermission("supremetags.admin")) {
                    if (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("settag")) {
                        // Add code here to retrieve available tag values
                        completions.addAll(SupremeTags.getInstance().getTagManager().getTags().keySet());
                    } else if (args[0].equalsIgnoreCase("setcategory")) {
                        String categoryName = args[1];
                        completions.addAll(SupremeTags.getInstance().getCategoryManager().getCatorgies());
                    } else if (args[0].equalsIgnoreCase("set")) {
                        // Add code here to retrieve available tag names
                        completions.addAll(SupremeTags.getInstance().getTagManager().getTags().keySet());
                    }
                }
            }
        }

        return completions;
    }
}