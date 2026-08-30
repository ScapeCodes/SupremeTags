package net.noscape.project.supremetags.commands.tags;

import de.rapha149.signgui.SignGUI;
import de.rapha149.signgui.exception.SignGUIVersionException;
import me.clip.placeholderapi.PlaceholderAPI;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.enums.TPermissions;
import net.noscape.project.supremetags.editorweb.TagEditorExportService;
import net.noscape.project.supremetags.editorweb.TagEditorImportService;
import net.noscape.project.supremetags.editorweb.TagEditorSessionClient;
import net.noscape.project.supremetags.editorweb.TagEditorValidationResult;
import net.noscape.project.supremetags.editorweb.TagDumpExportService;
import net.noscape.project.supremetags.guis.FavouritesMenu;
import net.noscape.project.supremetags.guis.MainMenu;
import net.noscape.project.supremetags.guis.TagMenu;
import net.noscape.project.supremetags.guis.TagShowcaseMenu;
import net.noscape.project.supremetags.guis.configeditor.ConfigEditor;
import net.noscape.project.supremetags.guis.confirm.ConfirmationMenu;
import net.noscape.project.supremetags.guis.search.SearchResultMenu;
import net.noscape.project.supremetags.guis.tageditor.EditorSelectorMenu;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.storage.UserData;
import net.noscape.project.supremetags.utils.Utils;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

import static net.noscape.project.supremetags.utils.Utils.*;

public class TagsCommand implements CommandExecutor, TabCompleter {

    private final FileConfiguration messages = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get();

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
                case "dump":
                    handleDump(sender);
                    break;
                case "credits":
                    handleCredits(sender, args);
                    break;
                case "config":
                    handleConfig(sender, player);
                    break;
                case "list":
                    handleList(sender);
                    break;
                case "stats":
                    handleStats(sender, args);
                    break;
                case "search":
                    handleSearch(sender, player);
                    break;
                case "editor":
                    handleEditor(sender, player, args);
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
                case "move":
                    handleMove(sender, args);
                    break;
                case "seteveryone":
                    handleSetEveryone(sender, player, args);
                    break;
                case "reseteveryone":
                    handleResetEveryone(sender, player);
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
                case "setcustomtag":
                    handleSetCustomTag(sender, args);
                    break;
                case "resetcustomtag":
                    handleResetCustomTag(sender, args);
                    break;
                case "edit":
                    handleEdit(sender, args);
                    break;
                case "help":
                    sendHelp(sender);
                    break;
                case "favourites":
                    handleFavourites(sender, player);
                    break;
                case "view":
                    handleView(sender, player, args);
                    break;
                default:
                    handleMainCommand(sender, player);
                    break;
            }
        }
        return true;
    }

    private void handleFavourites(CommandSender sender, Player player) {
        if (player != null) {
            new FavouritesMenu(SupremeTags.getMenuUtil(player)).open();
            playConfigSound((Player) sender, "open-menus");
        } else {
            msgPlayer(sender, msg("messages.players-only"));
        }
    }

    private void handleView(CommandSender sender, Player player, String[] args) {
        if (!sender.hasPermission(TPermissions.VIEW)) {
            msgPlayer(sender, noperm);
            return;
        }

        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                msgPlayer(sender, player_not_online);
                return;
            }

            if (!sender.hasPermission(TPermissions.VIEW_OTHER) && !target.equals(player)) {
                msgPlayer(sender, noperm);
                return;
            }
        } else {
            if (player == null) {
                msgPlayer(sender, msg("messages.usage.view"));
                return;
            }
            target = player;
        }

        if (player != null) {
            new TagShowcaseMenu(SupremeTags.getMenuUtil(player), target).open();
            playConfigSound(player, "open-menus");
        } else {
            msgPlayer(sender, msg("messages.players-only"));
        }
    }

    private void handleEdit(CommandSender sender, String[] args) {
        if (!sender.hasPermission(TPermissions.ADMIN)) {
            msgPlayer(sender, noperm);
            return;
        }

        if (args.length < 4) {
            msgPlayer(sender, msg("messages.usage.edit"));
            return;
        }

        String tag = args[1];
        String option = args[2];
        String value = String.join(" ", Arrays.copyOfRange(args, 3, args.length));

        if (!SupremeTags.getInstance().getTagManager().doesTagExist(tag)) {
            msgPlayer(sender, invalidtag);
            return;
        }

        Tag t = SupremeTags.getInstance().getTagManager().getTag(tag);
        boolean edited = false;

        if (option.equalsIgnoreCase("tag")) {
            List<String> tlist = new ArrayList<>();
            tlist.add(value);

            t.setTag(tlist);
            edited = true;
            msgPlayer(sender, tagedited.replace("%tag%", t.getIdentifier()));
        } else if (option.equalsIgnoreCase("permission")) {
            t.setPermission(value);
            edited = true;
            msgPlayer(sender, tagedited.replace("%tag%", t.getIdentifier()));
        } else if (option.equalsIgnoreCase("category")) {
            if (!SupremeTags.getInstance().getCategoryManager().isCategory(value)) {
                msgPlayer(sender, msg("messages.invalid-category"));
                return;
            }

            t.setCategory(value);
            edited = true;
            msgPlayer(sender, tagedited.replace("%tag%", t.getIdentifier()));
        } else if (option.equalsIgnoreCase("cost")) {
            try {
                double cost = Double.parseDouble(value);
                t.getEconomy().setAmount(cost);
                edited = true;
                msgPlayer(sender, tagedited.replace("%tag%", t.getIdentifier()));
            } catch (NumberFormatException e) {
                msgPlayer(sender, msg("messages.invalid-cost"));
            }
        } else if (option.equalsIgnoreCase("withdrawable")) {
            if (value.equalsIgnoreCase("true")) {
                t.setWithdrawable(true);
                edited = true;
                msgPlayer(sender, tagedited.replace("%tag%", t.getIdentifier()));
            } else if (value.equalsIgnoreCase("false")) {
                t.setWithdrawable(false);
                edited = true;
                msgPlayer(sender, tagedited.replace("%tag%", t.getIdentifier()));
            } else {
                msgPlayer(sender, msg("messages.requires-boolean"));
            }
        } else if (option.equalsIgnoreCase("rarity")) {
            if (!SupremeTags.getInstance().getRarityManager().isValid(value)) {
                msgPlayer(sender, msg("messages.invalid-rarity"));
                return;
            }

            t.setRarity(value);
            edited = true;
            msgPlayer(sender, tagedited.replace("%tag%", t.getIdentifier()));
        }

        if (edited) {
            SupremeTags.getInstance().getTagManager().saveTag(t);
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

    private void handleDump(CommandSender sender) {
        if (!sender.hasPermission(TPermissions.ADMIN)) {
            if (!SupremeTags.getInstance().isNoPermissionMenuAction()) {
                msgPlayer(sender, noperm);
            } else if (sender instanceof Player player) {
                handleMainCommand(sender, player);
            }
            return;
        }

        TagEditorSessionClient client = new TagEditorSessionClient(SupremeTags.getInstance());
        if (!client.isConfigured()) {
            msgPlayer(sender, msg("messages.dump.api-not-configured", "%prefix% <reset><red>Set <reset><white>editor.api-url <reset><red>in config.yml before creating dumps."));
            return;
        }

        runAsync(() -> {
            try {
                String json = new TagDumpExportService(SupremeTags.getInstance()).exportJson();
                TagEditorSessionClient.CreateDumpResponse dump = client.createDump(json);
                msgPlayer(sender, msg("messages.dump.created", "%prefix% <reset><green>Created dump: <reset><white>%dump_id%")
                        .replace("%dump_id%", dump.id));
                msgPlayer(sender, msg("messages.dump.open", "%prefix% <reset><gray>Open: <click:open_url:'%dump_url%'><hover:show_text:'<yellow>Click to open the dump<newline><gray>%dump_url%'><aqua><underlined>Open dump</underlined></aqua></hover></click>")
                        .replace("%dump_url%", dump.dumpUrl));
            } catch (Exception exception) {
                msgPlayer(sender, msg("messages.dump.failed", "%prefix% <reset><red>Failed to create dump: <reset><white>%error%")
                        .replace("%error%", exception.getMessage()));
            }
        });
    }

    private void handleCredits(CommandSender sender, String[] args) {
        FileConfiguration messages = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get();
        String prefix = Objects.requireNonNull(messages.getString("messages.prefix"));

        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                msgPlayer(sender, msg("messages.usage.credits"));
                return;
            }

            long credits = UserData.getTagCredits(player.getUniqueId());
            msgPlayer(player, messages.getString("messages.tag-credits-balance", "%prefix% &7You have &e%credits% Tag Credits&7.")
                    .replace("%prefix%", prefix)
                    .replace("%credits%", String.valueOf(credits)));
            return;
        }

        if (args.length == 2) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            if (target.getName() == null || !target.hasPlayedBefore()) {
                msgPlayer(sender, player_not_online);
                return;
            }

            long credits = UserData.getTagCredits(target.getUniqueId());
            msgPlayer(sender, messages.getString("messages.tag-credits-balance", "%prefix% &7You have &e%credits% Tag Credits&7.")
                    .replace("%prefix%", prefix)
                    .replace("%credits%", String.valueOf(credits))
                    .replace("You have", target.getName() + " has"));
            return;
        }

        if (!sender.hasPermission(TPermissions.ADMIN)) {
            msgPlayer(sender, noperm);
            return;
        }

        if (args.length < 4) {
            msgPlayer(sender, msg("messages.usage.credits-admin"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        if (target.getName() == null || !target.hasPlayedBefore()) {
            msgPlayer(sender, player_not_online);
            return;
        }

        long amount;
        try {
            amount = Long.parseLong(args[3]);
        } catch (NumberFormatException exception) {
            msgPlayer(sender, msg("messages.amount-whole-number"));
            return;
        }

        if (amount < 0L) {
            msgPlayer(sender, msg("messages.amount-positive"));
            return;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        if (action.equals("give") || action.equals("add")) {
            UserData.addTagCredits(target, amount);
            msgPlayer(sender, messages.getString("messages.tag-credits-added", "%prefix% &7Added &e%amount% Tag Credits &7to &e%player%&7. New balance: &e%credits%&7.")
                    .replace("%prefix%", prefix)
                    .replace("%player%", String.valueOf(target.getName()))
                    .replace("%amount%", String.valueOf(amount))
                    .replace("%credits%", String.valueOf(UserData.getTagCredits(target.getUniqueId()))));
        } else if (action.equals("take") || action.equals("remove")) {
            UserData.setTagCredits(target, Math.max(0L, UserData.getTagCredits(target.getUniqueId()) - amount));
            msgPlayer(sender, messages.getString("messages.tag-credits-removed", "%prefix% &7Removed &e%amount% Tag Credits &7from &e%player%&7. New balance: &e%credits%&7.")
                    .replace("%prefix%", prefix)
                    .replace("%player%", String.valueOf(target.getName()))
                    .replace("%amount%", String.valueOf(amount))
                    .replace("%credits%", String.valueOf(UserData.getTagCredits(target.getUniqueId()))));
        } else if (action.equals("set")) {
            UserData.setTagCredits(target, amount);
            msgPlayer(sender, messages.getString("messages.tag-credits-updated", "%prefix% &7Set &e%player%&7 Tag Credits to &e%credits%&7.")
                    .replace("%prefix%", prefix)
                    .replace("%player%", String.valueOf(target.getName()))
                    .replace("%credits%", String.valueOf(UserData.getTagCredits(target.getUniqueId()))));
        } else {
            msgPlayer(sender, msg("messages.usage.credits-admin"));
        }
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
            new ConfigEditor(SupremeTags.getMenuUtil(player)).open();
            playConfigSound((Player) sender, "open-menus");
        } else {
            msgPlayer(sender, msg("messages.players-only"));
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

    private void handleStats(CommandSender sender, String[] args) {
        if (!sender.hasPermission(TPermissions.ADMIN)) {
            msgPlayer(sender, noperm);
            return;
        }

        if (args.length < 2) {
            String topTag = SupremeTags.getInstance().getTagStatisticsManager().getTopTag();
            msgPlayer(sender,
                    "&e&lTag Stats &8➜ &7Total selections: &f" + SupremeTags.getInstance().getTagStatisticsManager().getTotalSelections(),
                    "&e&lTag Stats &8➜ &7Top tag: &f" + (topTag.isBlank() ? "None" : topTag),
                    "&e&lTag Stats &8➜ &7Usage: &f/tags stats <tag>");
            return;
        }

        String identifier = args[1];
        if (!SupremeTags.getInstance().getTagManager().doesTagExist(identifier)) {
            msgPlayer(sender, invalidtag);
            return;
        }

        msgPlayer(sender,
                "&e&lTag Stats &8➜ &f" + identifier,
                "&7Selections: &f" + SupremeTags.getInstance().getTagStatisticsManager().getTagSelections(identifier),
                "&7Unique Users: &f" + SupremeTags.getInstance().getTagStatisticsManager().getTagUniqueUsers(identifier),
                "&7Active Users: &f" + SupremeTags.getInstance().getTagStatisticsManager().getTagActiveUsers(identifier),
                "&7Rank: &f#" + SupremeTags.getInstance().getTagStatisticsManager().getTagRank(identifier),
                "&7First Selected: &f" + SupremeTags.getInstance().getTagStatisticsManager().formatTimestamp(SupremeTags.getInstance().getTagStatisticsManager().getTagFirstSelected(identifier)),
                "&7Last Selected: &f" + SupremeTags.getInstance().getTagStatisticsManager().formatTimestamp(SupremeTags.getInstance().getTagStatisticsManager().getTagLastSelected(identifier)));
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
            msgPlayer(sender, msg("messages.players-only"));
        }
    }

    private void handleEditor(CommandSender sender, Player player, String[] args) {
        if (!sender.hasPermission(TPermissions.ADMIN)) {
            if (!SupremeTags.getInstance().isNoPermissionMenuAction()) {
                msgPlayer(sender, noperm);
                if (sender instanceof Player deniedPlayer) {
                    playConfigSound(deniedPlayer, "error-message");
                }
            } else {
                handleMainCommand(sender, Bukkit.getPlayer(sender.getName()));
            }
            return;
        }

        if (args.length >= 2) {
            switch (args[1].toLowerCase(Locale.ROOT)) {
                case "apply":
                    handleEditorApplyShort(sender, args);
                    return;
                case "web":
                    handleEditorWeb(sender);
                    return;
                default:
                    msgPlayer(sender, msg("messages.editor.web.unknown-action", "%prefix% <reset><red>Unknown editor action. Use <reset><yellow>web <reset><red>or <reset><yellow>apply<reset><red>."));
                    return;
            }
        }

        if (player != null) {
            new EditorSelectorMenu(SupremeTags.getMenuUtil(player)).open();
            playConfigSound(player, "open-menus");
        } else {
            msgPlayer(sender, msg("messages.players-only"));
        }
    }

    private void handleEditorApplyShort(CommandSender sender, String[] args) {
        if (args.length < 3) {
            msgPlayer(sender, msg("messages.editor.web.apply-usage", "%prefix% <reset><red>Usage: /tags editor apply <sessionId>"));
            return;
        }

        String sessionId = args[2];
        String applyToken = SupremeTags.getInstance().getTagEditorSessionManager().getWebApplyToken(sessionId);
        if (applyToken == null) {
            msgPlayer(sender, msg("messages.editor.web.session-expired", "%prefix% <reset><red>Unknown or expired editor session."));
            msgPlayer(sender, msg("messages.editor.web.create-new-session", "%prefix% <reset><gray>Create a new session with <reset><yellow>/tags editor web<reset><gray>."));
            return;
        }

        handleEditorApplyStoredSession(sender, sessionId, applyToken);
    }

    private void handleEditorWeb(CommandSender sender) {
        TagEditorSessionClient client = new TagEditorSessionClient(SupremeTags.getInstance());
        if (!client.isConfigured()) {
            msgPlayer(sender, msg("messages.editor.web.api-not-configured", "%prefix% <reset><red>Set <reset><white>editor.api-url <reset><red>in config.yml before using web sessions."));
            return;
        }

        runAsync(() -> {
            try {
                String json = new TagEditorExportService(SupremeTags.getInstance()).exportJson();
                TagEditorSessionClient.CreateSessionResponse session = client.createSession(json);
                SupremeTags.getInstance().getTagEditorSessionManager().registerWebSession(session.id, session.applyToken);
                String editorUrl = client.buildEditorUrl(session.editToken);
                if (editorUrl.isBlank()) {
                    editorUrl = session.editorUrl;
                }
                msgPlayer(sender, msg("messages.editor.web.session-created", "%prefix% <reset><green>Created editor session: <reset><white>%session_id%")
                        .replace("%session_id%", session.id));
                msgPlayer(sender, msg("messages.editor.web.open", "%prefix% <reset><gray>Open: <click:open_url:'%editor_url%'><hover:show_text:'<yellow>Click to open the web editor<newline><gray>%editor_url%'><aqua><underlined>Open editor</underlined></aqua></hover></click>")
                        .replace("%editor_url%", editorUrl));
                msgPlayer(sender, msg("messages.editor.web.apply-later", "%prefix% <reset><gray>Apply later: <click:suggest_command:'%apply_command%'><hover:show_text:'<yellow>Click to paste this command<newline><gray>%apply_command%'><white>%apply_command%</white></hover></click>")
                        .replace("%session_id%", session.id)
                        .replace("%apply_command%", "/tags editor apply " + session.id));
            } catch (Exception exception) {
                msgPlayer(sender, msg("messages.editor.web.create-failed", "%prefix% <reset><red>Failed to create editor session: <reset><white>%error%")
                        .replace("%error%", exception.getMessage()));
            }
        });
    }

    private void handleEditorApplyStoredSession(CommandSender sender, String sessionId, String applyToken) {
        TagEditorSessionClient client = new TagEditorSessionClient(SupremeTags.getInstance());
        if (!client.isConfigured()) {
            msgPlayer(sender, msg("messages.editor.web.api-not-configured-apply", "%prefix% <reset><red>Set <reset><white>editor.api-url <reset><red>in config.yml before applying web sessions."));
            return;
        }

        runAsync(() -> {
            try {
                String json = client.fetchEditedPayload(sessionId, applyToken);
                if (SupremeTags.getInstance().isDBTags()) {
                    TagEditorImportService.ApplyResult result = new TagEditorImportService(SupremeTags.getInstance()).applyJson(json, sender);
                    runMain(() -> {
                        sendApplyResult(sender, result);
                        SupremeTags.getInstance().getTagEditorSessionManager().removeWebSession(sessionId);
                    });
                } else {
                    runMain(() -> {
                        sendApplyResult(sender, new TagEditorImportService(SupremeTags.getInstance()).applyJson(json, sender));
                        SupremeTags.getInstance().getTagEditorSessionManager().removeWebSession(sessionId);
                    });
                }
            } catch (Exception exception) {
                runMain(() -> {
                    msgPlayer(sender, msg("messages.editor.web.apply-failed", "%prefix% <reset><red>Failed to apply editor session: <reset><white>%error%")
                            .replace("%error%", exception.getMessage()));
                    msgPlayer(sender, msg("messages.editor.web.create-new-session-expired", "%prefix% <reset><gray>If the session expired, create a new one with <reset><yellow>/tags editor web<reset><gray>."));
                });
            }
        });
    }

    private void sendApplyResult(CommandSender sender, TagEditorImportService.ApplyResult result) {
        if (!result.isSuccess()) {
            msgPlayer(sender, msg("messages.editor.web.apply-validation-failed", "%prefix% <reset><red>%message%")
                    .replace("%message%", result.getMessage()));
            TagEditorValidationResult validation = result.getValidation();
            if (validation != null) {
                for (String error : validation.getErrors()) {
                    msgPlayer(sender, msg("messages.editor.web.validation-error", "<reset><red>- %error%")
                            .replace("%error%", error));
                }
            }
            return;
        }

        msgPlayer(sender, msg("messages.editor.web.apply-success", "%prefix% <reset><green>Applied editor payload. <reset><gray>Payload: <reset><white>%payload% <reset><gray>Created: <reset><white>%created% <reset><gray>Updated: <reset><white>%updated% <reset><gray>Deleted: <reset><white>%deleted%")
                .replace("%payload%", String.valueOf(result.getPayloadTagCount()))
                .replace("%created%", String.valueOf(result.getCreated()))
                .replace("%updated%", String.valueOf(result.getUpdated()))
                .replace("%deleted%", String.valueOf(result.getDeleted())));
        if (!result.getDisplayChanges().isEmpty()) {
            msgPlayer(sender, msg("messages.editor.web.changes-header", "%prefix% <reset><gray>Changes:"));
            int shown = 0;
            for (String change : result.getDisplayChanges()) {
                if (shown >= 8) {
                    msgPlayer(sender, msg("messages.editor.web.changes-more", "%prefix% <reset><gray>...and <reset><white>%amount% <reset><gray>more.")
                            .replace("%amount%", String.valueOf(result.getDisplayChanges().size() - shown)));
                    break;
                }
                msgPlayer(sender, msg("messages.editor.web.change-entry", "<reset><dark_gray>- <reset><white>%change%")
                        .replace("%change%", change));
                shown++;
            }
        } else {
            msgPlayer(sender, msg("messages.editor.web.no-changes", "%prefix% <reset><gray>No editable tag values changed."));
        }
        if (result.getValidation() != null) {
            for (String warning : result.getValidation().getWarnings()) {
                msgPlayer(sender, msg("messages.editor.web.validation-warning", "<reset><yellow>- %warning%")
                        .replace("%warning%", warning));
            }
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
                if (sender instanceof Player deniedPlayer) {
                    playConfigSound(deniedPlayer, "error-message");
                }
            } else if (sender instanceof Player fallbackPlayer) {
                handleMainCommand(sender, fallbackPlayer);
            }
            return;
        }
        String name = args[1];
        if (player != null) {
            new ConfirmationMenu(SupremeTags.getMenuUtil(player), "delete-tag:" + name).open();
        } else {
            SupremeTags.getInstance().getTagManager().deleteTag(sender, name);
        }
    }

    private void handleMove(CommandSender sender, String[] args) {
        if (!sender.hasPermission(TPermissions.ADMIN)) {
            if (!SupremeTags.getInstance().isNoPermissionMenuAction()) {
                msgPlayer(sender, noperm);
            } else if (sender instanceof Player player) {
                handleMainCommand(sender, player);
            }
            return;
        }

        if (args.length < 3) {
            msgPlayer(sender, msg("messages.usage.move"));
            return;
        }

        String identifier = args[1];
        String targetFileLocation = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        if (!SupremeTags.getInstance().getTagManager().tagExists(identifier)) {
            msgPlayer(sender, invalidtag);
            return;
        }

        if (sender instanceof Player player) {
            new ConfirmationMenu(SupremeTags.getMenuUtil(player), "move-tag:" + identifier + "|" + targetFileLocation).open();
        } else {
            SupremeTags.getInstance().getTagManager().moveTag(sender, identifier, targetFileLocation);
        }
    }

    private void handleSetEveryone(CommandSender sender, Player player, String[] args) {
        if (!sender.hasPermission(TPermissions.ADMIN)) {
            msgPlayer(sender, noperm);
            return;
        }

        if (args.length < 2) {
            msgPlayer(sender, msg("messages.usage.set-everyone"));
            return;
        }

        String identifier = args[1];
        if (!SupremeTags.getInstance().getTagManager().tagExists(identifier)) {
            msgPlayer(sender, invalidtag);
            return;
        }

        if (player != null) {
            new ConfirmationMenu(SupremeTags.getMenuUtil(player), "set-everyone:" + identifier).open();
        } else {
            int updated = UserData.setActiveForEveryone(identifier);
            msgPlayer(sender, msg("messages.set-everyone-command")
                    .replace("%identifier%", identifier)
                    .replace("%updated%", String.valueOf(updated)));
        }
    }

    private void handleResetEveryone(CommandSender sender, Player player) {
        if (!sender.hasPermission(TPermissions.ADMIN)) {
            msgPlayer(sender, noperm);
            return;
        }

        if (player != null) {
            new ConfirmationMenu(SupremeTags.getMenuUtil(player), "reset-everyone").open();
        } else {
            int updated = UserData.setActiveForEveryone("None");
            msgPlayer(sender, msg("messages.reset-everyone-command")
                    .replace("%updated%", String.valueOf(updated)));
        }
    }

    private void handleWithdraw(CommandSender sender, Player player, String[] args) {
        if (args.length < 2) {
            msgPlayer(sender, msg("messages.usage.withdraw"));
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

        if (args.length == 1 || (args.length == 2 && args[1].equalsIgnoreCase("-s"))) {

            if (!(sender instanceof Player p)) {
                msgPlayer(sender, msg("messages.usage.reset-console"));
                return;
            }

            if (!p.hasPermission("supremetags.reset")) {
                msgPlayer(p, noperm);
                playConfigSound(p, "error-message");
                return;
            }

            boolean silent = args.length == 2 && args[1].equalsIgnoreCase("-s");
            resetPlayerTag(p, p, silent);
            return;
        }

        if (args.length >= 2) {

            if (!sender.hasPermission("supremetags.reset.other")) {
                msgPlayer(sender, noperm);
                if (sender instanceof Player pl) playConfigSound(pl, "error-message");
                return;
            }

            boolean silent = args.length >= 3 && args[2].equalsIgnoreCase("-s");
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

            if (target == null || target.getName() == null) {
                msgPlayer(sender, player_not_online);
                return;
            }

            resetPlayerTag(sender, target, silent);
        }
    }

    private void resetPlayerTag(CommandSender sender, OfflinePlayer target, boolean silent) {
        boolean forced = SupremeTags.getInstance().getConfig().getBoolean("settings.forced-tag");
        String defaultTag = SupremeTags.getInstance().getConfig().getString("settings.default-tag", "None");

        String no_tag_selected = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.no-tag-selected").replaceAll("%prefix%", Objects.requireNonNull(SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.prefix")));

        if (UserData.getActive(target.getUniqueId()).equalsIgnoreCase("None")) {
            msgPlayer(sender, no_tag_selected);
            return;
        }

        if (target.isOnline()) {
            Tag t = SupremeTags.getInstance().getTagManager().getTag(UserData.getActive(target.getUniqueId()));
            if (t != null) {
                t.removeEffects(target.getPlayer());
            }
        }

        UserData.setActive(target, forced ? defaultTag : "None");
        if (!silent) {
            msgPlayer(sender, reset.replace("%player%", target.getName()));
        }
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission(TPermissions.ADMIN)) {
            if (!SupremeTags.getInstance().isNoPermissionMenuAction()) {
                msgPlayer(sender, noperm);
            } else {
                handleMainCommand(sender, Bukkit.getPlayer(sender.getName()));
            }
            return;
        }

        CreateCommandArguments createArgs = parseCreateCommandArguments(args);
        if (createArgs == null) {
            msgPlayer(sender, msg("messages.usage.create"));
            playConfigSound((Player) sender, "error-message");
            return;
        }

        String name = createArgs.name;

        if (SupremeTags.getInstance().getTagManager().tagExists(name)) {
            msgPlayer(sender, validtag);
            playConfigSound((Player) sender, "error-message");
            return;
        }

        List<String> desc = new ArrayList<>();
        desc.add("&7My tag is " + name);

        String fileLocation = createArgs.fileLocation;

        SupremeTags.getInstance().getTagManager().createTag(sender, name, createArgs.tag, desc, "supremetags.tag." + name, 100, fileLocation);
    }

    private CreateCommandArguments parseCreateCommandArguments(String[] args) {
        if (args.length < 3) {
            return null;
        }

        String defaultFileLocation = SupremeTags.getInstance().getConfig().getString("settings.default-tag-file", "countries.yml");
        String name = args[1];
        String firstTagArgument = args[2];

        if (!isQuotedArgumentStart(firstTagArgument)) {
            if (args.length > 4) {
                return null;
            }

            String fileLocation = args.length == 4 ? args[3] : defaultFileLocation;
            return new CreateCommandArguments(name, firstTagArgument, fileLocation);
        }

        char quote = firstTagArgument.charAt(0);
        StringBuilder tag = new StringBuilder(firstTagArgument.substring(1));
        int endIndex = -1;

        for (int index = 2; index < args.length; index++) {
            String current = args[index];
            if (index > 2) {
                tag.append(' ').append(current);
            }

            if (hasClosingQuote(current, quote)) {
                tag.setLength(tag.length() - 1);
                endIndex = index;
                break;
            }
        }

        if (endIndex == -1 || tag.length() == 0 || args.length > endIndex + 2) {
            return null;
        }

        String fileLocation = args.length == endIndex + 2 ? args[endIndex + 1] : defaultFileLocation;
        return new CreateCommandArguments(name, tag.toString(), fileLocation);
    }

    private boolean isQuotedArgumentStart(String argument) {
        return argument.length() > 1 && (argument.charAt(0) == '"' || argument.charAt(0) == '\'');
    }

    private boolean hasClosingQuote(String argument, char quote) {
        return argument.length() > 1 && argument.charAt(argument.length() - 1) == quote;
    }

    private static class CreateCommandArguments {
        private final String name;
        private final String tag;
        private final String fileLocation;

        private CreateCommandArguments(String name, String tag, String fileLocation) {
            this.name = name;
            this.tag = tag;
            this.fileLocation = fileLocation;
        }
    }

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
            msgPlayer(sender, msg("messages.usage.remove-tag-permission"));
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
            msgPlayer(sender, msg("messages.usage.give-voucher"));
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
    private void handleSetCustomTag(CommandSender sender, String[] args) {
        if (!sender.hasPermission("supremetags.setcustomtag")) {
            msgPlayer(sender, noperm);
            return;
        }

        if (args.length < 3) {
            msgPlayer(sender, msg("messages.usage.set-custom-tag"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

        if (!target.hasPlayedBefore() || target.getName() == null) {
            msgPlayer(sender, player_not_online);
            return;
        }

        String tag = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        UserData.setCustomTag(target, tag);

        msgPlayer(sender, msg("messages.custom-tag-set")
                .replace("%player%", String.valueOf(target.getName()))
                .replace("%tag%", tag));

        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null) {
            msgPlayer(onlineTarget, msg("messages.custom-tag-set-target")
                    .replace("%tag%", tag));
        }
    }

    private void handleResetCustomTag(CommandSender sender, String[] args) {
        if (!sender.hasPermission("supremetags.resetcustomtag")) {
            msgPlayer(sender, noperm);
            return;
        }

        if (args.length < 2) {
            msgPlayer(sender, msg("messages.usage.reset-custom-tag"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

        if (!target.hasPlayedBefore() || target.getName() == null) {
            msgPlayer(sender, player_not_online);
            return;
        }

        UserData.setCustomTag(target, "");

        msgPlayer(sender, msg("messages.custom-tag-reset")
                .replace("%player%", String.valueOf(target.getName())));

        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null) {
            msgPlayer(onlineTarget, msg("messages.custom-tag-reset-target"));
        }
    }

    private void handleSet(CommandSender sender, String[] args) {

        if (args.length < 2) {
            msgPlayer(sender, msg("messages.usage.set"));
            return;
        }

        String identifier = args[1];
        boolean tagExists = SupremeTags.getInstance().getTagManager().getTags().containsKey(identifier);

        if (!tagExists) {
            msgPlayer(sender, invalidtag);
            return;
        }

        if (args.length == 2 || (args.length == 3 && args[2].equalsIgnoreCase("-s"))) {
            if (!(sender instanceof Player player)) {
                msgPlayer(sender, msg("messages.players-only-set-own"));
                return;
            }

            if (!player.hasPermission("supremetags.set")) {
                msgPlayer(player, noperm);
                return;
            }

            Tag tag = SupremeTags.getInstance().getTagManager().getTag(identifier);
            if (!Utils.hasTagAccess(player, tag)) {
                sendLockedMessage(player, tag);
                return;
            }

            boolean silent = args.length == 3 && args[2].equalsIgnoreCase("-s");

            UserData.setActive(player, identifier);
            if (!silent) {
                String select = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.tag-select-message");
                if (select == null) {
                    select = "&aYou have selected the tag %tag%";
                }

                select = select.replace("%prefix%", SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.prefix"));
                select = replacePlaceholders(player, select);
                msgPlayer(player, select
                        .replace("%identifier%", identifier)
                        .replace("%tag%", SupremeTags.getInstance().getTagManager().getTag(identifier).getTag().getFirst()));
                playConfigSound(player, "selected-tag");
            }
            return;
        }

        if (args.length >= 3) {

            if (!sender.hasPermission("supremetags.set.other")) {
                msgPlayer(sender, noperm);
                return;
            }

            boolean silent = args.length >= 4 && args[3].equalsIgnoreCase("-s");
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);

            if (!target.hasPlayedBefore() || target.getName() == null) {
                msgPlayer(sender, player_not_online);
                return;
            }

            UserData.setActive(target, identifier);

            if (!silent) {
                msgPlayer(sender, msg("messages.tag-set-other")
                        .replace("%player%", String.valueOf(target.getName()))
                        .replace("%identifier%", identifier));

                Player onlineTarget = target.getPlayer();
                if (onlineTarget != null) {
                    msgPlayer(onlineTarget, msg("messages.tag-set-by-admin")
                            .replace("%identifier%", identifier));
                }
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        List<String> aliases = SupremeTags.getInstance().getConfig().getStringList("settings.commands.aliases");
        String mainCommand = SupremeTags.getInstance().getConfig().getString("settings.commands.main-command", "tags");

        List<String> allCommands = new ArrayList<>(aliases);
        allCommands.add(mainCommand);

        String label = command.getName().toLowerCase();

        List<String> completions = new ArrayList<>();

        String[] subCommands = new String[0];

        String[] edits;

        if (sender.hasPermission(TPermissions.ADMIN)) {
            subCommands = new String[]{
                      "create", "delete", "set", "setcustomtag", "resetcustomtag", "givevoucher", "reset",
                      "removetagp", "merge", "reload", "help", "config", "editor",
                      "list", "stats", "withdraw", "debug", "dump", "search", "edit", "move", "seteveryone", "reseteveryone", "favourites", "view", "credits"
            };
        } else if (!sender.hasPermission(TPermissions.ADMIN) && sender.hasPermission(TPermissions.WITHDRAW) && sender.hasPermission(TPermissions.SEARCH)) {
            subCommands = new String[]{
                    "withdraw", "search", "favourites", "view", "credits"
            };
        } else if (sender.hasPermission(TPermissions.VIEW)) {
            subCommands = new String[]{
                    "view", "credits"
            };
        } else {
            subCommands = new String[]{
                    "credits"
            };
        }

        if (allCommands.contains(label)) {

            if (args.length == 1) {

                for (String subCommand : subCommands) {
                    if (subCommand.startsWith(args[0].toLowerCase())) {
                        completions.add(subCommand);
                    }
                }
            }

            else if (args.length == 2) {
                String firstArg = args[0].toLowerCase();

                switch (firstArg) {
                    case "create":
                        completions.add("Name");
                        break;
                    case "editor":
                        completions.addAll(Arrays.asList("web", "apply"));
                        break;
                    case "delete":
                    case "move":
                    case "seteveryone":
                    case "stats":
                    case "withdraw":
                    case "edit":
                        completions.addAll(SupremeTags.getInstance().getTagManager().getTags().keySet());
                        break;
                    case "set":
                        SupremeTags.getInstance().getTagManager().getTags().values().stream()
                                .filter(tag -> sender.hasPermission(tag.getPermission()))
                                .forEach(tag -> completions.add(tag.getIdentifier()));
                        break;
                    case "reset":
                        completions.add("-s");
                        completions.addAll(Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .toList());
                        break;
                    case "removetagp":
                    case "resetcustomtag":
                    case "setcustomtag":
                    case "givevoucher":
                    case "view":
                        completions.addAll(Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .toList());
                        break;
                    case "credits":
                        completions.addAll(Arrays.asList("give", "take", "set"));
                        completions.addAll(Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .toList());
                        break;

                    default:
                        break;
                }
            }

            else if (args.length == 3) {
                String firstArg = args[0].toLowerCase();

                switch (firstArg) {
                    case "create":
                        completions.add("Tag");
                        break;
                    case "editor":
                        if (args[1].equalsIgnoreCase("apply")) {
                            completions.add("sessionId");
                        }
                        break;
                    case "setcustomtag":
                        completions.add("Tag Style Here");
                        break;
                    case "givevoucher":
                    case "set":
                        if (sender.hasPermission("supremetags.set.other")) {
                            completions.addAll(Bukkit.getOnlinePlayers().stream()
                                    .map(Player::getName)
                                    .toList());
                        }
                        break;
                    case "reset":
                        completions.add("-s");
                        break;
                    case "removetagp":

                        completions.addAll(SupremeTags.getInstance().getTagManager().getTags().keySet());
                        break;
                    case "edit":
                        edits = new String[]{
                                  "tag", "permission", "cost", "withdrawable", "category", "rarity"
                        };

                        completions.addAll(Arrays.stream(edits).toList());
                        break;
                    case "move":
                        String partialMoveFile = args[2].toLowerCase(Locale.ROOT);
                        for (String file : SupremeTags.getInstance().getConfigManager().getTagFilePaths()) {
                            if (file.toLowerCase(Locale.ROOT).startsWith(partialMoveFile)) {
                                completions.add(file);
                            }
                        }
                        completions.add("new-folder/new-file.yml");
                        break;
                    case "credits":
                        completions.addAll(Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .toList());
                        break;
                    default:
                        break;
                }
            }

            else if (args.length == 4) {
                if (args[0].equalsIgnoreCase("create")) {
                    List<String> tagFiles = SupremeTags.getInstance().getConfigManager().getTagFilePaths();
                    String partial = args[3].toLowerCase();
                    for (String file : tagFiles) {
                        if (file.toLowerCase().startsWith(partial)) {
                            completions.add(file);
                        }
                    }
                } else if (args[0].equalsIgnoreCase("givevoucher") || args[0].equalsIgnoreCase("set")) {
                    completions.add("-s");
                } else if (args[0].equalsIgnoreCase("edit") && args[2].equalsIgnoreCase("tag")) {
                    String tagName = args[1];
                    if (SupremeTags.getInstance().getTagManager().tagExists(tagName)) {
                        completions.addAll(SupremeTags.getInstance().getTagManager().getTag(tagName).getTag());
                    }
                } else if (args[0].equalsIgnoreCase("edit") && args[2].equalsIgnoreCase("category")) {
                    String tagName = args[1];
                    if (SupremeTags.getInstance().getTagManager().tagExists(tagName)) {
                        completions.addAll(SupremeTags.getInstance().getCategoryManager().getCatorgies());
                    }
                } else if (args[0].equalsIgnoreCase("edit") && args[2].equalsIgnoreCase("permission")) {
                    String tagName = args[1];
                    if (SupremeTags.getInstance().getTagManager().tagExists(tagName)) {
                        completions.add(SupremeTags.getInstance().getTagManager().getTag(tagName).getPermission());
                    }
                } else if (args[0].equalsIgnoreCase("edit") && args[2].equalsIgnoreCase("rarity")) {
                    String tagName = args[1];
                    if (SupremeTags.getInstance().getTagManager().tagExists(tagName)) {
                        completions.addAll(SupremeTags.getInstance().getRarityManager().getRarityMap().keySet());
                    }
                } else if (args[0].equalsIgnoreCase("edit") && args[2].equalsIgnoreCase("cost")) {
                    String tagName = args[1];
                    if (SupremeTags.getInstance().getTagManager().tagExists(tagName)) {
                        completions.add(String.valueOf(SupremeTags.getInstance().getTagManager().getTag(tagName).getEconomy().getAmount()));
                    }
                } else if (args[0].equalsIgnoreCase("edit") && args[2].equalsIgnoreCase("withdrawable")) {
                    String tagName = args[1];
                    if (SupremeTags.getInstance().getTagManager().tagExists(tagName)) {
                        completions.addAll(Arrays.asList("true", "false"));
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
            msgPlayer(sender, msg("messages.debug-missing"));
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
        map.put("%hook_excellenteconomy%", hookString(SupremeTags.getInstance().isExcellentEconomy()));
        map.put("%item_data%", "&aPDC");
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

    private String msg(String path) {
        return msg(path, "");
    }

    private String msg(String path, String fallback) {
        String value = messages.getString(path, fallback);
        if (value == null || value.isBlank()) {
            value = fallback;
        }

        return value
                .replace("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix", "")))
                .replace("%command%", SupremeTags.getInstance().getConfig().getString("settings.commands.main-command", "tags"));
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

        checkKey(settings, "commands.main-command", errors, String.class);
        checkKey(settings, "commands.aliases", errors, List.class);
        checkKey(settings, "no-permission-menu-action", errors, Boolean.class);
        checkKey(settings, "proxy-file-syncing", errors, Boolean.class);
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

        checkKey(settings, "personal-tags.enable", errors, Boolean.class);
        checkKey(settings, "personal-tags.limits", errors, ConfigurationSection.class);
        checkKey(settings, "personal-tags.format-replace", errors, String.class);
        checkKey(settings, "personal-tags.create-requirements", errors, ConfigurationSection.class);
        checkKey(settings, "personal-tags.create-requirements.min-server-age", errors, String.class);
        checkKey(settings, "personal-tags.create-requirements.min-playtime", errors, String.class);

        checkKey(settings, "layout-type", errors, String.class);
        checkKey(settings, "animated-tag-speed", errors, Integer.class);
        checkKey(settings, "tag-vouchers", errors, Boolean.class);
        checkKey(settings, "prioritise-selected-tag", errors, Boolean.class);
        checkKey(settings, "voucher-redeem-permission", errors, Boolean.class);
        checkKey(settings, "voucher-redeem-confirmation", errors, Boolean.class);
        checkKey(settings, "tag-purchase-confirmation", errors, Boolean.class);
        checkKey(settings, "tag-select-confirmation", errors, Boolean.class);
        checkKey(settings, "deactivate-click", errors, Boolean.class);
        checkKey(settings, "only-show-player-access-tags", errors, Boolean.class);
        checkKey(settings, "search-type", errors, String.class);
        checkKey(settings, "update-unlocked-cache", errors, Integer.class);

        String layout = settings.getString("layout-type", "").toUpperCase();
        if (!layout.equals("FULL") && !layout.equals("BORDER")) {
            errors.add("Invalid layout-type: " + layout + " (must be FULL or BORDER)");
        }

        String searchType = settings.getString("search-type", "").toUpperCase();
        if (!searchType.equals("SIGN") && !searchType.equals("ANVIL")) {
            errors.add("Invalid search-type: " + searchType + " (must be SIGN or ANVIL)");
        }

        List<String> phKeys = Arrays.asList("tag", "chat", "scoreboard", "tab");

        for (String key : phKeys) {
            checkKey(config, "placeholders." + key + ".none-output", errors, String.class);
            checkKey(config, "placeholders." + key + ".format", errors, String.class);
        }

        List<String> soundKeys = Arrays.asList("open-menus", "selected-tag", "reset-tag", "error-message");

        for (String key : soundKeys) {
            checkKey(config, "sounds." + key + ".enable", errors, Boolean.class);
            checkKey(config, "sounds." + key + ".sound", errors, String.class);
            checkKey(config, "sounds." + key + ".volume", errors, Double.class);
            checkKey(config, "sounds." + key + ".pitch", errors, Double.class);
        }

        return errors;
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
                                Utils.runMain(() -> new SearchResultMenu(SupremeTags.getMenuUtil(player), line1).open());
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

    protected void sendLockedMessage(Player player, Tag tag) {
            String requirementMessage = Utils.getTagRequirementMessage(player, tag);
            if (requirementMessage != null && !requirementMessage.isBlank()) {
                msgPlayer(player, replacePlaceholders(player, requirementMessage));
                return;
            }

            sendLockedMessage(player);
    }
}
