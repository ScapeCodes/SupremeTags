package net.noscape.project.supremetags.commands.tags;

import de.rapha149.signgui.SignGUI;
import de.rapha149.signgui.exception.SignGUIVersionException;
import me.clip.placeholderapi.PlaceholderAPI;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.enums.TPermissions;
import net.noscape.project.supremetags.guis.MainMenu;
import net.noscape.project.supremetags.guis.TagMenu;
import net.noscape.project.supremetags.guis.configeditor.ConfigOneMenu;
import net.noscape.project.supremetags.guis.search.SearchResultMenu;
import net.noscape.project.supremetags.guis.tageditor.EditorSelectorMenu;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.managers.TagManager;
import net.noscape.project.supremetags.storage.UserData;
import net.noscape.project.supremetags.utils.BungeeMessaging;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
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
    private String tag_removed = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.tag-removed").replaceAll("%prefix%", Objects.requireNonNull(SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.prefix")));

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
        if (!sender.hasPermission(TPermissions.ADMIN)) {
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

        if (!SupremeTags.getInstance().isNoPermissionMenuAction()) {
            if (!player.hasPermission(TPermissions.PLAYER)) {
                msgPlayer(player, noperm);
                playConfigSound(player, "error-message");
                return;
            }
        }

        if (!SupremeTags.getInstance().isDisabledWorldsTag()) {
            boolean lockedView = SupremeTags.getInstance().getConfig().getBoolean("settings.locked-view");
            boolean costSystem = SupremeTags.getInstance().getConfig().getBoolean("settings.cost-system");
            boolean useCategories = SupremeTags.getInstance().getConfig().getBoolean("settings.categories");

            if ((!lockedView && !costSystem)) {
                if (!hasTags(player)) {
                    msgPlayer(player, notags);
                    playConfigSound(player, "error-message");
                } else {
                    if (useCategories) {
                        new MainMenu(SupremeTags.getMenuUtil(player)).open();
                        playConfigSound(player, "open-menus");
                    } else {
                        new TagMenu(SupremeTags.getMenuUtil(player)).open();
                        playConfigSound(player, "open-menus");
                    }
                }
            } else {
                if (useCategories)  {
                    new MainMenu(SupremeTags.getMenuUtil(player)).open();
                    playConfigSound(player, "open-menus");
                }  else {
                    new TagMenu(SupremeTags.getMenuUtil(player)).open();
                    playConfigSound(player, "open-menus");
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
                                playConfigSound(player, "open-menus");
                            } else {
                                new TagMenu(SupremeTags.getMenuUtil(player)).open();
                                playConfigSound(player, "open-menus");
                            }
                        } else {
                            msgPlayer(player, notags);
                            playConfigSound(player, "error-message");
                        }
                    } else {
                        if (useCategories) {
                            new MainMenu(SupremeTags.getMenuUtil(player)).open();
                            playConfigSound(player, "open-menus");
                        } else {
                            new TagMenu(SupremeTags.getMenuUtil(player)).open();
                            playConfigSound(player, "open-menus");
                        }
                    }
                }
                break;
            }
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission(TPermissions.ADMIN)) {
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
        if (!sender.hasPermission(TPermissions.ADMIN)) {
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
        if (!sender.hasPermission(TPermissions.ADMIN)) {
            if (!SupremeTags.getInstance().isNoPermissionMenuAction()) {
                msgPlayer(sender, noperm);
            } else {
                handleMainCommand(sender, Bukkit.getPlayer(sender.getName()));
            }
            return;
        }

        if (player != null) {
            new ConfigOneMenu(SupremeTags.getMenuUtil(player)).open();
            playConfigSound((Player) sender, "open-menus");
        } else {
            sender.sendMessage("Only players can use this command");
        }
    }

    private void handleList(CommandSender sender) {
        if (!sender.hasPermission(TPermissions.ADMIN)) {
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
        if (!sender.hasPermission(TPermissions.SEARCH)) {
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
        if (!sender.hasPermission(TPermissions.ADMIN)) {
            if (!SupremeTags.getInstance().isNoPermissionMenuAction()) {
                msgPlayer(sender, noperm);
                playConfigSound((Player) sender, "error-message");
            } else {
                handleMainCommand(sender, Bukkit.getPlayer(sender.getName()));
            }
            return;
        }

        if (player != null) {
            new EditorSelectorMenu(SupremeTags.getMenuUtil(player)).open();
            playConfigSound(player, "open-menus");
        } else {
            sender.sendMessage("Only players can use this command");
        }
    }

    private void handleMerge(CommandSender sender, boolean isFree) {
        if (sender != null) {
            if (!sender.hasPermission(TPermissions.ADMIN)) {
                if (!SupremeTags.getInstance().isNoPermissionMenuAction()) {
                    msgPlayer(sender, noperm);
                    playConfigSound((Player) sender, "error-message");
                } else {
                    handleMainCommand(sender, Bukkit.getPlayer(sender.getName()));
                }
                return;
            }
        }

        SupremeTags.getInstance().getMergeManager().merge(sender, !isFree);
    }

    private void handleDelete(CommandSender sender, Player player, String[] args) {
        if (args.length < 2) {
            sendHelp(sender);
            return;
        }

        if (!sender.hasPermission(TPermissions.ADMIN)) {
            if (!SupremeTags.getInstance().isNoPermissionMenuAction()) {
                msgPlayer(sender, noperm);
                playConfigSound((Player) sender, "error-message");
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

    private void handleReset(CommandSender sender, Player player, String[] args) {

        // ───────────────────────────────────────────────────────────────
        // ✅ /tags reset                     → reset your own tag
        // ✅ /tags reset <player>            → reset another player (needs supremetags.reset.other)
        // ───────────────────────────────────────────────────────────────

        // ───────────────────────────────────────────────────────────────
        // ✅ Case 1: /tags reset (self reset)
        // ───────────────────────────────────────────────────────────────
        if (args.length == 1) {

            if (!(sender instanceof Player p)) {
                msgPlayer(sender, "&cConsole must use: /tags reset <player>");
                return;
            }

            if (!p.hasPermission("supremetags.reset")) {
                msgPlayer(p, noperm);
                playConfigSound(p, "error-message");
                return;
            }

            resetPlayerTag(p, p); // reset SELF
            return;
        }

        // ───────────────────────────────────────────────────────────────
        // ✅ Case 2: /tags reset <player> (other player reset)
        // ───────────────────────────────────────────────────────────────
        if (args.length >= 2) {

            if (!sender.hasPermission("supremetags.reset.other")) {
                msgPlayer(sender, noperm);
                if (sender instanceof Player pl) playConfigSound(pl, "error-message");
                return;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

            if (target == null || target.getName() == null) {
                msgPlayer(sender, player_not_online);
                return;
            }

            resetPlayerTag(sender, target);
        }
    }

    private void resetPlayerTag(CommandSender sender, OfflinePlayer target) {
        boolean forced = SupremeTags.getInstance().getConfig().getBoolean("settings.forced-tag");
        String defaultTag = SupremeTags.getInstance().getConfig().getString("settings.default-tag", "None");

        String no_tag_selected = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.no-tag-selected").replaceAll("%prefix%", Objects.requireNonNull(SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.prefix")));

        if (UserData.getActive(target.getUniqueId()).equalsIgnoreCase("None")) {
            msgPlayer(sender, no_tag_selected);
            return;
        }

        // Remove tag effects if online
        if (target.isOnline()) {
            Tag t = SupremeTags.getInstance().getTagManager().getTag(UserData.getActive(target.getUniqueId()));
            if (t != null) {
                t.removeEffects(target.getPlayer());
            }
        }

        // Set active value
        UserData.setActive(target, forced ? defaultTag : "None");
        msgPlayer(sender, reset.replace("%player%", target.getName()));
    }

    // Create Command - Create a new tag
    private void handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission(TPermissions.ADMIN)) {
            if (!SupremeTags.getInstance().isNoPermissionMenuAction()) {
                msgPlayer(sender, noperm);
            } else {
                handleMainCommand(sender, Bukkit.getPlayer(sender.getName()));
            }
            return;
        }

        if (args.length < 3) {
            msgPlayer(sender, "&cUsage: /" + SupremeTags.getInstance().getConfig().getString("settings.commands.main-command") + " create <name> <tag>");
            playConfigSound((Player) sender, "error-message");
            return;
        }

        if (args.length > 3) {
            msgPlayer(sender, "&cUsage: /" + SupremeTags.getInstance().getConfig().getString("settings.commands.main-command") + " create <name> <tag>");
            playConfigSound((Player) sender, "error-message");
            return;
        }

        String name = args[1];

        if (SupremeTags.getInstance().getTagManager().tagExists(name)) {
            msgPlayer(sender, validtag);
            playConfigSound((Player) sender, "error-message");
            return;
        }

        String tag = args[2];
        List<String> desc = new ArrayList<>();
        desc.add("&7My tag is " + name);

        SupremeTags.getInstance().getTagManager().createTag(sender, name, tag, desc, "supremetags.tag." + name, 100);
    }

    // RemoveTagP Command - Remove a tag from a player
    private void handleRemoveTagP(CommandSender sender, Player player, String[] args) {
        if (!sender.hasPermission(TPermissions.ADMIN)) {
            if (!SupremeTags.getInstance().isNoPermissionMenuAction()) {
                msgPlayer(sender, noperm);
                playConfigSound((Player) sender, "error-message");
            } else {
                handleMainCommand(sender, Bukkit.getPlayer(sender.getName()));
            }
            return;
        }
        if (args.length < 3) {
            msgPlayer(sender, "&cUsage: /tags removetagp <player> <tag>");
            playConfigSound((Player) sender, "error-message");
            return;
        }

        String playerName = args[1];
        String tag = args[2];

        boolean hasTag = false;

        if (!SupremeTags.getInstance().getTagManager().tagExists(tag)) {
            msgPlayer(sender, invalidtag);
            playConfigSound((Player) sender, "error-message");
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

            msgPlayer(sender, tag_removed.replace("%player%", Bukkit.getOfflinePlayer(playerName).getName()));
        }

        if (!hasTag) {
            msgPlayer(player, player_no_tag);
            playConfigSound(player, "error-message");
        }
    }

    // GiveVoucher Command - Give a tag voucher to a player
    private void handleGiveVoucher(CommandSender sender, String[] args) {
        if (!sender.hasPermission(TPermissions.ADMIN)) {
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
            msgPlayer(sender, given_voucher.replace("%target%", target.getName()).replace("%identifier%", name).replace("%tag%", SupremeTags.getInstance().getTagManager().getTag(name).getTag().getFirst()));

            if (!received_voucher.isEmpty()) {
                msgPlayer(target, received_voucher.replace("%identifier%", name).replace("%tag%", SupremeTags.getInstance().getTagManager().getTag(name).getTag().getFirst()));
            }
        } else {
            msgPlayer(sender, invalidtag);
            playConfigSound((Player) sender, "error-message");
        }
    }

    private void handleSetCategory(CommandSender sender, String[] args) {
        if (!sender.hasPermission(TPermissions.ADMIN)) {
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

        // ───────────────────────────────────────────────────────────────
        // ✅ /tags set <identifier>
        // ✅ /tags set <identifier> <player>
        // ───────────────────────────────────────────────────────────────

        if (args.length < 2) {
            msgPlayer(sender, "&cUsage: /tags set <identifier> [player]");
            return;
        }

        String identifier = args[1];
        boolean tagExists = SupremeTags.getInstance().getTagManager().getTags().containsKey(identifier);

        if (!tagExists) {
            msgPlayer(sender, invalidtag);
            return;
        }

        // ───────────────────────────────────────────────────────────────
        // ✅ Case 1: Player setting THEIR OWN tag → /tags set <identifier>
        // ───────────────────────────────────────────────────────────────
        if (args.length == 2) {
            if (!(sender instanceof Player player)) {
                msgPlayer(sender, "&cOnly players can set their own tags. Use /tags set <identifier> <player>");
                return;
            }

            if (!player.hasPermission("supremetags.set")) {
                msgPlayer(player, noperm);
                return;
            }

            if (!player.hasPermission(SupremeTags.getInstance().getTagManager().getTag(identifier).getPermission())) {
                sendLockedMessage(player);
                return;
            }

            UserData.setActive(player, identifier);
            String select = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.tag-select-message");
            if (select == null) {
                select = "&aYou have selected the tag %tag%"; // fallback value
            }

            select = select.replace("%prefix%", SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.prefix"));
            select = replacePlaceholders(player, select);
            msgPlayer(player, select
                    .replace("%identifier%", identifier)
                    .replace("%tag%", SupremeTags.getInstance().getTagManager().getTag(identifier).getTag().getFirst()));
            playConfigSound(player, "selected-tag");
            return;
        }

        // ───────────────────────────────────────────────────────────────
        // ✅ Case 2: Admin setting ANOTHER PLAYER’S TAG
        //     /tags set <identifier> <player>
        // ───────────────────────────────────────────────────────────────
        if (args.length >= 3) {

            if (!sender.hasPermission("supremetags.set.other")) {
                msgPlayer(sender, noperm);
                return;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);

            if (target == null || target.getName() == null) {
                msgPlayer(sender, player_not_online);
                return;
            }

            // Set tag for target
            UserData.setActive(target, identifier);

            msgPlayer(sender, "&eSet &b" + target.getName() + "'s &etag to &b" + identifier);

            Player onlineTarget = target.getPlayer();
            if (onlineTarget != null) {
                msgPlayer(onlineTarget, "&aYour active tag was set to &b" + identifier + " &aby an administrator.");
            }
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

        if (sender.hasPermission(TPermissions.ADMIN)) {
            subCommands = new String[]{
                    "create", "delete", "set", "givevoucher", "reset",
                    "removetagp", "merge", "reload", "help", "config", "editor",
                    "list", "withdraw", "debug", "search", "edit"
            };
        } else if (!sender.hasPermission(TPermissions.ADMIN) && sender.hasPermission(TPermissions.WITHDRAW) && sender.hasPermission(TPermissions.SEARCH)) {
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
                    case "set":
                        SupremeTags.getInstance().getTagManager().getTags().values().stream()
                                .filter(tag -> sender.hasPermission(tag.getPermission()))
                                .forEach(tag -> completions.add(tag.getIdentifier()));
                        break;
                    case "removetagp":
                    case "givevoucher":
                    case "reset":
                        completions.addAll(Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .toList());
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
                        if (sender.hasPermission("supremetags.set.other")) {
                            completions.addAll(Bukkit.getOnlinePlayers().stream()
                                    .map(Player::getName)
                                    .toList());
                        }
                        break;
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

    public void sendDebug(CommandSender sender) {

        FileConfiguration msg = SupremeTags.getInstance()
                .getConfigManager().getConfig("messages.yml").get();

        List<String> lines = msg.getStringList("messages.debug");
        if (lines.isEmpty()) {
            msgPlayer(sender, "&cDebug section missing in messages.yml");
            return;
        }

        Map<String, String> values = collectDebugValues(sender);

        for (String line : lines) {
            msgPlayer(sender, replace(line, values));
        }
    }

    private Map<String, String> collectDebugValues(CommandSender sender) {

        Map<String, String> map = new HashMap<>();

        String version = SupremeTags.getInstance().getDescription().getVersion();
        if (SupremeTags.getInstance().dev_build) {
            version += "-DEV#" + SupremeTags.getInstance().build;
        }

        map.put("%version%", version);
        map.put("%author%", "DevScape");
        map.put("%discord%", "https://discord.gg/AnPwty8asP");

        map.put("%tags_loaded%", String.valueOf(SupremeTags.getInstance().getTagManager().getTags().size()));
        map.put("%categories_loaded%", String.valueOf(SupremeTags.getInstance().getCategoryManager().getCatorgies().size()));

        String dbType = SupremeTags.getInstance()
                .getConfigManager().getConfig("data.yml").get().getString("data.type", "UNKNOWN");

        map.put("%db_type%", dbType);
        map.put("%db_connected%", UserData.isConnected() ? "&aYES" : "&cNO");

        map.put("%hook_vault%", hookString(SupremeTags.getInstance().isVaultAPI()));
        map.put("%hook_playerpoints%", hookString(isPlugin("PlayerPoints")));
        map.put("%hook_coinsengine%", hookString(SupremeTags.getInstance().isCoinsEngine()));
        map.put("%hook_nbtapi%", hookString(isPlugin("NBTAPI")));
        map.put("%hook_papi%", hookString(SupremeTags.getInstance().isPlaceholderAPI()));

        map.put("%config_errors%", formatMultiline(validateMainConfigCollect()));
        map.put("%papi_test_tag%", formatMultiline(debugPlaceholderTag(sender)));
        map.put("%papi_test_chat%", formatMultiline(debugPlaceholderChat(sender)));
        map.put("%tag_errors%", formatMultiline(debugTagErrorsCollect()));

        return map;
    }

    private List<String> debugPlaceholderChat(CommandSender sender) {

        List<String> out = new ArrayList<>();

        if (!SupremeTags.getInstance().isPlaceholderAPI()) {
            out.add("PlaceholderAPI not installed.");
            return out;
        }

        String val = PlaceholderAPI.setPlaceholders(
                sender instanceof Player ? (Player) sender : null,
                "%supremetags_chattag%"
        );

        if (val == null || val.isEmpty() || val.equalsIgnoreCase("null")) {
            out.add("Chat placeholder returned null/empty.");
        } else {
            out.add("Output: " + val);
        }

        return out;
    }

    private String replace(String line, Map<String, String> map) {
        for (Map.Entry<String, String> e : map.entrySet()) {
            line = line.replace(e.getKey(), e.getValue());
        }
        return line;
    }

    private String hookString(boolean found) {
        return found ? "&aFound" : "&cNot Found";
    }


    private String formatMultiline(List<String> list) {
        if (list.isEmpty()) return "&aNo issues found.";

        StringBuilder b = new StringBuilder();
        for (String s : list) {
            b.append("\n").append(" &c- ").append(s);
        }
        return b.toString().trim();
    }


    private List<String> debugPlaceholderTag(CommandSender sender) {

        List<String> out = new ArrayList<>();

        if (!SupremeTags.getInstance().isPlaceholderAPI()) {
            out.add("PlaceholderAPI not installed.");
            return out;
        }

        String val = PlaceholderAPI.setPlaceholders(
                sender instanceof Player ? (Player) sender : null,
                "%supremetags_tag%"
        );

        if (val == null || val.isEmpty() || val.equalsIgnoreCase("null")) {
            out.add("Tag placeholder returned null/empty.");
        } else {
            out.add("Output: " + val);
        }

        return out;
    }


    private List<String> debugTagErrorsCollect() {

        List<String> errors = new ArrayList<>();

        Collection<Tag> tags = SupremeTags.getInstance().getTagManager().getTags().values();

        if (tags.isEmpty()) {
            errors.add("No tags loaded.");
            return errors;
        }

        for (Tag tag : tags) {

            if (tag.getIdentifier() == null || tag.getIdentifier().trim().isEmpty()) {
                errors.add("Tag missing identifier.");
            }
            if (tag.getDescription() == null || tag.getDescription().isEmpty()) {
                errors.add("Tag '" + tag.getIdentifier() + "' missing description.");
            }
            if (tag.getPermission() == null || tag.getPermission().trim().isEmpty()) {
                errors.add("Tag '" + tag.getIdentifier() + "' missing permission.");
            }
            if (SupremeTags.getInstance().getConfig().getBoolean("settings.categories")) {
                if (tag.getCategory() == null || tag.getCategory().trim().isEmpty()) {
                    errors.add("Tag '" + tag.getIdentifier() + "' missing category.");
                }
            }
        }

        return errors;
    }



    private void hook(CommandSender sender, String name, boolean found) {
        msgPlayer(sender, " &8● &7" + name + ": " + (found ? "&aFound" : "&cNot found"));
    }

    private boolean isPlugin(String name) {
        return Bukkit.getPluginManager().getPlugin(name) != null;
    }

    private List<String> validateMainConfigCollect() {

        FileConfiguration config = SupremeTags.getInstance().getConfig();
        List<String> errors = new ArrayList<>();

        if (!config.isConfigurationSection("settings")) {
            errors.add("Missing section: settings");
            return errors;
        }

        ConfigurationSection settings = config.getConfigurationSection("settings");

        // Required keys inside "settings"
        checkKey(settings, "commands.main-command", errors, String.class);
        checkKey(settings, "commands.aliases", errors, List.class);
        checkKey(settings, "no-permission-menu-action", errors, Boolean.class);
        checkKey(settings, "messaging-platform", errors, String.class);
        checkKey(settings, "bungee-messaging", errors, Boolean.class);
        checkKey(settings, "default-tag", errors, String.class);
        checkKey(settings, "forced-tag", errors, Boolean.class);
        checkKey(settings, "categories", errors, Boolean.class);
        checkKey(settings, "default-category", errors, String.class);
        checkKey(settings, "update-check", errors, Boolean.class);
        checkKey(settings, "auto-merge", errors, Boolean.class);
        checkKey(settings, "active-tag-glow", errors, Boolean.class);
        checkKey(settings, "tag-command-in-disabled-worlds", errors, Boolean.class);
        checkKey(settings, "disabled-worlds", errors, List.class);
        checkKey(settings, "gui-messages", errors, Boolean.class);
        checkKey(settings, "locked-view", errors, Boolean.class);

        // personal-tags
        checkKey(settings, "personal-tags.enable", errors, Boolean.class);
        checkKey(settings, "personal-tags.limits", errors, ConfigurationSection.class);
        checkKey(settings, "personal-tags.format-replace", errors, String.class);

        // layout + tag behavior
        checkKey(settings, "layout-type", errors, String.class);
        checkKey(settings, "animated-tag-speed", errors, Integer.class);
        checkKey(settings, "tag-vouchers", errors, Boolean.class);
        checkKey(settings, "prioritise-selected-tag", errors, Boolean.class);
        checkKey(settings, "voucher-redeem-permission", errors, Boolean.class);
        checkKey(settings, "deactivate-click", errors, Boolean.class);
        checkKey(settings, "only-show-player-access-tags", errors, Boolean.class);
        checkKey(settings, "search-type", errors, String.class);
        checkKey(settings, "update-unlocked-cache", errors, Integer.class);

        // ---------------------------
        // ✅ Validate specific values
        // ---------------------------

        String messaging = settings.getString("messaging-platform", "").toLowerCase();
        if (!messaging.equals("bungeecord") && !messaging.equals("velocity")) {
            errors.add("Invalid messaging-platform: " + messaging + " (must be bungeecord or velocity)");
        }

        String layout = settings.getString("layout-type", "").toUpperCase();
        if (!layout.equals("FULL") && !layout.equals("BORDER")) {
            errors.add("Invalid layout-type: " + layout + " (must be FULL or BORDER)");
        }

        String searchType = settings.getString("search-type", "").toUpperCase();
        if (!searchType.equals("SIGN") && !searchType.equals("ANVIL")) {
            errors.add("Invalid search-type: " + searchType + " (must be SIGN or ANVIL)");
        }

        // ---------------------------
        // ✅ placeholders section
        // ---------------------------

        List<String> phKeys = Arrays.asList("tag", "chat", "scoreboard", "tab");

        for (String key : phKeys) {
            checkKey(config, "placeholders." + key + ".none-output", errors, String.class);
            checkKey(config, "placeholders." + key + ".format", errors, String.class);
        }

        // ---------------------------
        // ✅ sounds section
        // ---------------------------

        List<String> soundKeys = Arrays.asList("open-menus", "selected-tag", "reset-tag", "error-message");

        for (String key : soundKeys) {
            checkKey(config, "sounds." + key + ".enable", errors, Boolean.class);
            checkKey(config, "sounds." + key + ".sound", errors, String.class);
            checkKey(config, "sounds." + key + ".volume", errors, Double.class);
            checkKey(config, "sounds." + key + ".pitch", errors, Double.class);
        }

        return errors;
    }

    private void sendConfigResult(CommandSender sender, List<String> errors) {
        if (errors.isEmpty()) {
            msgPlayer(sender, " &8● &aConfig.yml validated successfully. No issues found.");
        } else {
            msgPlayer(sender, " &8● &cConfig.yml Errors Found:");
            for (String err : errors) {
                msgPlayer(sender, "   &c- " + err);
            }
        }
    }

    private void checkKey(ConfigurationSection sec, String path, List<String> errors, Class<?> type) {
        if (!sec.contains(path)) {
            errors.add("Missing key: " + path);
            return;
        }

        Object val = sec.get(path);
        if (!type.isInstance(val)) {
            errors.add("Invalid type for '" + path + "' (expected " + type.getSimpleName() + ")");
        }
    }



    public void sendDebugOLD(CommandSender player) {
        msgPlayer(player, "");
        msgPlayer(player, "&fDebugging SupremeTags2 &8➜");
        msgPlayer(player, "");
        msgPlayer(player, "&7Version: &f" + SupremeTags.getInstance().getDescription().getVersion());
        msgPlayer(player, "&7Author: &fDevScape (aka. Scape)");
        msgPlayer(player, "&7Discord:&f https://discord.gg/AnPwty8asP");
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
            msgPlayer(player, " &8● &7Vault: &fFound.");
        } else {
            msgPlayer(player, " &8● &7Vault: &fNot found.");
        }

        if (Bukkit.getPluginManager().getPlugin("PlayerPoints") != null) {
            msgPlayer(player, " &8● &7PlayerPoints: &fFound.");
        } else {
            msgPlayer(player, " &8● &7PlayerPoints: &fNot found.");
        }

        if (SupremeTags.getInstance().isCoinsEngine()) {
            msgPlayer(player, " &8● &7CoinsEngine: &fFound.");
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

    protected void sendLockedMessage(Player player) {
            String locked = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.locked-tag")
                    .replace("%prefix%", Objects.requireNonNull(SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.prefix")));
            locked = replacePlaceholders(player, locked);
            msgPlayer(player, locked);
    }
}