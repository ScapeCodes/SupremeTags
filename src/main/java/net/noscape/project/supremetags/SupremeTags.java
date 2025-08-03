package net.noscape.project.supremetags;

import com.nexomc.nexo.api.NexoItems;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import net.noscape.project.supremetags.api.SupremeTagsAPI;
import net.noscape.project.supremetags.checkers.Metrics;
import net.noscape.project.supremetags.checkers.UpdateChecker;
import net.noscape.project.supremetags.commands.tags.TagsCommand;
import net.noscape.project.supremetags.handlers.Editor;
import net.noscape.project.supremetags.handlers.SetupTag;
import net.noscape.project.supremetags.handlers.hooks.PAPI;
import net.noscape.project.supremetags.handlers.menu.MenuUtil;
import net.noscape.project.supremetags.managers.*;
import net.noscape.project.supremetags.storage.*;
import net.noscape.project.supremetags.utils.BungeeMessaging;
import net.noscape.project.supremetags.utils.ClassRegistrationUtils;
import net.noscape.project.supremetags.utils.commands.CommandFramework;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import static net.noscape.project.supremetags.utils.Utils.*;

public final class SupremeTags extends JavaPlugin {

    private static SupremeTags instance;
    private ConfigManager configManager;
    private TagManager tagManager;
    private CategoryManager categoryManager;
    private MergeManager mergeManager;
    private VoucherManager voucherManager;
    private RarityManager rarityManager;

    private static SupremeTagsAPI api;

    private static Economy econ = null;
    private static Permission perms = null;
    private PlayerPointsAPI ppAPI;

    private static MySQLDatabase mysql;
    private static H2Database h2;
    private static SQLiteDatabase sqlite;
    private final SQLiteUserData sqLiteUser = new SQLiteUserData();
    private final H2UserData h2user = new H2UserData();
    private static String connectionURL;
    private final MySQLUserData user = new MySQLUserData();

    private static final HashMap<Player, MenuUtil> menuUtilMap = new HashMap<>();
    private final HashMap<Player, Editor> editorList = new HashMap<>();
    private final HashMap<Player, SetupTag> setupList = new HashMap<>();

    private boolean legacy_format;
    private boolean minimessage;
    private boolean cmi_hex;
    private boolean disabledWorldsTag;
    private boolean deactivateClick;

    private PlayerManager playerManager;
    private PlayerConfig playerConfig;

    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
    private boolean useSSL;

    private String layout = getConfig().getString("setting.layout-type");

    private Boolean isFoliaFound;

    private final CommandFramework commandFramework = new CommandFramework(this);

    private DataCache dataCache;

    private ItemStack head;

    @Override
    public void onEnable() {
        init();
    }

    @Override
    public void onDisable() {
        tagManager.unloadTags();
        editorList.clear();
        setupList.clear();
        dataCache.clearCache();
        if (!isFoliaFound()) {
            this.getServer().getScheduler().cancelTasks(this);
        }

        if (isProtocolLib()) {
            try {
                Class<?> clazz = Class.forName("net.noscape.project.supremetags.handlers.packets.ProtocolLibHandler");
                Object handler = clazz.getConstructor(Plugin.class).newInstance(this);
                clazz.getMethod("unRegister").invoke(handler);
            } catch (Exception e) {
                //logger.warning("Failed to register ProtocolLib listener: " + e.getMessage());
            }
        } else if (isPacketEvents()) {
            try {
                Class<?> clazz = Class.forName("net.noscape.project.supremetags.handlers.packets.PacketEventsHandler");
                Object handler = clazz.getConstructor(Plugin.class).newInstance(this);
                clazz.getMethod("unRegister").invoke(handler);
            } catch (Exception e) {
                //logger.warning("Failed to register PacketEvents listener: " + e.getMessage());
            }
        }

        if (isMySQL() || isMaria()) {
            mysql.disconnect();
        }
        if (isSQLite()) {
            sqlite.disconnect();
        }
    }

    private void registerCommand(String mainCommand, List<String> aliases) {
        // Register the main command dynamically
        PluginCommand command = getCommand(mainCommand);

        if (command == null) {
            getLogger().severe("Could not find command: " + mainCommand + ". Please check your plugin.yml file.");
            return;
        }

        command.setExecutor(new TagsCommand());
        command.setTabCompleter(new TagsCommand());
        command.setAliases(aliases);
    }

    private void init() {
        instance = this;

        Logger logger = Bukkit.getLogger();

        this.getConfig().options().copyDefaults(true);
        this.saveConfig();
        configManager = new ConfigManager(this);

        host = configManager.getConfig("data.yml").get().getString("data.address");
        port = configManager.getConfig("data.yml").get().getInt("data.port");
        database = configManager.getConfig("data.yml").get().getString("data.database");
        username = configManager.getConfig("data.yml").get().getString("data.username");
        password = configManager.getConfig("data.yml").get().getString("data.password");
        useSSL = configManager.getConfig("data.yml").get().getBoolean("data.useSSL");

        this.callMetrics();

        dataCache = new DataCache();
        loadDatabases();

        sendConsoleLog();

        tagManager = new TagManager(getConfig().getBoolean("settings.cost-system"));
        categoryManager = new CategoryManager();
        playerManager = new PlayerManager();
        voucherManager = new VoucherManager();
        mergeManager = new MergeManager();
        playerConfig = new PlayerConfig();
        rarityManager = new RarityManager();

        String mainCommand = getConfig().getString("settings.commands.main-command", "tags");
        List<String> aliases = getConfig().getStringList("settings.commands.aliases");
        registerCommand(mainCommand, aliases);

        ClassRegistrationUtils.loadCommands("net.noscape.project.supremetags.commands", this);
        ClassRegistrationUtils.loadListeners("net.noscape.project.supremetags.listeners", this);

        if (isProtocolLib()) {
            try {
                Class<?> clazz = Class.forName("net.noscape.project.supremetags.handlers.packets.ProtocolLibHandler");
                Object handler = clazz.getConstructor(Plugin.class).newInstance(this);
                clazz.getMethod("register").invoke(handler);
                logger.info("ProtocolLib found! Registered packet listener.");
            } catch (Exception e) {
                logger.warning("Failed to register ProtocolLib listener: " + e.getMessage());
            }
        } else if (isPacketEvents()) {
            try {
                Class<?> clazz = Class.forName("net.noscape.project.supremetags.handlers.packets.PacketEventsHandler");
                Object handler = clazz.getConstructor(Plugin.class).newInstance(this);
                clazz.getMethod("register").invoke(handler);
                logger.info("PacketEvents found! Registered packet listener.");
            } catch (Exception e) {
                logger.warning("Failed to register PacketEvents listener: " + e.getMessage());
            }
        } else {
            logger.info("> No packet lib found! Skipping packet listeners.");
        }

        legacy_format = getConfig().getBoolean("settings.color-formatting.legacy-hex-format");
        minimessage = getConfig().getBoolean("settings.use-minimessage");
        cmi_hex = getConfig().getBoolean("settings.color-formatting.cmi-color-support");
        disabledWorldsTag = getConfig().getBoolean("settings.tag-command-in-disabled-worlds");
        layout = getConfig().getString("settings.layout-type");
        deactivateClick = getConfig().getBoolean("settings.deactivate-click");

        merge(logger);

        if (isPlaceholderAPI()) {
            logger.info("> PlaceholderAPI: Found");
            new PAPI(this).register();
        } else {
            logger.info("> PlaceholderAPI: Not Found!");
        }

        api = new SupremeTagsAPI();

        tagManager.validateTags(false);

        // load tags again incase they did not load properly, on first installment.
        if (tagManager.getTags().isEmpty()) {
            tagManager.loadTags(false);
        }

        if (getServer().getPluginManager().getPlugin("Luckperms") != null) {
            calculateUnlockedTagCounts();
            scheduleUnlockCount();
        } else {
            logger.warning("> Luckperms not found! disabling unlocked count function.");
        }

        Material skullMaterial;
        try {
            skullMaterial = Material.valueOf("PLAYER_HEAD"); // 1.13+
        } catch (IllegalArgumentException e) {
            skullMaterial = Material.valueOf("SKULL_ITEM");  // 1.12-
            this.head = new ItemStack(skullMaterial, 1, (short) 3);
            return;
        }
        this.head = new ItemStack(skullMaterial, 1);

        if (getConfig().getBoolean("settings.bungee-messaging")) {
            BungeeMessaging.registerChannels();
        }
    }

    public static SupremeTags getInstance() { return instance; }

    public TagManager getTagManager() { return tagManager; }

    public CategoryManager getCategoryManager() { return categoryManager; }

    public static MenuUtil getMenuUtil(Player player) {
        MenuUtil menuUtil;

        if (menuUtilMap.containsKey(player)) {
            return menuUtilMap.get(player);
        } else {
            menuUtil = new MenuUtil(player, UserData.getActive(player.getUniqueId()));
            menuUtil.setFilter("all");
            menuUtil.setSort("none");
            menuUtilMap.put(player, menuUtil);
        }

        return menuUtil;
    }

    public static MenuUtil getMenuUtilIdentifier(Player player, String identifier) {
        MenuUtil menuUtil;

        if (menuUtilMap.containsKey(player)) {
            return menuUtilMap.get(player);
        } else {
            menuUtil = new MenuUtil(player, identifier);
            menuUtil.setFilter("all");
            menuUtil.setSort("none");
            menuUtilMap.put(player, menuUtil);
        }

        return menuUtil;
    }
 
    public static MenuUtil getMenuUtil(Player player, String category) {
        MenuUtil menuUtil;

        if (menuUtilMap.containsKey(player)) {
            return menuUtilMap.get(player);
        } else {
            menuUtil = new MenuUtil(player, UserData.getActive(player.getUniqueId()), category);
            menuUtil.setFilter("all");
            menuUtil.setSort("none");
            menuUtilMap.put(player, menuUtil);
        }

        return menuUtil;
    }

    public HashMap<Player, MenuUtil> getMenuUtil() {
        return menuUtilMap;
    }

    public static String getConnectionURL() {
        return connectionURL;
    }

    public H2UserData getUserData() { return h2user; }

    public static H2Database getH2Database() { return h2; }

    public MySQLUserData getUser() {
        return instance.user;
    }

    public static MySQLDatabase getMysql() {
        return mysql;
    }

    public CommandFramework getCommandFramework() {
        return commandFramework;
    }

    public void reload() {
        /// reloading the config.yml
        super.reloadConfig();

        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();

        configManager.reloadConfig("categories.yml");
        configManager.reloadConfig("banned-words.yml");
        configManager.reloadConfig("data.yml");
        configManager.reloadConfig("guis.yml");
        configManager.reloadConfig("tags.yml");
        configManager.reloadConfig("messages.yml");
        configManager.reloadConfig("rarities.yml");

        legacy_format = getConfig().getBoolean("settings.color-formatting.legacy-hex-format");
        minimessage = getConfig().getBoolean("settings.use-minimessage");
        cmi_hex = getConfig().getBoolean("settings.color-formatting.cmi-color-support");
        disabledWorldsTag = getConfig().getBoolean("settings.tag-command-in-disabled-worlds");
        layout = getConfig().getString("settings.layout-type");
        deactivateClick = getConfig().getBoolean("settings.deactivate-click");

        rarityManager.unloadRarities();
        rarityManager.loadRarities();

        tagManager.unloadTags();
        tagManager.loadTags(false);

        tagManager.getDataItem().clear();

        tagManager.setCost(SupremeTags.getInstance().getConfig().getBoolean("settings.cost-system"));

        categoryManager.initCategories();

        configManager.reloadConfig("messages.yml");
    }

    private void loadDatabases() {
        if (isH2()) {
            connectionURL = "jdbc:h2:" + getDataFolder().getAbsolutePath() + "/database";
            h2 = new H2Database(connectionURL);
        }

        if (isSQLite()) {
            connectionURL = "jdbc:sqlite:" + getDataFolder().getAbsolutePath() + "/database.db";
            sqlite = new SQLiteDatabase(connectionURL);
        }

        if (isMySQL() || isMaria()) {
            mysql = new MySQLDatabase(host, port, database, username, password, useSSL);
        }
    }

    public boolean isLegacyFormat() {
        return legacy_format;
    }

    public void merge(Logger log) {
        mergeManager.merge(log);
    }

    private void sendConsoleLog() {
        Logger logger = Bukkit.getLogger();

        logger.info("");
        logger.info("  ____  _   _ ____  ____  _____ __  __ _____ _____  _    ____ ____  ");
        logger.info(" / ___|| | | |  _ \\|  _ \\| ____|  \\/  | ____|_   _|/ \\  / ___/ ___| ");
        logger.info(" \\___ \\| | | | |_) | |_) |  _| | |\\/| |  _|   | | / _ \\| |  _\\___ \\ ");
        logger.info("  ___) | |_| |  __/|  _ <| |___| |  | | |___  | |/ ___ \\ |_| |___) |");
        logger.info(" |____/ \\___/|_|   |_| \\_\\_____|_|  |_|_____| |_/_/   \\_\\____|____/ ");
        logger.info(" Allow players to show off their supreme tags!");
        logger.info("");

        isFoliaFound();

        logger.info("");
        logger.info("> Version: " + getDescription().getVersion());
        logger.info("> Author: DevScape");

        if (Bukkit.getPluginManager().getPlugin("PlayerPoints") != null) {
            this.ppAPI = PlayerPoints.getInstance().getAPI();
            logger.info("> PlayerPoints: Found!");
        } else {
            logger.info("> PlayerPoints: Not Found!");
        }

        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            setupEconomy();
            setupPermissions();
            logger.info("> Vault: Found!");
        } else {
            logger.info("> Vault: Not Found!");
        }

        if (isCoinsEngine()) {
            logger.info("> CoinsEngine: Found!");
        } else {
            logger.info("> CoinsEngine: Not Found!");
        }

        if (isH2()) {
            logger.info("> Database: H2!");
        } else if (isMySQL()) {
            logger.info("> Database: MySQL!");
        } else if (isMaria()) {
            logger.info("> Database: MariaDB!");
        } else if (isSQLite()) {
            logger.info("> Database: SQLite!");
        }

        if (getConfig().getBoolean("settings.update-check")) {
            new UpdateChecker(this, 111481).getVersion(version -> {
                if (version == null) {
                    logger.warning("> Updater: Failed to retrieve latest version of SupremeTags.");
                    return;
                }

                String currentVersion = this.getDescription().getVersion();
                if (compareVersions(version, currentVersion) > 0) {
                    logger.info("> Updater: An update is available! " + version);
                    logger.info("Download at https://www.spigotmc.org/resources/111481/updates");
                } else {
                    logger.info("> Updater: Plugin up to date!");
                }
            });
        }
    }

    public boolean isBungeeCord() {
        return getConfig().getString("settings.messaging-platform").equalsIgnoreCase("bungeecord");
    }

    public boolean isNoPermissionMenuAction() {
        return getConfig().getBoolean("settings.no-permission-menu-action");
    }

    public boolean isFoliaFound() {
        if (this.isFoliaFound != null) {
            return this.isFoliaFound;
        }
        try {
            this.getLogger().info("[FOLIA] Found: " + Class.forName("io.papermc.paper.threadedregions.RegionizedServer"));
            this.getLogger().info("[ST-Folia-Version] Please report any compatibility issues with folia schedulers/features not working properly to ScapeHelp discord.");
            this.isFoliaFound = true;
        } catch (Exception ex) {
            this.isFoliaFound = false;
        }
        return this.isFoliaFound;
    }

    public Boolean isH2() {
        return Objects.requireNonNull(configManager.getConfig("data.yml").get().getString("data.type")).equalsIgnoreCase("H2");
    }

    public Boolean isMaria() {
        return Objects.requireNonNull(configManager.getConfig("data.yml").get().getString("data.type")).equalsIgnoreCase("MARIADB");
    }

    public Boolean isMySQL() {
        return Objects.requireNonNull(configManager.getConfig("data.yml").get().getString("data.type")).equalsIgnoreCase("MYSQL");
    }

    public boolean isSQLite() {
        return Objects.requireNonNull(configManager.getConfig("data.yml").get().getString("data.type")).equalsIgnoreCase("SQLite");
    }

    public boolean isDataCache() {
        return configManager.getConfig("data.yml").get().getBoolean("data.cache-data");
    }

    private void callMetrics() {
        int pluginId = 19397;
        Metrics metrics = new Metrics(this, pluginId);

        metrics.addCustomChart(new Metrics.SimplePie("used_language", () -> getConfig().getString("language", "en")));

        metrics.addCustomChart(new Metrics.DrilldownPie("java_version", () -> {
            Map<String, Map<String, Integer>> map = new HashMap<>();
            String javaVersion = System.getProperty("java.version");
            Map<String, Integer> entry = new HashMap<>();
            entry.put(javaVersion, 1);
            if (javaVersion.startsWith("1.7")) {
                map.put("Java 1.7", entry);
            } else if (javaVersion.startsWith("1.8")) {
                map.put("Java 1.8", entry);
            } else if (javaVersion.startsWith("1.9")) {
                map.put("Java 1.9", entry);
            } else {
                map.put("Other", entry);
            }
            return map;
        }));
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public PlayerPointsAPI getPpAPI() {
        return ppAPI;
    }

    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
        return perms != null;
    }

    public static Economy getEconomy() {
        return econ;
    }

    public static Permission getPermissions() {
        return perms;
    }

    public static SupremeTagsAPI getTagAPI() {
        return api;
    }

    public DataCache getDataCache() { return dataCache; }

    public HashMap<Player, Editor> getEditorList() {
        return editorList;
    }

    public boolean isCMIHex() {
        return cmi_hex;
    }

    public boolean isMiniMessage() {
        return minimessage;
    }

    public boolean isDisabledWorldsTag() {
        return disabledWorldsTag;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MergeManager getMergeManager() {
        return mergeManager;
    }

    public PlayerConfig getPlayerConfig() {
        return playerConfig;
    }

    public RarityManager getRarityManager() {
        return rarityManager;
    }

    public HashMap<Player, SetupTag> getSetupList() {
        return setupList;
    }

    public boolean isPlaceholderAPI() {
        return getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    public boolean isProtocolLib() {
        Plugin plugin = getServer().getPluginManager().getPlugin("ProtocolLib");
        return plugin != null && plugin.isEnabled();
    }

    public boolean isPacketEvents() {
        Plugin plugin = getServer().getPluginManager().getPlugin("PacketEvents");
        return plugin != null && plugin.isEnabled();
    }

    public boolean isItemsAdder() {
        return getServer().getPluginManager().getPlugin("ItemsAdder") != null;
    }

    public boolean isVaultAPI() {
        return getServer().getPluginManager().getPlugin("Vault") != null;
    }

    public boolean isCoinsEngine() {
        return getServer().getPluginManager().getPlugin("CoinsEngine") != null;
    }

    public String getLayout() {
        return layout;
    }

    public VoucherManager getVoucherManager() {
        return voucherManager;
    }

    public static SQLiteDatabase getSQLite() {
        return sqlite;
    }

    public SQLiteUserData getSQLiteUser() {
        return sqLiteUser;
    }

    public boolean isDeactivateClick() {
        return deactivateClick;
    }

    public ItemStack getItemWithNexo(String id) {
        return NexoItems.itemFromId(id).build();
    }

    public ItemStack getHead() {
        return head != null ? head : new ItemStack(Material.DIRT, 1);
    }
}