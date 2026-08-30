package net.noscape.project.supremetags.utils;

import com.cryptomorin.xseries.XItemFlag;
import com.cryptomorin.xseries.XSound;
import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.FontImages.FontImageWrapper;
import io.th0rgal.oraxen.api.OraxenItems;
import me.clip.placeholderapi.PAPIComponents;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.util.Tristate;
import net.md_5.bungee.api.ChatColor;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.api.events.TagBuyEvent;
import net.noscape.project.supremetags.guis.confirm.ConfirmationMenu;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.handlers.TagEconomy;
import net.noscape.project.supremetags.handlers.Variant;
import net.noscape.project.supremetags.handlers.menu.MenuUtil;
import net.noscape.project.supremetags.handlers.requirements.RequirementEvaluator;
import net.noscape.project.supremetags.handlers.requirements.RequirementResult;
import net.noscape.project.supremetags.managers.TagManager;
import net.noscape.project.supremetags.storage.UserData;
import net.noscape.project.supremetags.storage.user.PlayerConfig;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Utils {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("###.#");
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.builder()
            .character('\u00A7')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    public static String format(String message) {
        if (message == null || message.isEmpty()) return "";
        return LEGACY_SECTION.serialize(formatComponent(message));
    }

    public static String toMiniMessage(String message) {
        if (message == null || message.isEmpty()) return "";

        String normalized = message.replace('\u00A7', '&');
        StringBuilder output = new StringBuilder(normalized.length());

        for (int i = 0; i < normalized.length(); i++) {
            char current = normalized.charAt(i);

            if (current != '&' || i + 1 >= normalized.length()) {
                output.append(current);
                continue;
            }

            if (Character.toLowerCase(normalized.charAt(i + 1)) == 'x' && i + 13 < normalized.length()) {
                StringBuilder hex = new StringBuilder(6);
                boolean validHex = true;

                for (int j = 0; j < 6; j++) {
                    int ampIndex = i + 2 + (j * 2);
                    int hexIndex = ampIndex + 1;

                    if (normalized.charAt(ampIndex) != '&' || !isHexDigit(normalized.charAt(hexIndex))) {
                        validHex = false;
                        break;
                    }

                    hex.append(normalized.charAt(hexIndex));
                }

                if (validHex) {
                    output.append("<#").append(hex).append(">");
                    i += 13;
                    continue;
                }
            }

            if (normalized.charAt(i + 1) == '#' && i + 7 < normalized.length()) {
                String hex = normalized.substring(i + 2, i + 8);
                if (hex.chars().allMatch(value -> isHexDigit((char) value))) {
                    output.append("<#").append(hex).append(">");
                    i += 7;
                    continue;
                }
            }

            String replacement = legacyCodeToMiniMessage(normalized.charAt(i + 1));
            if (replacement == null) {
                output.append(current);
                continue;
            }

            output.append(replacement);
            i++;
        }

        return output.toString();
    }

    private static boolean isHexDigit(char character) {
        return (character >= '0' && character <= '9')
                || (character >= 'a' && character <= 'f')
                || (character >= 'A' && character <= 'F');
    }

    private static String legacyCodeToMiniMessage(char code) {
        return switch (Character.toLowerCase(code)) {
            case '0' -> "<reset><black>";
            case '1' -> "<reset><dark_blue>";
            case '2' -> "<reset><dark_green>";
            case '3' -> "<reset><dark_aqua>";
            case '4' -> "<reset><dark_red>";
            case '5' -> "<reset><dark_purple>";
            case '6' -> "<reset><gold>";
            case '7' -> "<reset><gray>";
            case '8' -> "<reset><dark_gray>";
            case '9' -> "<reset><blue>";
            case 'a' -> "<reset><green>";
            case 'b' -> "<reset><aqua>";
            case 'c' -> "<reset><red>";
            case 'd' -> "<reset><light_purple>";
            case 'e' -> "<reset><yellow>";
            case 'f' -> "<reset><white>";
            case 'k' -> "<obfuscated>";
            case 'l' -> "<bold>";
            case 'm' -> "<strikethrough>";
            case 'n' -> "<underlined>";
            case 'o' -> "<italic>";
            case 'r' -> "<reset>";
            default -> null;
        };
    }

    public static Component formatComponent(String message) {
        if (message == null || message.isEmpty()) return Component.empty();

        try {
            return MINI_MESSAGE.deserialize(toMiniMessage(message));
        } catch (Exception e) {
            return Component.text(message);
        }
    }

    public static Component formatComponent(Player player, String message) {
        Component component = formatComponent(message);
        if (player == null || Bukkit.getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return component;
        }

        if (!PlaceholderAPI.containsPlaceholders(message)) {
            return component;
        }

        try {
            return PAPIComponents.setPlaceholders(player, component, Utils::placeholderComponent);
        } catch (LinkageError | RuntimeException ignored) {
            return formatComponent(PlaceholderAPI.setPlaceholders(player, message));
        }
    }

    private static ComponentLike placeholderComponent(String value) {
        return formatComponent(value);
    }

    public static void msgPlayer(CommandSender sender, String... messages) {
        if (messages == null || messages.length == 0) return;

        for (String msg : messages) {
            if (msg == null || msg.isEmpty()) continue;

            Component component = sender instanceof Player player
                    ? formatComponent(player, msg)
                    : formatComponent(msg);
            sender.sendMessage(component);
        }
    }

    public static void msgPlayer(Player player, String... messages) {
        msgPlayer((CommandSender) player, messages);
    }

    public static List<String> color(List<String> lore) {
        if (lore == null) return Collections.emptyList();
        return lore.stream()
                .map(line -> format(line))
                .collect(Collectors.toList());
    }

    public static Component itemName(String text) {
        return formatComponent(text).decoration(TextDecoration.ITALIC, false);
    }

    public static List<Component> itemLore(List<String> lore) {
        if (lore == null) return Collections.emptyList();
        return lore.stream()
                .map(Utils::formatComponent)
                .map(component -> component.decoration(TextDecoration.ITALIC, false))
                .collect(Collectors.toList());
    }

    public static boolean isVersionLessThan(String version) {
        try {
            String current = Bukkit.getBukkitVersion().split("-")[0];

            String[] currentParts = current.split("\\.");
            String[] targetParts = version.split("\\.");

            int length = Math.max(currentParts.length, targetParts.length);

            for (int i = 0; i < length; i++) {
                int currentNum = i < currentParts.length
                        ? Integer.parseInt(currentParts[i])
                        : 0;

                int targetNum = i < targetParts.length
                        ? Integer.parseInt(targetParts[i])
                        : 0;

                if (currentNum < targetNum) return true;
                if (currentNum > targetNum) return false;
            }

            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void addPerm(OfflinePlayer player, String permission) {
        SupremeTags.getPermissions().playerAdd(null, player, permission);
    }

    public static void removePerm(OfflinePlayer player, String permission) {
        SupremeTags.getPermissions().playerRemove(null, player, permission);
    }

    public static boolean hasGroupAccess(Player player, List<String> groups) {
        if (groups == null || groups.isEmpty()) return false;

        try {
            Class<?> vaultUnlockedClass = Class.forName("net.milkbowl.vault.VaultUnlockedAPI");
            Object groupManager = vaultUnlockedClass.getMethod("getGroupManager").invoke(null);
            Object playerGroups = groupManager.getClass().getMethod("getPlayerGroups", org.bukkit.OfflinePlayer.class).invoke(groupManager, player);

            if (playerGroups instanceof List<?> playerGroupList) {
                for (Object g : playerGroupList) {
                    if (g != null && groups.contains(g.toString())) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {

        }

        return false;
    }

    public static boolean hasTagAccess(Player player, Tag tag) {
        if (player.hasPermission(tag.getPermission()) || tag.getPermission().equalsIgnoreCase("none")) {
            return true;
        }
        return hasGroupAccess(player, tag.getGroups());
    }

    public static boolean hasAmount(Player player, String economyType, double cost, String tag) {
        if (economyType.equalsIgnoreCase("VAULT")) {
            return SupremeTags.getEconomy().has(player, cost);
        } else if (economyType.equalsIgnoreCase("PLAYERPOINTS")) {
            return SupremeTags.getInstance().getPpAPI().look(player.getUniqueId()) >= cost;
        } else if (economyType.equalsIgnoreCase("EXP_LEVEL")) {
            return player.getLevel() >= cost;
        } else if (economyType.startsWith("EXCELLENTECONOMY-")) {
            String eco_name = economyType.replace("EXCELLENTECONOMY-", "");
            return SupremeTags.getExcellentEconomy().getBalance(player, eco_name) >= cost;
        } else if (economyType.equalsIgnoreCase("CUSTOM")) {
            TagEconomy eco = SupremeTags.getInstance().getTagManager().getTag(tag).getEconomy();
            String condition = eco.getCondition();

            if (condition == null || condition.trim().isEmpty()) return false;

            condition = condition.replace("%amount%", String.valueOf(cost));
            condition = PlaceholderAPI.setPlaceholders(player, condition);

            return evaluateCondition(condition);
        }

        return false;
    }

    public static void take(Player player, String economyType, double cost, String tag) {
        if (economyType.equalsIgnoreCase("VAULT")) {
            SupremeTags.getEconomy().withdrawPlayer(player, cost);
        } else if (economyType.equalsIgnoreCase("PLAYERPOINTS")) {
            SupremeTags.getInstance().getPpAPI().take(player.getUniqueId(), (int) cost);
        } else if (economyType.equalsIgnoreCase("EXP_LEVEL")) {
            player.setLevel((int) (player.getLevel() - cost));
        } else if (economyType.startsWith("EXCELLENTECONOMY-")) {
            String eco_name = economyType.replace("EXCELLENTECONOMY-", "");
            SupremeTags.getExcellentEconomy().deposit(player, eco_name, cost);
        } else if (economyType.equalsIgnoreCase("CUSTOM")) {
            TagEconomy eco = SupremeTags.getInstance().getTagManager().getTag(tag).getEconomy();
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), eco.getTake_cmd().replace("%player%", player.getName()).replace("%amount%", String.valueOf(cost)));
        }
    }

    private static boolean evaluateCondition(String condition) {
        condition = condition.replace(" ", "");

        String[] operators = {">=", "<=", "==", "!=", ">", "<"};

        for (String op : operators) {
            if (condition.contains(op)) {
                String[] parts = condition.split(java.util.regex.Pattern.quote(op));
                if (parts.length != 2) return false;

                double left, right;
                try {
                    left = Double.parseDouble(parts[0]);
                    right = Double.parseDouble(parts[1]);
                } catch (NumberFormatException e) {
                    return false;
                }

                return switch (op) {
                    case ">=" -> left >= right;
                    case "<=" -> left <= right;
                    case ">" -> left > right;
                    case "<" -> left < right;
                    case "==" -> left == right;
                    case "!=" -> left != right;
                    default -> false;
                };
            }
        }

        return false;
    }

    public static String deformat(String str) {
        return PLAIN_TEXT.serialize(formatComponent(str));
    }

    public static void titlePlayer(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.sendTitle(format(title), format(subtitle), fadeIn, stay, fadeOut);
    }

    public static void soundPlayer(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    public static String replacePlaceholders(Player user, String base) {
        if (Bukkit.getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) return base;

        return PlaceholderAPI.setPlaceholders(user, base);
    }

    public static String replaceInternalPlaceholders(Player user, String message) {
        if (message == null || message.isEmpty()) return "";

        if (message.contains("%tag_credits%")) {
            long credits = user != null ? UserData.getDisplayTagCredits(user.getUniqueId()) : 0;
            message = message.replace("%tag_credits%", String.valueOf(credits));
        }

        if (message.contains("%tag_credits_creation_cost%")) {
            int creationCost = SupremeTags.getInstance().getConfig()
                    .getInt("settings.personal-tags.credits.creation-cost", 0);
            message = message.replace("%tag_credits_creation_cost%", String.valueOf(creationCost));
        }

        return message;
    }

    public static List<String> replaceInternalPlaceholders(Player user, List<String> lore) {
        if (lore == null) return Collections.emptyList();
        return lore.stream()
                .map(line -> replaceInternalPlaceholders(user, line))
                .collect(Collectors.toList());
    }

    public static String globalPlaceholders(Player user, String message) {
        message = replaceInternalPlaceholders(user, message);
        message = replacePlaceholders(user, message);
        if (Bukkit.getServer().getPluginManager().getPlugin("ItemsAdder") != null) {
            message = FontImageWrapper.replaceFontImages(message);
        }

        return message;
    }

    public static int isCustomGUIItemSlot(Player player, ItemStack itemStack) {
        FileConfiguration guis = SupremeTags.getInstance().getConfigManager().getConfig("guis.yml").get();

        if (itemStack == null || !itemStack.hasItemMeta()) {
            return -1;
        }

        ItemMeta meta = itemStack.getItemMeta();

        for (String key : guis.getConfigurationSection("gui.tag-menu.custom-items").getKeys(false)) {
            if (!meta.hasDisplayName()) continue;

            String displayName = deformat(meta.getDisplayName());
            String configDisplayName = deformat(guis.getString("gui.tag-menu.custom-items." + key + ".displayname", ""));
            String material = guis.getString("gui.tag-menu.custom-items." + key + ".material", "");

            int customModelData = 0;
            int configCustomModelData = guis.getInt("gui.tag-menu.custom-items." + key + ".custom-model-data", 0);
            if (meta.hasCustomModelData()) {
                customModelData = meta.getCustomModelData();
            }
            configDisplayName = replacePlaceholders(player, configDisplayName);

            if (material != null
                    && displayName.equals(configDisplayName)
                    && customModelData == configCustomModelData) {
                return guis.getInt("gui.tag-menu.custom-items." + key + ".slot");
            }
        }

        return -1;
    }

    public static String isCustomGUIItemName(Player player, ItemStack itemStack) {
        FileConfiguration guis = SupremeTags.getInstance().getConfigManager().getConfig("guis.yml").get();

        for (String key : guis.getConfigurationSection("gui.tag-menu.custom-items").getKeys(false)) {
            if (itemStack != null && itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()) {
                String displayName = deformat(itemStack.getItemMeta().getDisplayName());
                int customModelData;
                if (itemStack.getItemMeta().hasCustomModelData()) {
                    customModelData = itemStack.getItemMeta().getCustomModelData();
                } else {
                    customModelData = 0;
                }

                String material = guis.getString("gui.tag-menu.custom-items." + key + ".material");
                String displaynameConfig = deformat(guis.getString("gui.tag-menu.custom-items." + key + ".displayname"));
                int configCustomModelData = guis.getInt("gui.tag-menu.custom-items." + key + ".custom-model-data");

                displaynameConfig = replacePlaceholders(player, displaynameConfig);

                if (material != null && displayName.equals(displaynameConfig) && customModelData == configCustomModelData) {
                    return key;
                }
            }
        }

        return "";
    }

    public static int compareVersions(String version1, String version2) {
        String[] splitVersion1 = version1.split("\\.");
        String[] splitVersion2 = version2.split("\\.");

        int length = Math.max(splitVersion1.length, splitVersion2.length);

        for (int i = 0; i < length; i++) {
            int v1 = i < splitVersion1.length ? Integer.parseInt(splitVersion1[i]) : 0;
            int v2 = i < splitVersion2.length ? Integer.parseInt(splitVersion2[i]) : 0;
            if (v1 < v2) {
                return -1;
            }
            if (v1 > v2) {
                return 1;
            }
        }
        return 0;
    }

    public static void addItemFlags(ItemMeta meta, XItemFlag... flags) {
        if (meta == null || flags == null)
            return;
        for (XItemFlag flag : flags) {
            if (flag != null)
                try {
                    Object resolvedFlag = flag.getClass().getMethod("get", new Class[0]).invoke(flag, new Object[0]);
                    if (resolvedFlag instanceof Optional) {
                        Optional<?> optional = (Optional)resolvedFlag;
                        resolvedFlag = optional.orElse(null);
                    }
                    if (resolvedFlag instanceof ItemFlag) {
                        ItemFlag itemFlag = (ItemFlag)resolvedFlag;
                        meta.addItemFlags(new ItemFlag[] { itemFlag });
                    }
                } catch (ReflectiveOperationException|LinkageError reflectiveOperationException) {}
        }
    }

    public static boolean hasBaseTagAccess(Player player, Tag tag) {
        if (player.hasPermission(tag.getPermission()) || tag.getPermission().equalsIgnoreCase("none"))
            return true;
        return hasGroupAccess(player, tag.getGroups());
    }

    public static boolean hasTagRequirements(Player player, Tag tag) {
        return RequirementEvaluator.evaluate(player, tag).isPassed();
    }

    public static String getTagRequirementMessage(Player player, Tag tag) {
        RequirementResult result = RequirementEvaluator.evaluate(player, tag);
        return result.isPassed() ? "" : result.getMessage();
    }

    public static String getTagRequirementsStatus(Player player, Tag tag) {
        return RequirementEvaluator.formatStatus(player, tag);
    }

    public static boolean shouldConfirmTagSelection() {
        return SupremeTags.getInstance().getConfig().getBoolean("settings.tag-select-confirmation", false);
    }

    public static boolean shouldConfirmTagPurchase() {
        return SupremeTags.getInstance().getConfig().getBoolean("settings.tag-purchase-confirmation", false);
    }

    public static void openTagSelectionConfirmation(Player player, String identifier) {
        (new ConfirmationMenu(new MenuUtil(player, identifier), "select-tag:" + identifier)).open();
    }

    public static void openTagPurchaseConfirmation(Player player, String identifier) {
        (new ConfirmationMenu(new MenuUtil(player, identifier), "purchase-tag:" + identifier)).open();
    }

    public static void purchaseTag(Player player, Tag tag) {
        if (tag == null)
            return;
        TagBuyEvent tagEvent = new TagBuyEvent(player, tag.getIdentifier(), tag.getEcoAmount(), false);
        Bukkit.getPluginManager().callEvent((Event)tagEvent);
        if (tagEvent.isCancelled())
            return;
        take(player, tag.getEcoType(), tag.getEcoAmount(), tag.getIdentifier());
        addPerm((OfflinePlayer)player, tag.getPermission());
        if (SupremeTags.getInstance().getConfig().getBoolean("settings.gui-messages")) {
            YamlConfiguration yamlConfiguration = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get();
            String unlocked = yamlConfiguration.getString("messages.tag-unlocked", "%prefix% &7You have unlocked the tag: &6%identifier%").replace("%prefix%", Objects.<CharSequence>requireNonNull(yamlConfiguration.getString("messages.prefix", "")));
            unlocked = replacePlaceholders(player, unlocked);
            msgPlayer(player, new String[] { unlocked.replace("%identifier%", tag.getIdentifier()).replace("%tag%", tag.getCurrentTag()) });
        }
    }

    private static String formatLarge(double n, int iteration) {
        double f = n / 1000.0D;
        return f < 1000 || iteration >= getNumberFormat().length - 1 ?
                DECIMAL_FORMAT.format(f) + getNumberFormat()[iteration] : formatLarge(f, iteration + 1);
    }

    public static String formatNumber(double value) {
        return value < 1000 ? DECIMAL_FORMAT.format(value) : formatLarge(value, 0);
    }

    private static String[] getNumberFormat() {
        return "k;M;B;T;Q;QQ;S;SS;OC;N;D;UN;DD;TR;QT;QN;SD;SPD;OD;ND;VG;UVG;DVG;TVG;QTV;QNV;SEV;SPV;OVG;NVG;TG".split(";");
    }

    public static ItemStack getItemWithIA(String id) {
        if (CustomStack.isInRegistry(id)) {
            CustomStack stack = CustomStack.getInstance(id);
            if (stack != null) {
                return stack.getItemStack();
            }
        }

        return null;
    }

    public static ItemStack getItemWithOraxen(String id) {
        return OraxenItems.getItemById(id).build();
    }
    public static void calculateUnlockedTagCounts() {
        runAsync(() -> {
            LuckPerms luckPerms = LuckPermsProvider.get();
            UserManager userManager = luckPerms.getUserManager();
            Map<String, Integer> result = new ConcurrentHashMap<>();

            List<Tag> tagsSnapshot = new ArrayList<>(SupremeTags.getInstance().getTagManager().getTags().values());
            List<Variant> variantsSnapshot = new ArrayList<>(SupremeTags.getInstance().getTagManager().getVariants());

            OfflinePlayer[] offlinePlayers = Bukkit.getOfflinePlayers();
            Map<UUID, User> userCache = new ConcurrentHashMap<>();

            final int batchSize = 200;

            List<List<OfflinePlayer>> batches = new ArrayList<>();
            for (int i = 0; i < offlinePlayers.length; i += batchSize) {
                batches.add(Arrays.asList(Arrays.copyOfRange(
                        offlinePlayers,
                        i,
                        Math.min(i + batchSize, offlinePlayers.length)
                )));
            }

            CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);

            for (List<OfflinePlayer> batch : batches) {
                chain = chain.thenCompose(v ->
                        CompletableFuture.allOf(
                                batch.stream()
                                        .map(player ->
                                                userManager.loadUser(player.getUniqueId())
                                                        .thenAccept(user -> {
                                                            if (user != null) {
                                                                userCache.put(player.getUniqueId(), user);
                                                            }
                                                        })
                                                        .exceptionally(ex -> null)
                                        )
                                        .toArray(CompletableFuture[]::new)
                        )
                );
            }

            chain.thenRun(() -> {

                for (Tag tag : tagsSnapshot) {
                    long unlocked = userCache.values().stream()
                            .filter(user -> user.getCachedData().getPermissionData().checkPermission(tag.getPermission()) == Tristate.TRUE)
                            .count();
                    result.put(tag.getIdentifier(), (int) unlocked);
                }

                for (Variant var : variantsSnapshot) {
                    long unlocked = userCache.values().stream()
                            .filter(user -> user.getCachedData().getPermissionData().checkPermission(var.getPermission()) == Tristate.TRUE)
                            .count();
                    result.put(var.getIdentifier(), (int) unlocked);
                }

                runMain(() -> {
                    TagManager.tagUnlockCounts.clear();
                    TagManager.tagUnlockCounts.putAll(result);
                });
            });
        });
    }

    public static void scheduleUnlockCount() {
        long intervalTicks = 20L * SupremeTags.getInstance().getConfig().getInt("settings.update-unlocked-cache");
        long initialDelay = Math.max(1L, intervalTicks);

        if (SupremeTags.getInstance().isFoliaFound()) {
            Object future = Bukkit.getServer().getGlobalRegionScheduler().runAtFixedRate(
                    SupremeTags.getInstance(),
                    task -> calculateUnlockedTagCounts(),
                    initialDelay,
                    intervalTicks
            );
            SupremeTags.getFoliaScheduledTasks().add(future);
        } else {
            Bukkit.getScheduler().runTaskTimer(
                    SupremeTags.getInstance(),
                    () -> calculateUnlockedTagCounts(),
                    1L,
                    intervalTicks
            );
        }
    }

    public static void runAsync(Runnable task) {
        if (SupremeTags.getInstance().isFoliaFound()) {
            Bukkit.getServer().getGlobalRegionScheduler().run(SupremeTags.getInstance(), (s) -> task.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(SupremeTags.getInstance(), task);
        }
    }

    public static void runMain(Runnable task) {
        if (SupremeTags.isFoliaFound()) {
            Bukkit.getServer().getGlobalRegionScheduler().execute(SupremeTags.getInstance(), task);
        } else {
            Bukkit.getScheduler().runTask(SupremeTags.getInstance(), task);
        }
    }

    public static void runMainLater(Runnable task, long ticks) {
        if (SupremeTags.isFoliaFound()) {
            Bukkit.getServer().getGlobalRegionScheduler()
                    .runDelayed(SupremeTags.getInstance(), s -> task.run(), ticks);
        } else {
            Bukkit.getScheduler().runTaskLater(SupremeTags.getInstance(), task, ticks);
        }
    }

    public static boolean isPaperVersionAtLeast(int major, int minor, int patch) {
        String version = Bukkit.getVersion();
        Pattern pattern = Pattern.compile("\\(MC: (\\d+)\\.(\\d+)(?:\\.(\\d+))?\\)");
        Matcher matcher = pattern.matcher(version);

        if (matcher.find()) {
            int majorVer = Integer.parseInt(matcher.group(1));
            int minorVer = Integer.parseInt(matcher.group(2));
            int patchVer = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;

            if (majorVer > major) return true;
            if (majorVer == major && minorVer > minor) return true;
            if (majorVer == major && minorVer == minor && patchVer >= patch) return true;
        }

        return false;
    }

    public static int getTypeAmount(Player player, String type) {
        int count = 0;

        if (type.equalsIgnoreCase("yourtags")) {
            for (Tag tag : SupremeTags.getInstance().getTagManager().getTags().values()) {
                if (player.hasPermission(tag.getPermission())) {
                    count++;
                }
            }
        }

        if (type.equalsIgnoreCase("all")) {
            count = SupremeTags.getInstance().getTagManager().getTags().values().size();
        }

        if (type.startsWith("category:")) {
            String category = type.replace("category:", "");

            for (Tag tag : SupremeTags.getInstance().getTagManager().getTags().values()) {
                if (tag.getCategory().equalsIgnoreCase(category)) {
                    if (player.hasPermission(tag.getPermission())) {
                        count++;
                    }
                }
            }
        }

        if (type.startsWith("rarity:")) {
            String rarity = type.replace("rarity:", "");

            for (Tag tag : SupremeTags.getInstance().getTagManager().getTags().values()) {
                if (tag.getRarity().equalsIgnoreCase(rarity)) {
                    if (player.hasPermission(tag.getPermission())) {
                        count++;
                    }
                }
            }
        }

        return count;
    }

    public static int getVariantTypeAmount(Player player, String type) {
        int count = 0;

        if (type.equalsIgnoreCase("yourtags")) {
            for (Variant tag : SupremeTags.getInstance().getTagManager().getVariants()) {
                if (player.hasPermission(tag.getPermission())) {
                    count++;
                }
            }
        }

        if (type.equalsIgnoreCase("all")) {
            count = SupremeTags.getInstance().getTagManager().getVariants().size();
        }

        if (type.startsWith("category:")) {
            String category = type.replace("category:", "");

            for (Variant tag : SupremeTags.getInstance().getTagManager().getVariants()) {
                if (tag.getSisterTag().getCategory().equalsIgnoreCase(category)) {
                    if (player.hasPermission(tag.getPermission())) {
                        count++;
                    }
                }
            }
        }

        if (type.startsWith("rarity:")) {
            String rarity = type.replace("rarity:", "");

            for (Variant tag : SupremeTags.getInstance().getTagManager().getVariants()) {
                if (tag.getRarity().equalsIgnoreCase(rarity)) {
                    if (player.hasPermission(tag.getPermission())) {
                        count++;
                    }
                }
            }
        }

        return count;
    }

    public static String configMessage(String key, FileConfiguration messages) {
        String prefix = Objects.requireNonNull(messages.getString("messages.prefix"));
        String raw = Objects.requireNonNull(messages.getString(key));
        return raw.replace("%prefix%", prefix);
    }

    public static List<String> configMessageList(String key, FileConfiguration messages) {
        String prefix = messages.getString("messages.prefix", "");
        List<String> rawList = messages.getStringList("messages." + key);

        List<String> formattedList = new ArrayList<>();

        for (String line : rawList) {
            formattedList.add(line.replace("%prefix%", prefix));
        }

        return formattedList;
    }

    public static void playConfigSound(Player player, String value) {
        if (player == null || value == null) return;

        var config = SupremeTags.getInstance().getConfig();
        String basePath = "sounds." + value;

        if (!config.getBoolean(basePath + ".enable", false)) return;

        String soundName = config.getString(basePath + ".sound", "");
        double volume = config.getDouble(basePath + ".volume", 1.0);
        double pitch = config.getDouble(basePath + ".pitch", 1.0);

        Sound sound = XSound.of(soundName).get().get();

        if (sound == null) {
            SupremeTags.getInstance().getLogger().warning("Invalid sound: " + soundName + " (" + value + ")");
            return;
        }

        player.playSound(player.getLocation(), sound, (float) volume, (float) pitch);
    }
}
