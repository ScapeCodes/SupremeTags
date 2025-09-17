package net.noscape.project.supremetags.commands.tags;

import de.rapha149.signgui.SignGUI;
import de.rapha149.signgui.exception.SignGUIVersionException;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.guis.MainMenu;
import net.noscape.project.supremetags.guis.TagMenu;
import net.noscape.project.supremetags.guis.configeditor.ConfigOneMenu;
import net.noscape.project.supremetags.guis.search.SearchResultMenu;
import net.noscape.project.supremetags.guis.tageditor.EditorSelectorMenu;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.storage.UserData;
import net.noscape.project.supremetags.utils.BungeeMessaging;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

import static net.noscape.project.supremetags.utils.Utils.*;

public class TagsCommand implements CommandExecutor, TabCompleter {
    
    private String noperm = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.no-permission").replaceAll("%prefix%", Objects.requireNonNull(SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.prefix")));
    private String notags = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.no-tags").replaceAll("%prefix%", Objects.requireNonNull(SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.prefix")));
    private String tagedited = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.tag-edited").replaceAll("%prefix%", Objects.requireNonNull(SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.prefix")));
    private String commanddisabled = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.tag-command-disabled").replaceAll("%prefix%", Objects.requireNonNull(SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.prefix")));
    private String invalidtag = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.invalid-tag").replaceAll("%prefix%", Objects.requireNonNull(SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.prefix")));
    private String validtag = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.valid-tag").replaceAll("%prefix%", Objects.requireNonNull(SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.prefix")));
    private String player_no_tag = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.player-not-have-tag").replaceAll("%prefix%", Objects.requireNonNull(SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.prefix")));
    private String player_not_online = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.player-not-online").replaceAll("%prefix%", Objects.requireNonNull(SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.prefix")));
    private String given_voucher = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.given-voucher").replaceAll("%prefix%", Objects.requireNonNull(SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.prefix")));
    private String received_voucher = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.received-voucher").replaceAll("%prefix%", Objects.requireNonNull(SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.prefix")));
    private String reset = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.reset-command").replaceAll("%prefix%", Objects.requireNonNull(SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.prefix")));

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (sender instanceof Player) ? (Player) sender : null;

        if (args.length == 0) {
            handleMainCommand(sender, player);
        } else {
            switch (args[0].toLowerCase()) {
                case "reload":
                    handleReload(sender);
                    break;
                case "debug":
                    handleDebug(sender);
                    break;
                case "config":
                    handleConfig(sender, player);
                    break;
                case "list":
                    handleList(sender);
                    break;
                case "search":
                    handleSearch(sender, player);
                    break;
                case "editor":
                    handleEditor(sender, player);
                    break;
                case "merge":
                    handleMerge(sender, false);
                    break;
                case "merge-free":
                    handleMerge(sender, true);
                    break;
                case "delete":
                    handleDelete(sender, player, args);
                    break;
                case "withdraw":
                    handleWithdraw(sender, player, args);
                    break;
                case "reset":
                    handleReset(sender, player, args);
                    break;
                case "create":
                    handleCreate(sender, args);
                    break;
                case "removetagp":
                    handleRemoveTagP(sender, player, args);
                    break;
                case "givevoucher":
                    handleGiveVoucher(sender, args);
                    break;
                case "set":
                    handleSet(sender, args);
                    break;
                case "edit":
                    handleEdit(sender, args);
                    break;
                case "help":
                    sendHelp(sender);
                    break;
                default:
                    handleMainCommand(sender, player);
                    break;
            }
        }
        return true;
    }

    private void handleEdit(CommandSender sender, String[] args) {
        if (!sender.hasPermission("supremetags.admin")) {
            msgPlayer(sender, noperm);
            return;
        }

        String tag = args[1];
        String option = args[2];
        String value = args[3];

        if (!SupremeTags.getInstance().getTagManager().doesTagExist(tag)) {
            msgPlayer(sender, invalidtag);
            return;
        }

        Tag t = SupremeTags.getInstance().getTagManager().getTag(tag);

        if (option.equalsIgnoreCase("tag")) {
            List<String> tlist = new ArrayList<>();
            tlist.add(value);

            t.setTag(tlist);
            msgPlayer(sender, tagedited.replace("%tag%", t.getIdentifier()));
        } else if (option.equalsIgnoreCase("permission")) {
            t.setPermission(value);
            msgPlayer(sender, tagedited.replace("%tag%", t.getIdentifier()));
        } else if (option.equalsIgnoreCase("category")) {
            t.setCategory(value);
            msgPlayer(sender, tagedited.replace("%tag%", t.getIdentifier()));
        } else if (option.equalsIgnoreCase("cost")) {
            try {
                double cost = Double.parseDouble(value);
                t.getEconomy().setAmount(cost);
                msgPlayer(sender, tagedited.replace("%tag%", t.getIdentifier()));
            } catch (NumberFormatException e) {
                msgPlayer(sender, "&cThe cost must be a valid number.");
            }
        } else if (option.equalsIgnoreCase("withdrawable")) {
            if (value.equalsIgnoreCase("true")) {
                t.setWithdrawable(true);
                msgPlayer(sender, tagedited.replace("%tag%", t.getIdentifier()));
            } else if (value.equalsIgnoreCase("false")) {
                t.setWithdrawable(false);
                msgPlayer(sender, tagedited.replace("%tag%", t.getIdentifier()));
            } else {
                msgPlayer(sender, "&cRequires: true or false.");
            }
        }
    }

    private void handleMainCommand(CommandSender sender, Player player) {
        if (player == null) {
            sendHelp(sender);
            return;
        }

        if (!player.hasPermission("supremetags.player")) {
            msgPlayer(player, noperm);
            return;
        }

        if (!SupremeTags.getInstance().isDisabledWorldsTag()) {
            boolean lockedView = SupremeTags.getInstance().getConfig().getBoolean("settings.locked-view");
            boolean costSystem = SupremeTags.getInstance().getConfig().getBoolean("settings.cost-system");
            boolean useCategories = SupremeTags.getInstance().getConfig().getBoolean("settings.categories");

            if ((!lockedView && !costSystem)) {
                if (!hasTags(player)) {
                    msgPlayer(player, notags);
                } else {
                    if (useCategories) {
                        new MainMenu(SupremeTags.getMenuUtil(player)).open();
                    } else {
                        new TagMenu(SupremeTags.getMenuUtil(player)).open();
                    }
                }
            } else {
                if (useCategories) new MainMenu(SupremeTags.getMenuUtil(player)).open();
                else {
                    new TagMenu(SupremeTags.getMenuUtil(player)).open();
                }
            }
        } else {
            for (String world : SupremeTags.getInstance().getConfig().getStringList("settings.disabled-worlds")) {
                if (player.getWorld().getName().equalsIgnoreCase(world)) {
                    msgPlayer(player, commanddisabled);
                } else {
                    boolean lockedView = SupremeTags.getInstance().getConfig().getBoolean("settings.locked-view");
                    boolean costSystem = SupremeTags.getInstance().getConfig().getBoolean("settings.cost-system");
                    boolean useCategories = SupremeTags.getInstance().getConfig().getBoolean("settings.categories");

                    if ((!lockedView && !costSystem)) {
                        if (hasTags(player)) {
                            if (useCategories) {
                                new MainMenu(SupremeTags.getMenuUtil(player)).open();
                            } else {
                                new TagMenu(SupremeTags.getMenuUtil(player)).open();
                            }
                        } else {
                            msgPlayer(player, notags);
                        }
                    } else {
                        if (useCategories) {
                            new MainMenu(SupremeTags.getMenuUtil(player)).open();
                        } else {
                            new TagMenu(SupremeTags.getMenuUtil(player)).open();
                        }
                    }
                }
                break;
            }
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("supremetags.admin")) {
            if (!SupremeTags.getInstance().isNoPermissionMenuAction()) {
                msgPlayer(sender, noperm);
            } else {
                handleMainCommand(sender, Bukkit.getPlayer(sender.getName()));
            }
            return;
        }

        if (SupremeTags.getInstance().getConfig().getBoolean("settings.bungee-messaging")) {
            BungeeMessaging.sendReload();
        }

        SupremeTags.getInstance().reload();

        SupremeTags.getInstance().getConfigManager().reloadConfig("messages.yml");
        msgPlayer(sender, SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.reload").replaceAll("%prefix%", Objects.requireNonNull(SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.prefix"))));
    }

    private void handleDebug(CommandSender sender) {
        if (!sender.hasPermission("supremetags.admin")) {
            if (!SupremeTags.getInstance().isNoPermissionMenuAction()) {
                msgPlayer(sender, noperm);
            } else {
                handleMainCommand(sender, Bukkit.getPlayer(sender.getName()));
            }
            return;
        }

        sendDebug(sender);
    }

    private void handleConfig(CommandSender sender, Player player) {
        if (!sender.hasPermission("supremetags.admin")) {
            if (!SupremeTags.getInstance().isNoPermissionMenuAction()) {
                msgPlayer(sender, noperm);
            } else {
                handleMainCommand(sender, Bukkit.getPlayer(sender.getName()));
            }
            return;
        }
        if (player != null) {
            new ConfigOneMenu(SupremeTags.getMenuUtil(player)).open();
        } else {
            sender.sendMessage("Only players can use this command");
        }
    }

    private void handleList(CommandSender sender) {
        if (!sender.hasPermission("supremetags.admin")) {
            if (!SupremeTags.getInstance().isNoPermissionMenuAction()) {
                msgPlayer(sender, noperm);
            } else {
                handleMainCommand(sender, Bukkit.getPlayer(sender.getName()));
            }
            return;
        }

        msgPlayer(sender,
                "&e&lTags &8➜ &7There are &f" + SupremeTags.getInstance().getTagManager().getTags().size() + " &7tags loaded!",
                "&e&lTags &8➜ &7There are &f" + SupremeTags.getInstance().getCategoryManager().getCatorgies().size() + " &7categories loaded!",
                "&e&lTags &8➜ &7Do &f/tags editor &7to see/edit all tags loaded!");
    }

    private void handleSearch(CommandSender sender, Player player) {
        if (!sender.hasPermission("supremetags.search")) {
            if (!SupremeTags.getInstance().isNoPermissionMenuAction()) {
                msgPlayer(sender, noperm);
            } else {
                handleMainCommand(sender, Bukkit.getPlayer(sender.getName()));
            }
            return;
        }

        if (player != null) {
            openSearchSign(player);
        } else {
            sender.sendMessage("Only players can use this command");
        }
    }

    private void handleEditor(CommandSender sender, Player player) {
        if (!sender.hasPermission("supremetags.admin")) {
            if (!SupremeTags.getInstance().isNoPermissionMenuAction()) {
                msgPlayer(sender, noperm);
            } else {
                handleMainCommand(sender, Bukkit.getPlayer(sender.getName()));
            }
            return;
        }

        if (player != null) {
            new EditorSelectorMenu(SupremeTags.getMenuUtil(player)).open();
        } else {
            sender.sendMessage("Only players can use this command");
        }
    }

    private void handleMerge(CommandSender sender, boolean isFree) {
        if (sender != null) {
            if (!sender.hasPermission("supremetags.admin")) {
                if (!SupremeTags.getInstance().isNoPermissionMenuAction()) {
                    msgPlayer(sender, noperm);
                } else {
                    handleMainCommand(sender, Bukkit.getPlayer(sender.getName()));
                }
                return;
            }
        }

        if (!isFree) {
            SupremeTags.getInstance().getMergeManager().mergeForced(sender);
        } else {
            SupremeTags.getInstance().getMergeManager().mergeFromFree(sender);
        }
    }

    private void handleDelete(CommandSender sender, Player player, String[] args) {
        if (args.length < 2) {
            sendHelp(sender);
            return;
        }
        if (!sender.hasPermission("supremetags.admin")) {
            if (!SupremeTags.getInstance().isNoPermissionMenuAction()) {
                msgPlayer(sender, noperm);
            } else {
                handleMainCommand(sender, Bukkit.getPlayer(sender.getName()));
            }
            return;
        }
        String name = args[1];
        SupremeTags.getInstance().getTagManager().deleteTag(sender, name);
    }

    // Withdraw Command - Withdraw a tag voucher for the player
    private void handleWithdraw(CommandSender sender, Player player, String[] args) {
        if (args.length < 2) {
            msgPlayer(sender, "&cUsage: /tags withdraw <tag>");
            return;
        }

        String tagName = args[1];
        if (!SupremeTags.getInstance().getTagManager().tagExists(tagName)) {
            msgPlayer(sender, invalidtag);
            return;
        }

        SupremeTags.getInstance().getVoucherManager().withdrawTag(player, tagName);
    }

    // Reset Command - Reset a player's tag to the default or "None"
    private void handleReset(CommandSender sender, Player player, String[] args) {
        if (!sender.hasPermission("supremetags.admin")) {
            if (!SupremeTags.getInstance().isNoPermissionMenuAction()) {
                msgPlayer(sender, noperm);
            } else {
                handleMainCommand(sender, Bukkit.getPlayer(sender.getName()));
            }
            return;
        }
        if (args.length < 2) {
            msgPlayer(sender, "&cUsage: /tags reset <player>");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

        if (SupremeTags.getInstance().getConfig().isBoolean("settings.forced-tag")) {
            String defaultTag = SupremeTags.getInstance().getConfig().getString("settings.default-tag");

            if (target.isOnline()) {
                Tag t = SupremeTags.getInstance().getTagManager().getTag(UserData.getActive(target.getUniqueId()));
                if (t != null) {
                    t.removeEffects(Objects.requireNonNull(target.getPlayer()));
                }
            }

            UserData.setActive(target, defaultTag);

            if (Bukkit.getServer().getPluginManager().getPlugin("Vault") != null) {
                for (Tag tag : SupremeTags.getInstance().getTagManager().getTags().values()) {
                    String permission = tag.getPermission();
                    if (player.hasPermission(permission)) {
                        removePerm(target.getPlayer(), permission);
                    }
                }
            } else {
                Bukkit.getLogger().warning("Vault not found!");
            }

            msgPlayer(player, reset.replaceAll("%player%", target.getName()));
        } else {
            UserData.setActive(target, "None");
            msgPlayer(player, reset.replaceAll("%player%", target.getName()));
        }
    }

    // Create Command - Create a new tag
    private void handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("supremetags.admin")) {
            if (!SupremeTags.getInstance().isNoPermissionMenuAction()) {
                msgPlayer(sender, noperm);
            } else {
                handleMainCommand(sender, Bukkit.getPlayer(sender.getName()));
            }
            return;
        }

        if (args.length < 3) {
            msgPlayer(sender, "&cUsage: /" + SupremeTags.getInstance().getConfig().getString("settings.commands.main-command") + " create <name> <tag>");
            return;
        }

        if (args.length > 3) {
            msgPlayer(sender, "&cUsage: /" + SupremeTags.getInstance().getConfig().getString("settings.commands.main-command") + " create <name> <tag>");
            return;
        }

        String name = args[1];

        if (SupremeTags.getInstance().getTagManager().tagExists(name)) {
            msgPlayer(sender, validtag);
            return;
        }

        String tag = args[2];
        List<String> desc = new ArrayList<>();
        desc.add("&7My tag is " + name);

        SupremeTags.getInstance().getTagManager().createTag(sender, name, tag, desc, "supremetags.tag." + name, 100);
    }

    // RemoveTagP Command - Remove a tag from a player
    private void handleRemoveTagP(CommandSender sender, Player player, String[] args) {
        if (!sender.hasPermission("supremetags.admin")) {
            if (!SupremeTags.getInstance().isNoPermissionMenuAction()) {
                msgPlayer(sender, noperm);
            } else {
                handleMainCommand(sender, Bukkit.getPlayer(sender.getName()));
            }
            return;
        }
        if (args.length < 3) {
            msgPlayer(sender, "&cUsage: /tags removetagp <player> <tag>");
            return;
        }

        String playerName = args[1];
        String tag = args[2];

        boolean hasTag = false;

        if (!SupremeTags.getInstance().getTagManager().tagExists(tag)) {
            msgPlayer(sender, invalidtag);
            return;
        }

        String permission = SupremeTags.getInstance().getTagManager().getTag(tag).getPermission();

        for (World world : Bukkit.getWorlds()) {
            if (SupremeTags.getPermissions().playerHas(world.getName(), Bukkit.getOfflinePlayer(playerName), permission)) {
                SupremeTags.getPermissions().playerRemove(world.getName(), Bukkit.getOfflinePlayer(playerName), permission);
                hasTag = true;
            }
        }

        if (hasTag) {
            if (UserData.getActive(Bukkit.getOfflinePlayer(playerName).getUniqueId()).equalsIgnoreCase(tag)) {
                UserData.setActive(Bukkit.getOfflinePlayer(playerName), "None");
            }

            msgPlayer(player, "&8[&6&lTag&8] &7You have removed the tag from " + Bukkit.getOfflinePlayer(playerName).getName() + "!");
        }

        if (!hasTag) {
            msgPlayer(player, player_no_tag);
        }
    }

    // SetTag Command - Set a specific tag for the player
    private void handleSetTag(CommandSender sender, String[] args) {
        if (!sender.hasPermission("supremetags.admin")) {
            if (!SupremeTags.getInstance().isNoPermissionMenuAction()) {
                msgPlayer(sender, noperm);
            } else {
                handleMainCommand(sender, Bukkit.getPlayer(sender.getName()));
            }
            return;
        }
        if (args.length < 2) {
            msgPlayer(sender, "&cUsage: /tags settag <identifier> <tag>");
            return;
        }

        String name = args[1];

        if (!SupremeTags.getInstance().getTagManager().tagExists(name)) {
            msgPlayer(sender, invalidtag);
            return;
        }

        String tag = args[2];

        SupremeTags.getInstance().getTagManager().setTag(sender, name, tag);
    }

    // GiveVoucher Command - Give a tag voucher to a player
    private void handleGiveVoucher(CommandSender sender, String[] args) {
        if (!sender.hasPermission("supremetags.admin")) {
            if (!SupremeTags.getInstance().isNoPermissionMenuAction()) {
                msgPlayer(sender, noperm);
            } else {
                handleMainCommand(sender, Bukkit.getPlayer(sender.getName()));
            }
            return;
        }

        if (args.length < 3) {
            msgPlayer(sender, "&cUsage: /tags givevoucher <player> <tag>");
            return;
        }

        String name = args[2];
        String target_name = args[1];

        Player target = Bukkit.getPlayer(target_name);

        if (target == null) {
            msgPlayer(sender, player_not_online);
            return;
        }

        if (SupremeTags.getInstance().getTagManager().getTag(name) != null) {
            SupremeTags.getInstance().getVoucherManager().giveVoucher(target, name);
            msgPlayer(sender, given_voucher.replaceAll("%target%", target.getName()).replaceAll("%identifier%", name));

            if (!received_voucher.equals("")) {
                msgPlayer(target, received_voucher.replaceAll("%identifier%", name));
            }
        } else {
            msgPlayer(sender, invalidtag);
        }
    }

    private void handleSetCategory(CommandSender sender, String[] args) {
        if (!sender.hasPermission("supremetags.admin")) {
            if (!SupremeTags.getInstance().isNoPermissionMenuAction()) {
                msgPlayer(sender, noperm);
            } else {
                handleMainCommand(sender, Bukkit.getPlayer(sender.getName()));
            }
            return;
        }
        if (args.length < 3) {
            msgPlayer(sender, "&cUsage: /tags setcategory <tag> <category>");
            return;
        }

        String name = args[1];

        if (!SupremeTags.getInstance().getTagManager().tagExists(name)) {
            msgPlayer(sender, invalidtag);
            return;
        }

        String category = args[2];

        SupremeTags.getInstance().getTagManager().setCategory(sender, name, category);
    }
    private void handleSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("supremetags.admin")) {
            if (!SupremeTags.getInstance().isNoPermissionMenuAction()) {
                msgPlayer(sender, noperm);
            } else {
                handleMainCommand(sender, Bukkit.getPlayer(sender.getName()));
            }
            return;
        }
        if (args.length < 3) {
            msgPlayer(sender, "&cUsage: /tags set <player> <tag>");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        String identifier = args[2];

        if (SupremeTags.getInstance().getTagManager().getTags().containsKey(identifier)) {
            UserData.setActive(target, identifier);
            msgPlayer(sender, "&8[&6&lTag&8] &7Set &b" + target.getName() + "'s &7tag to &b" + identifier);
        } else {
            msgPlayer(sender, invalidtag);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Retrieve aliases and the main command from the config
        List<String> aliases = SupremeTags.getInstance().getConfig().getStringList("settings.commands.aliases");
        String mainCommand = SupremeTags.getInstance().getConfig().getString("settings.commands.main-command", "tags");

        // Add the main command to the aliases list for completion
        List<String> allCommands = new ArrayList<>(aliases);
        allCommands.add(mainCommand);

        // Convert command name to lower case for comparison
        String label = command.getName().toLowerCase();

        // Create a list for completions
        List<String> completions = new ArrayList<>();

        // Define available subcommands
        String[] subCommands = new String[0];

        String[] edits;

        if (sender.hasPermission("supremetags.admin")) {
            subCommands = new String[]{
                    "create", "delete", "set", "givevoucher", "reset",
                    "removetagp", "merge", "reload", "help", "config", "editor",
                    "list", "withdraw", "debug", "search", "edit"
            };
        } else if (!sender.hasPermission("supremetags.admin") && sender.hasPermission("supremetags.withdraw") && sender.hasPermission("supremetags.search")) {
            subCommands = new String[]{
                    "withdraw", "search"
            };
        }

        // Check if the command label is one of the aliases or the main command
        if (allCommands.contains(label)) {
            // First argument completion (subcommands)
            if (args.length == 1) {
                // Suggest subcommands when typing the first argument
                for (String subCommand : subCommands) {
                    if (subCommand.startsWith(args[0].toLowerCase())) {
                        completions.add(subCommand);
                    }
                }
            }

            // Second argument completion for specific subcommands
            else if (args.length == 2) {
                String firstArg = args[0].toLowerCase();

                switch (firstArg) {
                    case "create":
                        completions.add("Name");
                        break;
                    case "delete":
                    case "withdraw":
                    case "edit":
                        completions.addAll(SupremeTags.getInstance().getTagManager().getTags().keySet());
                        break;
                    case "removetagp":
                    case "set":
                    case "givevoucher":
                    case "reset":
                        completions.addAll(Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .collect(Collectors.toList()));
                        break;
                    default:
                        break;
                }
            }

            // Third argument completion for commands that require a tag or more complex arguments
            else if (args.length == 3) {
                String firstArg = args[0].toLowerCase();

                switch (firstArg) {
                    case "create":
                        completions.add("Tag");
                        break;
                    case "givevoucher":
                    case "set":
                    case "removetagp":
                        // Suggest <tag> for these commands
                        completions.addAll(SupremeTags.getInstance().getTagManager().getTags().keySet());
                        break;
                    case "edit":
                        edits = new String[]{
                                "tag", "permission", "cost", "withdrawable", "category"
                        };

                        completions.addAll(Arrays.stream(edits).toList());
                        break;
                    default:
                        break;
                }
            }

            // Fourth argument completion for the givevoucher command (optional flag -s)
            else if (args.length == 4) {
                if (args[0].equalsIgnoreCase("givevoucher")) {
                    completions.add("-s");
                } else if (args[0].equalsIgnoreCase("edit") && args[2].equalsIgnoreCase("tag")) {
                    String tagName = args[1];
                    if (SupremeTags.getInstance().getTagManager().tagExists(tagName)) {
                        completions.addAll(SupremeTags.getInstance().getTagManager().getTag(tagName).getTag());
                    }
                }
            }
        }

        return completions;
    }

    public boolean hasTags(Player player) {
        for (Tag tag : SupremeTags.getInstance().getTagManager().getTags().values()) {
            if (player.hasPermission(tag.getPermission())) {
                return true;
            }
        }
        return false;
    }

    public void sendHelp(CommandSender sender) {
        if (sender == null) {
            for (String msg : SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getStringList("messages.help.admin")) {
                msgPlayer(Bukkit.getConsoleSender(), msg.replaceAll("%prefix%", Objects.requireNonNull(SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.prefix"))).replaceAll("%command%", SupremeTags.getInstance().getConfig().getString("settings.commands.main-command")));
            }
            return;
        }

        if (sender instanceof Player player) {
            if (player.hasPermission("sc.admin")) {
                for (String msg : SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getStringList("messages.help.admin")) {
                    msgPlayer(player, msg.replaceAll("%prefix%", Objects.requireNonNull(SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.prefix"))).replaceAll("%command%", SupremeTags.getInstance().getConfig().getString("settings.commands.main-command")));
                }
            } else {
                for (String msg : SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getStringList("messages.help.default")) {
                    msgPlayer(player, msg.replaceAll("%prefix%", Objects.requireNonNull(SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.prefix"))).replaceAll("%command%", SupremeTags.getInstance().getConfig().getString("settings.commands.main-command")));
                }
            }
        } else {
            for (String msg : SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getStringList("messages.help.admin")) {
                msgPlayer(sender, msg.replaceAll("%prefix%", Objects.requireNonNull(SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.prefix"))).replaceAll("%command%", SupremeTags.getInstance().getConfig().getString("settings.commands.main-command")));
            }
        }
    }

    public void sendDebug(CommandSender player) {
        msgPlayer(player, "");
        msgPlayer(player, "&fDebugging SupremeTags2 &8➜");
        msgPlayer(player, "");
        msgPlayer(player, "&7Version: &f" + SupremeTags.getInstance().getDescription().getVersion());
        msgPlayer(player, "&7Author: &fDevScape (aka. Scape)");
        msgPlayer(player, "");
        msgPlayer(player, "&7Tags loaded: &f" + SupremeTags.getInstance().getTagManager().getTags().size());
        msgPlayer(player, "&7Categories loaded: &f" + SupremeTags.getInstance().getCategoryManager().getCatorgies().size());
        msgPlayer(player, "&7Database Assigned: &f" + SupremeTags.getInstance().getConfigManager().getConfig("data.yml").get().getString("data.type"));
        if (UserData.isConnected()) {
            msgPlayer(player, "&7Database Connected: &fYES");
        } else {
            msgPlayer(player, "&7Database Connected: &fNO");
        }
        msgPlayer(player, "");
        msgPlayer(player, "&e&lPlugins Hooked:");
        if (SupremeTags.getInstance().isVaultAPI()) {
            if (SupremeTags.getInstance().getConfig().getString("settings.economy").equalsIgnoreCase("VAULT")) {
                msgPlayer(player, " &8● &7Vault: &fFound. (Economy Type)");
            } else {
                msgPlayer(player, " &8● &7Vault: &fFound.");
            }
        } else {
            msgPlayer(player, " &8● &7Vault: &fNot found.");
        }

        if (Bukkit.getPluginManager().getPlugin("PlayerPoints") != null) {
            if (SupremeTags.getInstance().getConfig().getString("settings.economy").equalsIgnoreCase("PLAYERPOINTS")) {
                msgPlayer(player, " &8● &7PlayerPoints: &fFound. (Economy Type)");
            } else {
                msgPlayer(player, " &8● &7PlayerPoints: &fFound.");
            }
        } else {
            msgPlayer(player, " &8● &7PlayerPoints: &fNot found.");
        }

        if (SupremeTags.getInstance().isCoinsEngine()) {
            if (SupremeTags.getInstance().getConfig().getString("settings.economy").startsWith("COINSENGINE-")) {
                msgPlayer(player, " &8● &7CoinsEngine: &fFound. (Economy Type)");
            } else {
                msgPlayer(player, " &8● &7CoinsEngine: &fFound.");
            }
        } else {
            msgPlayer(player, " &8● &7CoinsEngine: &fNot found.");
        }

        if (SupremeTags.getInstance().getServer().getPluginManager().getPlugin("NBTAPI") != null) {
            msgPlayer(player, " &8● &7NBTAPI: &fFound.");
        } else {
            msgPlayer(player, " &8● &7NBTAPI: &fNot found.");
        }

        if (SupremeTags.getInstance().isPlaceholderAPI()) {
            msgPlayer(player, " &8● &7PlaceholderaAPI: &fFound.");
        } else {
            msgPlayer(player, " &8● &7PlaceholderaAPI: &fNot found.");
        }
    }

    public void openSearchSign(Player player) {
        SignGUI gui;
        try {
            gui = SignGUI.builder()
                    .setLines(format(SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.sign-line-top")), null, null)
                    .setColor(DyeColor.YELLOW)

                    .setHandler((p, result) -> {
                        String line1 = result.getLineWithoutColor(1);

                        if (!line1.isEmpty()) {
                            if (SupremeTags.getInstance().getCategoryManager().isCategoryNearName(line1) || SupremeTags.getInstance().getTagManager().tagExistsNearName(line1)) {
                                Bukkit.getScheduler().runTask(SupremeTags.getInstance(), () -> new SearchResultMenu(SupremeTags.getMenuUtil(player), line1).open());
                            } else {
                                String search_invalid = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.search-invalid-1").replaceAll("%prefix%", Objects.requireNonNull(SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.prefix")));
                                msgPlayer(player, search_invalid);
                            }
                        } else {
                            String search_invalid = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.search-invalid-2").replaceAll("%prefix%", Objects.requireNonNull(SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.prefix")));
                            msgPlayer(player, search_invalid);
                        }

                        return Collections.emptyList();
                    })

                    .build();
        } catch (SignGUIVersionException e) {
            throw new RuntimeException(e);
        }

        gui.open(player);
    }
}