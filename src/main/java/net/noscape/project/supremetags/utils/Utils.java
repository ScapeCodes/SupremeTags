package net.noscape.project.supremetags.utils;

import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.FontImages.FontImageWrapper;
import io.th0rgal.oraxen.api.OraxenItems;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.util.Tristate;
import net.md_5.bungee.api.ChatColor;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.handlers.Variant;
import net.noscape.project.supremetags.managers.TagManager;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import su.nightexpress.coinsengine.api.CoinsEngineAPI;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Utils {

    private static final int VERSION = getVersion();
    private static final boolean SUPPORTS_RGB = VERSION >= 16 || VERSION == -1;

    private static final long CACHE_UPDATE_INTERVAL_TICKS = 20L * 60; // 1 minute

    private static final int BATCH_SIZE = 100; // Number of players per chunk
    private static final int BATCH_DELAY_TICKS = 2; // Delay between batches

    private static final Pattern p1 = Pattern.compile("\\{#([0-9A-Fa-f]{6})\\}");
    private static final Pattern p2 = Pattern.compile("&#([A-Fa-f0-9]){6}");
    private static final Pattern p3 = Pattern.compile("#([A-Fa-f0-9]){6}");
    private static final Pattern p4 = Pattern.compile("<#([A-Fa-f0-9])>{6}");
    private static final Pattern p5 = Pattern.compile("<#&([A-Fa-f0-9])>{6}");

    private static final Pattern g1 = Pattern.compile("<gradient:([0-9A-Fa-f]{6})>(.*?)</gradient:([0-9A-Fa-f]{6})>");
    private static final Pattern g2 = Pattern.compile("<gradient:#([A-Fa-f0-9]{6})>(.*?)</gradient:#([A-Fa-f0-9]{6})>");
    private static final Pattern g3 = Pattern.compile("<gradient:&#([A-Fa-f0-9]{6})>(.*?)</gradient:&#([A-Fa-f0-9]{6})>");

    private static final Pattern g4 = Pattern.compile("<g:&#([A-Fa-f0-9]){6}>(.*?)</g:&#([A-Fa-f0-9]){6}");
    private static final Pattern g5 = Pattern.compile("<g:&#([A-Fa-f0-9]){6}>(.*?)</g:&#([A-Fa-f0-9]){6}");
    private static final Pattern g6 = Pattern.compile("<g:&#([A-Fa-f0-9]){6}>(.*?)</g:&#([A-Fa-f0-9]){6}");

    private static final Pattern rainbow1 = Pattern.compile("<rainbow>(.*?)</rainbow>");
    private static final Pattern rainbow2 = Pattern.compile("<r>(.*?)</r>");

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("###.#");

    public static String format(String message) {
        if (isVersionLessThan("1.16")) {
            return ChatColor.translateAlternateColorCodes('&', message);
        }

        boolean useMiniMessage = SupremeTags.getInstance().isMiniMessage() &&
                SupremeTags.getInstance().getConfig().getBoolean("settings.use-minimessage");

        if (useMiniMessage) {
            message = legacyToMiniMessage(message);

            MiniMessage miniMessage = MiniMessage.miniMessage();
            Component component = miniMessage.deserialize(message);
            return LegacyComponentSerializer.legacySection().serialize(component);
        } else {

            // Legacy formatting path
            message = ChatColor.translateAlternateColorCodes('&', message);

            // Handle hex color codes (ensure p1, p2, p3, p4, p5 are properly compiled Pattern instances)
            Matcher hexMatcher = p1.matcher(message);
            while (hexMatcher.find()) {
                message = message.replace(hexMatcher.group(), ChatColor.of(hexMatcher.group().substring(1)).toString());
            }

            hexMatcher = p2.matcher(message);
            while (hexMatcher.find()) {
                message = message.replace(hexMatcher.group(), ChatColor.of(hexMatcher.group().substring(1)).toString());
            }

            Matcher[] matchers = {p3.matcher(message), p4.matcher(message), p5.matcher(message)};
            for (Matcher matcher : matchers) {
                while (matcher.find()) {
                    String hexColor = matcher.group().replaceAll("[<#&>]", "").substring(0, 6);
                    message = message.replace(matcher.group(), ChatColor.of(hexColor).toString());
                }
            }

            // Replace custom tags with legacy codes
            message = message.replace("<black>", "§0")
                    .replace("<dark_blue>", "§1")
                    .replace("<dark_green>", "§2")
                    .replace("<dark_aqua>", "§3")
                    .replace("<dark_red>", "§4")
                    .replace("<dark_purple>", "§5")
                    .replace("<gold>", "§6")
                    .replace("<gray>", "§7")
                    .replace("<dark_gray>", "§8")
                    .replace("<blue>", "§9")
                    .replace("<green>", "§a")
                    .replace("<aqua>", "§b")
                    .replace("<red>", "§c")
                    .replace("<light_purple>", "§d")
                    .replace("<yellow>", "§e")
                    .replace("<white>", "§f")
                    .replace("<obfuscated>", "§k")
                    .replace("<bold>", "§l")
                    .replace("<strikethrough>", "§m")
                    .replace("<underlined>", "§n")
                    .replace("<italic>", "§o")
                    .replace("<reset>", "§r");
        }

        return message;
    }

    private static String legacyToMiniMessage(String message) {
        return message
                .replace("&0", "<black>")
                .replace("&1", "<dark_blue>")
                .replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>")
                .replace("&4", "<dark_red>")
                .replace("&5", "<dark_purple>")
                .replace("&6", "<gold>")
                .replace("&7", "<gray>")
                .replace("&8", "<dark_gray>")
                .replace("&9", "<blue>")
                .replace("&a", "<green>")
                .replace("&b", "<aqua>")
                .replace("&c", "<red>")
                .replace("&d", "<light_purple>")
                .replace("&e", "<yellow>")
                .replace("&f", "<white>")
                .replace("&k", "<obfuscated>")
                .replace("&l", "<bold>")
                .replace("&m", "<strikethrough>")
                .replace("&n", "<underlined>")
                .replace("&o", "<italic>")
                .replace("&r", "<reset>");
    }


    private static String createRainbowTextWithFormatting(String text, String formatting) {
        int length = text.length();
        Color[] rainbowColors = {
                Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.BLUE, Color.MAGENTA
        };
        int colorCount = rainbowColors.length;

        StringBuilder rainbowBuilder = new StringBuilder();

        for (int i = 0; i < length; i++) {
            char currentChar = text.charAt(i);

            // Calculate the color for this character
            Color color = rainbowColors[i % colorCount];
            String hexColor = String.format("%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());

            rainbowBuilder.append(toMinecraftHex(hexColor)).append(formatting).append(currentChar);
        }

        return rainbowBuilder.toString();
    }

    private static String toMinecraftHex(String hexColor) {
        StringBuilder minecraftHex = new StringBuilder("§x");
        for (char c : hexColor.toCharArray()) {
            minecraftHex.append("§").append(c);
        }
        return minecraftHex.toString();
    }

    public static boolean isValidVersion(String version) {
        return version.matches("\\d+(\\.\\d+)*"); // Matches version strings like "1", "1.2", "1.2.3", etc.
    }

    public static boolean isVersionLessThan(String version) {
        String serverVersion = Bukkit.getVersion();
        String[] serverParts = serverVersion.split(" ")[2].split("\\.");
        String[] targetParts = version.split("\\.");

        for (int i = 0; i < Math.min(serverParts.length, targetParts.length); i++) {
            if (!isValidVersion(serverParts[i]) || !isValidVersion(targetParts[i])) {
                return false;
            }

            int serverPart = Integer.parseInt(serverParts[i]);
            int targetPart = Integer.parseInt(targetParts[i]);

            if (serverPart < targetPart) {
                return true;
            } else if (serverPart > targetPart) {
                return false;
            }
        }
        return serverParts.length < targetParts.length;
    }

    /**
     * Gets a simplified major version (..., 9, 10, ..., 14).
     * In most cases, you shouldn't be using this method.
     *
     * @return the simplified major version, or -1 for bungeecord
     * @since 1.0.0
     */
    private static int getVersion() {
        if (!classExists("org.bukkit.Bukkit") && classExists("net.md_5.bungee.api.ChatColor")) {
            return -1;
        }

        String version = Bukkit.getVersion();
        Validate.notEmpty(version, "Cannot get major Minecraft version from null or empty string");

        // getVersion()
        int index = version.lastIndexOf("MC:");
        if (index != -1) {
            version = version.substring(index + 4, version.length() - 1);
        } else if (version.endsWith("SNAPSHOT")) {
            // getBukkitVersion()
            index = version.indexOf('-');
            version = version.substring(0, index);
        }
        // 1.13.2, 1.14.4, etc...
        int lastDot = version.lastIndexOf('.');
        if (version.indexOf('.') != lastDot) version = version.substring(0, lastDot);

        return Integer.parseInt(version.substring(2));
    }

    /**
     * Checks if a class exists in the current server
     *
     * @param path The path of that class
     * @return true if the class exists, false if it doesn't
     * @since 1.0.7
     */
    private static boolean classExists(final String path) {
        try {
            Class.forName(path);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static String parseMiniMessage(String message) {
        // Handle MiniMessage parsing and legacy conversion
        Component legacy = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
        String miniMessage = MiniMessage.miniMessage().serialize(legacy).replace("\\", "");
        Component component = MiniMessage.miniMessage().deserialize(miniMessage);
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    public static String colorizeRGB(String input) {
        Matcher matcher = p1.matcher(input);
        String color;
        while (matcher.find()) {
            color = matcher.group(1);
            if (color == null) {
                color = matcher.group(2);
            }
            input = input.replace(matcher.group(), ChatColor.of(color) + "");
        }
        return input;
    }

    public static void addPerm(OfflinePlayer player, String permission) {
        SupremeTags.getPermissions().playerAdd(null, player, permission); // null = global
    }

    public static void removePerm(OfflinePlayer player, String permission) {
        SupremeTags.getPermissions().playerRemove(null, player, permission);
    }

    public static boolean hasAmount(Player player, String economyType, double cost) {
        if (economyType.equalsIgnoreCase("VAULT")) {
            return SupremeTags.getEconomy().has(player, cost);
        } else if (economyType.equalsIgnoreCase("PLAYERPOINTS")) {
            return SupremeTags.getInstance().getPpAPI().look(player.getUniqueId()) >= cost;
        } else if (economyType.equalsIgnoreCase("EXP_LEVEL")) {
            return player.getLevel() >= cost;
        } else if (economyType.startsWith("COINSENGINE-")) {
            String eco_name = economyType.replace("COINSENGINE-", "");
            return CoinsEngineAPI.getBalance(player.getUniqueId(), eco_name) >= cost;
        }

        return false;
    }

    public static void take(Player player, String economyType, double cost) {
        if (economyType.equalsIgnoreCase("VAULT")) {
            SupremeTags.getEconomy().withdrawPlayer(player, cost);
        } else if (economyType.equalsIgnoreCase("PLAYERPOINTS")) {
            SupremeTags.getInstance().getPpAPI().take(player.getUniqueId(), (int) cost);
        } else if (economyType.equalsIgnoreCase("EXP_LEVEL")) {
            player.setLevel((int) (player.getLevel() - cost));
        } else if (economyType.startsWith("COINSENGINE-")) {
            String eco_name = economyType.replace("COINSENGINE-", "");
            CoinsEngineAPI.removeBalance(player.getUniqueId(), eco_name, cost);
        }
    }

    public static String deformat(String str) {
        return ChatColor.stripColor(format(str));
    }

    public static void msgPlayer(Player player, String... str) {
        for (String msg : str) {
            player.sendMessage(format(msg));
        }
    }

    public static void msgPlayer(CommandSender player, String... str) {
        for (String msg : str) {
            player.sendMessage(format(msg));
        }
    }

    public static void titlePlayer(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.sendTitle(format(title), format(subtitle), fadeIn, stay, fadeOut);
    }

    public static void soundPlayer(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    public static List<String> color(List<String> lore) {
        return lore.stream().map(Utils::format).collect(Collectors.toList());
    }

    private static final Pattern rgbPat = Pattern.compile("(?:#|0x)(?:[a-f0-9]{3}|[a-f0-9]{6})\\b|(?:rgb|hsl)a?\\([^\\)]*\\)");

    public static String getRGB(String msg) {
        String temp = msg;
        try {

            String status = "none";
            String r = "";
            String g = "";
            String b = "";
            Matcher match = rgbPat.matcher(msg);
            while (match.find()) {
                String color = msg.substring(match.start(), match.end());
                for (char character : msg.substring(match.start(), match.end()).toCharArray()) {
                    switch (character) {
                        case '(':
                            status = "r";
                            continue;
                        case ',':
                            switch (status) {
                                case "r":
                                    status = "g";
                                    continue;
                                case "g":
                                    status = "b";
                                    continue;
                                default:
                                    break;
                            }
                        default:
                            switch (status) {
                                case "r":
                                    r = r + character;
                                    continue;
                                case "g":
                                    g = g + character;
                                    continue;
                                case "b":
                                    b = b + character;
                                    continue;
                            }
                            break;
                    }


                }
                b = b.replace(")", "");
                Color col = new Color(Integer.parseInt(r), Integer.parseInt(g), Integer.parseInt(b));
                temp = temp.replaceFirst("(?:#|0x)(?:[a-f0-9]{3}|[a-f0-9]{6})\\b|(?:rgb|hsl)a?\\([^\\)]*\\)", ChatColor.of(col) + "");
                r = "";
                g = "";
                b = "";
                status = "none";
            }
        } catch (Exception e) {
            return msg;
        }
        return temp;
    }

    public static String replacePlaceholders(Player user, String base) {
        if (Bukkit.getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) return base;

        return PlaceholderAPI.setPlaceholders(user, base);
    }

    public static String globalPlaceholders(Player user, String message) {
        message = replacePlaceholders(user, message);
        if (Bukkit.getServer().getPluginManager().getPlugin("ItemsAdder") != null) {
            message = FontImageWrapper.replaceFontImages(message);
        }

        return message;
    }

    public static boolean isCustomGUIItem(ItemStack itemStack) {
        FileConfiguration guis = SupremeTags.getInstance().getConfigManager().getConfig("guis.yml").get();

        ConfigurationSection customItemsSection = guis.getConfigurationSection("gui.tag-menu.custom-items");
        if (customItemsSection == null) {
            return false;
        }

        for (String key : customItemsSection.getKeys(false)) {
            ConfigurationSection itemSection = customItemsSection.getConfigurationSection(key);
            if (itemSection != null) {
                if (itemStack.getItemMeta().getDisplayName().equals(itemSection.getString(format("displayname")))
                        && itemStack.getItemMeta().getCustomModelData() == itemSection.getInt("custom-model-data")) {
                    return true;
                }
            }
            break;
        }

        return false; // Item not found in custom items
    }

    public static int isCustomGUIItemSlot(Player player, ItemStack itemStack) {
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
                    return guis.getInt("gui.tag-menu.custom-items." + key + ".slot");
                }
            }
        }

        return -1; // Indicates no match found
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

        return ""; // Indicates no match found
    }

    public static int isCustomGUIItemMMSlot(Player player, ItemStack itemStack) {
        FileConfiguration guis = SupremeTags.getInstance().getConfigManager().getConfig("guis.yml").get();

        for (String key : guis.getConfigurationSection("gui.main-menu.custom-items").getKeys(false)) {
            if (itemStack != null && itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()) {
                String displayName = deformat(itemStack.getItemMeta().getDisplayName());
                int customModelData;
                if (itemStack.getItemMeta().hasCustomModelData()) {
                    customModelData = itemStack.getItemMeta().getCustomModelData();
                } else {
                    customModelData = 0;
                }

                String material = guis.getString("gui.main-menu.custom-items." + key + ".material");
                String displaynameConfig = guis.getString("gui.main-menu.custom-items." + key + ".displayname");
                int configCustomModelData = guis.getInt("gui.main-menu.custom-items." + key + ".custom-model-data");

                displaynameConfig = replacePlaceholders(player, displaynameConfig);

                if (material != null && displayName.equals(displaynameConfig) && customModelData == configCustomModelData) {
                    return guis.getInt("gui.main-menu.custom-items." + key + ".slot");
                }
            }
        }

        return -1; // Indicates no match found
    }

    public static String isCustomGUIItemMMName(Player player, ItemStack itemStack) {
        FileConfiguration guis = SupremeTags.getInstance().getConfigManager().getConfig("guis.yml").get();

        for (String key : guis.getConfigurationSection("gui.main-menu.custom-items").getKeys(false)) {
            if (itemStack != null && itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()) {
                String displayName = deformat(itemStack.getItemMeta().getDisplayName());
                int customModelData;
                if (itemStack.getItemMeta().hasCustomModelData()) {
                    customModelData = itemStack.getItemMeta().getCustomModelData();
                } else {
                    customModelData = 0;
                }

                String material = guis.getString("gui.main-menu.custom-items." + key + ".material");
                String displaynameConfig = guis.getString("gui.main-menu.custom-items." + key + ".displayname");
                int configCustomModelData = guis.getInt("gui.main-menu.custom-items." + key + ".custom-model-data");

                displaynameConfig = replacePlaceholders(player, displaynameConfig);

                if (material != null && displayName.equals(displaynameConfig) && customModelData == configCustomModelData) {
                    return key;
                }
            }
        }

        return ""; // Indicates no match found
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
        return 0; // versions are equal
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

    public static void addTagDisplayName(Player player, String identifier) {
        boolean isDisplayName = SupremeTags.getInstance().getConfig().getBoolean("settings.auto-apply.displayname");
        boolean isTab = SupremeTags.getInstance().getConfig().getBoolean("settings.auto-apply.tab");

        // =============================================================
        // TAB -
        // =============================================================
        String oldPlayerName = player.getPlayerListName();
        System.out.println("Before Tag Removal (Tab): " + oldPlayerName); // Log for debugging

        if (isTab) {
            // Remove all existing tags from player list name
            oldPlayerName = removeAllTagsFromString(oldPlayerName);

            System.out.println("After Tag Removal (Tab): " + oldPlayerName); // Log for debugging

            if (identifier.equalsIgnoreCase("none")) {
                player.setPlayerListName(format(oldPlayerName));
                return;
            }

            // Determine the new tag to add
            String addTag = getTagForIdentifier(identifier, player);
            String newPlayerName = SupremeTags.getInstance().getConfig().getString("settings.auto-apply.tab-format")
                    .replaceAll("%playerlistname%", oldPlayerName).replaceAll("%tag%", addTag);

            System.out.println("Final Player Name (Tab): " + newPlayerName); // Log for debugging

            player.setPlayerListName(format(newPlayerName));
        }

        // =============================================================
        // DISPLAYNAME -
        // =============================================================
        String currentDisplayName = player.getDisplayName();
        System.out.println("Before Tag Removal (Display Name): " + currentDisplayName); // Log for debugging

        if (isDisplayName) {
            // Remove all existing tags from display name
            currentDisplayName = removeAllTagsFromString(currentDisplayName);

            System.out.println("After Tag Removal (Display Name): " + currentDisplayName); // Log for debugging

            if (identifier.equalsIgnoreCase("none")) {
                player.setDisplayName(format(currentDisplayName));
                return;
            }

            // Determine the new tag to add
            String addTag = getTagForIdentifier(identifier, player);
            String newDisplayName = SupremeTags.getInstance().getConfig().getString("settings.auto-apply.displayname-format")
                    .replaceAll("%displayname%", currentDisplayName).replaceAll("%tag%", addTag);

            System.out.println("Final Display Name: " + newDisplayName); // Log for debugging

            player.setDisplayName(format(newDisplayName));
        }
    }

    // Helper method to remove all tags from a string (for both tab list and display name)
    private static String removeAllTagsFromString(String name) {
        // First, strip color formatting to ensure consistency
        String strippedName = ChatColor.stripColor(name);

        for (Tag tags : SupremeTags.getInstance().getTagManager().getTags().values()) {
            for (String t : tags.getTag()) {
                strippedName = strippedName.replace(t, "").trim();
            }
            for (Variant v : tags.getVariants()) {
                for (String t : v.getTag()) {
                    strippedName = strippedName.replace(t, "").trim();
                }
            }
        }

        return strippedName;
    }

    // Helper method to get the appropriate tag for an identifier (normal tag, variant, or player-specific)
    private static String getTagForIdentifier(String identifier, Player player) {
        String addTag = "";

        if (SupremeTags.getInstance().getTagManager().doesTagExist(identifier)) {
            addTag = SupremeTags.getInstance().getTagManager().getTag(identifier).getTag().get(0);
        } else if (SupremeTags.getInstance().getTagManager().isVariant(identifier)) {
            addTag = SupremeTags.getInstance().getTagManager().getVariant(identifier).getTag().get(0);
        } else if (SupremeTags.getInstance().getPlayerManager().doesTagExist(player.getUniqueId(), identifier)) {
            addTag = SupremeTags.getInstance().getPlayerManager().getTag(player.getUniqueId(), identifier).getTag().get(0);
        }

        return addTag;
    }

    public static void calculateUnlockedTagCounts() {
        runAsync(() -> {
            LuckPerms luckPerms = LuckPermsProvider.get();
            UserManager userManager = luckPerms.getUserManager();
            Map<String, Integer> result = new HashMap<>();

            for (Tag tag : SupremeTags.getInstance().getTagManager().getTags().values()) {
                String permission = tag.getPermission();
                int unlocked = 0;

                for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
                    try {
                        User user = userManager.loadUser(offlinePlayer.getUniqueId()).join();
                        if (user != null) {
                            Tristate resultperm = user.getCachedData().getPermissionData().checkPermission(permission);
                            if (resultperm == Tristate.TRUE) {
                                unlocked++;
                            }
                        }
                    } catch (Exception e) {
                        // Optional: log the error
                    }
                }

                result.put(tag.getIdentifier(), unlocked);
            }

            for (Variant var : SupremeTags.getInstance().getTagManager().getVariants()) {
                String permission = var.getPermission();
                int unlocked = 0;

                for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
                    try {
                        User user = userManager.loadUser(offlinePlayer.getUniqueId()).join();
                        if (user != null) {
                            Tristate resultperm = user.getCachedData().getPermissionData().checkPermission(permission);
                            if (resultperm == Tristate.TRUE) {
                                unlocked++;
                            }
                        }
                    } catch (Exception e) {
                        // Optional: log the error
                    }
                }

                result.put(var.getIdentifier(), unlocked);
            }

            TagManager.tagUnlockCounts.clear();
            TagManager.tagUnlockCounts.putAll(result);
        });
    }

    public static void scheduleUnlockCount() {
        long intervalTicks = 20L * SupremeTags.getInstance().getConfig().getInt("settings.update-unlocked-cache");
        long initialDelay = Math.max(1L, intervalTicks); // Ensure at least 1 tick delay

        if (SupremeTags.getInstance().isFoliaFound()) {
            Bukkit.getServer().getGlobalRegionScheduler().runAtFixedRate(
                    SupremeTags.getInstance(),
                    task -> calculateUnlockedTagCounts(),
                    initialDelay,
                    intervalTicks
            );
        } else {
            Bukkit.getScheduler().runTaskTimer(
                    SupremeTags.getInstance(),
                    () -> calculateUnlockedTagCounts(),
                    1L, // Always use at least 1 tick for compatibility
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
        if (SupremeTags.getInstance().isFoliaFound()) {
            Bukkit.getServer().getGlobalRegionScheduler().execute(SupremeTags.getInstance(), task); // global sync task
        } else {
            Bukkit.getScheduler().runTask(SupremeTags.getInstance(), task);
        }
    }

    public static boolean isPaperVersionAtLeast(int major, int minor, int patch) {
        String version = Bukkit.getVersion(); // Example: git-Paper-441 (MC: 1.21.5)
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
}