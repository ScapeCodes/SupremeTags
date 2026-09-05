package net.noscape.project.supremetags;

import com.nexomc.nexo.api.NexoItems;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import net.noscape.project.supremetags.api.SupremeTagsAPI;
import net.noscape.project.supremetags.checkers.Metrics;
import net.noscape.project.supremetags.checkers.UpdateChecker;
import net.noscape.project.supremetags.commands.MyTags;
import net.noscape.project.supremetags.commands.tags.TagsCommand;
import net.noscape.project.supremetags.handlers.Editor;
import net.noscape.project.supremetags.handlers.SetupTag;
import net.noscape.project.supremetags.handlers.hooks.EssentialsChatListener;
import net.noscape.project.supremetags.handlers.hooks.PAPI;
import net.noscape.project.supremetags.handlers.menu.MenuUtil;
import net.noscape.project.supremetags.managers.*;
import net.noscape.project.supremetags.redis.RedisUpdateService;
import net.noscape.project.supremetags.storage.*;
import net.noscape.project.supremetags.storage.tags.MySQLTags;
import net.noscape.project.supremetags.storage.tags.SQLiteTags;
import net.noscape.project.supremetags.storage.user.H2UserData;
import net.noscape.project.supremetags.storage.user.MySQLUserData;
import net.noscape.project.supremetags.storage.user.PlayerConfig;
import net.noscape.project.supremetags.storage.user.SQLiteUserData;
import net.noscape.project.supremetags.utils.BungeeMessaging;
import net.noscape.project.supremetags.utils.ClassRegistrationUtils;
import net.noscape.project.supremetags.utils.commands.BukkitCommand;
import net.noscape.project.supremetags.utils.commands.CommandFramework;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import su.nightexpress.excellenteconomy.api.ExcellentEconomyAPI;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static net.noscape.project.supremetags.utils.Utils.*;

public final class SupremeTags extends JavaPlugin {

    public final boolean dev_build;
    public final int build;

    {
        dev_build = false;
        build = 1;
    }

    private static SupremeTags instance;
    private Metrics metrics;
    private ConfigManager configManager;
    private TagManager tagManager;
    private CategoryManager categoryManager;
    private MergeManager mergeManager;
    private VoucherManager voucherManager;
    private RarityManager rarityManager;
    private TagStatisticsManager tagStatisticsManager;
    private FileSyncingManager fileSyncingManager;
    private AutoApplyManager autoApplyManager;
    private TagEditorSessionManager tagEditorSessionManager;
    private RedisUpdateService redisUpdateService;

    private static SupremeTagsAPI api;

    private static Economy econ = null;
    private static Permission perms = null;

    private static ExcellentEconomyAPI excellentEconomy;

    public static ExcellentEconomyAPI getExcellentEconomy() {
        return excellentEconomy;
    }

    private PlayerPointsAPI ppAPI;

    private static MySQLDatabase mysql;
    private static H2Database h2;
    private static SQLiteDatabase sqlite;
    private final SQLiteUserData sqLiteUser = new SQLiteUserData();
    private final H2UserData h2user = new H2UserData();
    private static String connectionURL;
    private final MySQLUserData user = new MySQLUserData();

    private MySQLTags mySQLTags;
    private SQLiteTags sqLiteTags;

    private static final ConcurrentHashMap<UUID, MenuUtil> menuUtilMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Editor> editorList = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, SetupTag> setupList = new ConcurrentHashMap<>();

    private boolean disabledWorldsTag;
    private boolean deactivateClick;
    private boolean isDBTags;
    private volatile boolean refreshingDatabaseTags;
    private volatile long lastKnownTagDataVersion;

    private PlayerManager playerManager;
    private PlayerConfig playerConfig;

    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
    private boolean useSSL;

    private String layout = getConfig().getString("settings.layout-type", "BORDER");

    private final CommandFramework commandFramework = new CommandFramework(this);

    private DataCache dataCache;

    private ItemStack head;

    private static boolean foliaDetected = false;
    private static boolean foliaChecked = false;
    private static final java.util.Set<Object> foliaScheduledTasks = ConcurrentHashMap.newKeySet();

    @Override
    public void onEnable() {
        init();
    }

    @Override
    public void onDisable() {
        if (metrics != null) {
            metrics.shutdown();
            metrics = null;
        }
        if (redisUpdateService != null) redisUpdateService.shutdown();
        if (autoApplyManager != null) autoApplyManager.restoreAll();
        if (tagStatisticsManager != null) tagStatisticsManager.save();
        if (tagManager != null) tagManager.unloadTags();
        if (fileSyncingManager != null) fileSyncingManager.unregisterChannels();
        editorList.clear();
        setupList.clear();
        if (dataCache != null) dataCache.clearCache();
        if (!isFoliaFound()) {
            this.getServer().getScheduler().cancelTasks(this);
        } else {

            for (Object task : foliaScheduledTasks) {
                if (task instanceof java.util.concurrent.ScheduledFuture) {
                    ((java.util.concurrent.ScheduledFuture<?>) task).cancel(false);
                } else {
                    try {
                        java.lang.reflect.Method cancelMethod = task.getClass().getMethod("cancel");
                        cancelMethod.invoke(task);
                    } catch (Exception e) {
                        SupremeTags.getInstance().getLogger().warning("Failed to cancel Folia task: " + e.getMessage());
                    }
                }
            }
            foliaScheduledTasks.clear();
        }

        if (isProtocolLib()) {
            try {
                Class<?> clazz = Class.forName("net.noscape.project.supremetags.handlers.packets.ProtocolLibHandler");
                Object handler = clazz.getConstructor(Plugin.class).newInstance(this);
                clazz.getMethod("unRegister").invoke(handler);
            } catch (Exception e) {

            }
        }

        if (isPacketEvents()) {
            try {
                Class<?> clazz = Class.forName("net.noscape.project.supremetags.handlers.packets.PacketEventsHandler");
                Object handler = clazz.getConstructor(Plugin.class).newInstance(this);
                clazz.getMethod("unRegister").invoke(handler);
            } catch (Exception e) {

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
        TagsCommand tagsCommand = new TagsCommand();
        PluginCommand declaredCommand = getCommand("tags");

        if (declaredCommand == null) {
            getLogger().severe("Could not find command: tags. Please check your plugin.yml file.");
            return;
        }

        Set<String> configuredLabels = getConfiguredCommandLabels(mainCommand, aliases);
        List<String> configuredAliases = configuredLabels.stream()
                .filter(label -> !label.equalsIgnoreCase(normalizeCommandLabel(mainCommand, "tags")))
                .toList();

        declaredCommand.setExecutor(tagsCommand);
        declaredCommand.setTabCompleter(tagsCommand);
        declaredCommand.setAliases(configuredAliases);

        CommandMap commandMap = Bukkit.getCommandMap();
        unregisterStaleConfiguredCommands(commandMap, declaredCommand, configuredLabels);

        for (String label : configuredLabels) {
            org.bukkit.command.Command existingCommand = commandMap.getCommand(label);
            if (existingCommand == declaredCommand) {
                continue;
            }

            if (existingCommand != null && !(existingCommand instanceof BukkitCommand)) {
                getLogger().warning("Could not register configured command /" + label + " because another plugin already owns it.");
                continue;
            }

            commandMap.register(getName(), new BukkitCommand(label, tagsCommand, this));
        }
    }

    private Set<String> getConfiguredCommandLabels(String mainCommand, List<String> aliases) {
        Set<String> labels = new LinkedHashSet<>();
        labels.add(normalizeCommandLabel(mainCommand, "tags"));

        if (aliases != null) {
            for (String alias : aliases) {
                String normalizedAlias = normalizeCommandLabel(alias, null);
                if (normalizedAlias != null) {
                    labels.add(normalizedAlias);
                }
            }
        }

        return labels;
    }

    private String normalizeCommandLabel(String label, String fallback) {
        if (label == null || label.isBlank()) {
            return fallback;
        }

        String normalized = label.trim().toLowerCase(Locale.ROOT);
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        if (!normalized.matches("[a-z0-9][a-z0-9_-]*")) {
            getLogger().warning("Ignoring invalid command label in settings.commands: " + label);
            return fallback;
        }

        return normalized;
    }

    @SuppressWarnings("unchecked")
    private void unregisterStaleConfiguredCommands(CommandMap commandMap, PluginCommand declaredCommand, Set<String> configuredLabels) {
        try {
            Field knownCommandsField = findKnownCommandsField(commandMap.getClass());
            knownCommandsField.setAccessible(true);
            Map<String, org.bukkit.command.Command> knownCommands = (Map<String, org.bukkit.command.Command>) knownCommandsField.get(commandMap);

            List<String> staleCommandKeys = new ArrayList<>();
            for (Map.Entry<String, org.bukkit.command.Command> entry : knownCommands.entrySet()) {
                String key = entry.getKey().toLowerCase(Locale.ROOT);
                String bareKey = key.replace(getName().toLowerCase(Locale.ROOT) + ":", "");

                org.bukkit.command.Command command = entry.getValue();
                if (command == declaredCommand) {
                    if (!configuredLabels.contains(key) && !configuredLabels.contains(bareKey)) {
                        staleCommandKeys.add(entry.getKey());
                    }
                    continue;
                }

                if (command instanceof BukkitCommand bukkitCommand) {
                    if (bukkitCommand.getOwnerPlugin() == this && bukkitCommand.getExecutor() instanceof TagsCommand) {
                        staleCommandKeys.add(entry.getKey());
                    }
                }
            }

            for (String key : staleCommandKeys) {
                knownCommands.remove(key);
            }
        } catch (ReflectiveOperationException | ClassCastException | UnsupportedOperationException exception) {
            getLogger().warning("Could not clean stale SupremeTags command aliases: " + exception.getMessage());
        }
    }

    private Field findKnownCommandsField(Class<?> commandMapClass) throws NoSuchFieldException {
        Class<?> currentClass = commandMapClass;
        while (currentClass != null) {
            try {
                return currentClass.getDeclaredField("knownCommands");
            } catch (NoSuchFieldException ignored) {
                currentClass = currentClass.getSuperclass();
            }
        }

        throw new NoSuchFieldException("knownCommands");
    }

    private void init() {
        instance = this;

        Logger logger = Bukkit.getLogger();
        boolean firstInstall = !getDataFolder().exists();

        this.getConfig().options().copyDefaults(true);
        this.saveConfig();
        configManager = new ConfigManager(this, firstInstall);
        ColorMigrationManager colorMigrationManager = new ColorMigrationManager(this, configManager);
        if (colorMigrationManager.needsMigration()) {
            getLogger().info("[SupremeTags] Legacy color codes detected. Converting configuration files to MiniMessage...");
            colorMigrationManager.migrate();
            getLogger().info("[SupremeTags] Legacy color conversion complete.");
        }
        isDBTags = getConfig().getBoolean("settings.db-only-tags", false);

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

        tagManager = new TagManager();
        categoryManager = new CategoryManager();
        playerManager = new PlayerManager();
        voucherManager = new VoucherManager();
        mergeManager = new MergeManager(this);
        playerConfig = new PlayerConfig();
        rarityManager = new RarityManager();
        tagStatisticsManager = new TagStatisticsManager(this);
        fileSyncingManager = new FileSyncingManager(this);
        fileSyncingManager.registerChannels();
        autoApplyManager = new AutoApplyManager(this);
        tagEditorSessionManager = new TagEditorSessionManager();
        redisUpdateService = new RedisUpdateService(this);
        redisUpdateService.start();

        if (isDBTags) {
            lastKnownTagDataVersion = TagData.getTagDataVersion();
        }
        startDatabaseTagRefreshTask();

        String mainCommand = getConfig().getString("settings.commands.main-command", "tags");
        List<String> aliases = getConfig().getStringList("settings.commands.aliases");
        registerCommand(mainCommand, aliases);

        getCommand("mytags").setExecutor(new MyTags());

        ClassRegistrationUtils.loadListeners("net.noscape.project.supremetags.listeners", this);

        if (isEssentials()) {
            getLogger().info("> EssentialsX + EssentialsXChat detected! Registering Essentials listeners...");
            getServer().getPluginManager().registerEvents(new EssentialsChatListener(), this);
        } else {
            getLogger().warning("> EssentialsX or EssentialsXChat not found. Skipping Essentials listener registration.");
        }

        disabledWorldsTag = getConfig().getBoolean("settings.tag-command-in-disabled-worlds");
        layout = getConfig().getString("settings.layout-type", "BORDER");
        deactivateClick = getConfig().getBoolean("settings.deactivate-click");

        merge();

        if (isPlaceholderAPI()) {
            logger.info("> PlaceholderAPI: Found");
            new PAPI(this).register();
        } else {
            logger.info("> PlaceholderAPI: Not Found!");
        }

        api = new SupremeTagsAPI();

        if (tagManager.getTags().isEmpty()) {
            tagManager.loadTags(false);
        }

        if (getServer().getPluginManager().getPlugin("Luckperms") != null) {
            calculateUnlockedTagCounts();
            scheduleUnlockCount();
        } else {
            logger.warning("> Luckperms not found! disabling unlocked count function.");
        }

        this.head = new ItemStack(Material.PLAYER_HEAD, 1);

        if (getConfig().getBoolean("settings.bungee-messaging")) {
            BungeeMessaging.registerChannels();
        }

        validateDefaultSounds();
    }

    public static SupremeTags getInstance() {
        return instance;
    }

    public TagManager getTagManager() {
        return tagManager;
    }

    public FileSyncingManager getFileSyncingManager() {
        return fileSyncingManager;
    }

    public AutoApplyManager getAutoApplyManager() {
        return autoApplyManager;
    }

    public TagEditorSessionManager getTagEditorSessionManager() {
        return tagEditorSessionManager;
    }

    public RedisUpdateService getRedisUpdateService() {
        return redisUpdateService;
    }

    public CategoryManager getCategoryManager() {
        return categoryManager;
    }

    public static MenuUtil getMenuUtil(Player player) {
        MenuUtil menuUtil;
        UUID uuid = player.getUniqueId();

        if (menuUtilMap.containsKey(uuid)) {
            menuUtil = menuUtilMap.get(uuid);
            menuUtil.setOwner(player);
            return menuUtil;
        } else {
            menuUtil = new MenuUtil(player, UserData.getActive(uuid));
            menuUtil.setFilter("all");
            menuUtil.setSort("none");
            menuUtilMap.put(uuid, menuUtil);
        }

        return menuUtil;
    }

    public static MenuUtil getMenuUtilIdentifier(Player player, String identifier) {
        MenuUtil menuUtil;
        UUID uuid = player.getUniqueId();

        if (menuUtilMap.containsKey(uuid)) {
            menuUtil = menuUtilMap.get(uuid);
            menuUtil.setOwner(player);
            menuUtil.setIdentifier(identifier);
            return menuUtil;
        } else {
            menuUtil = new MenuUtil(player, identifier);
            menuUtil.setFilter("all");
            menuUtil.setSort("none");
            menuUtilMap.put(uuid, menuUtil);
        }

        return menuUtil;
    }

    public static MenuUtil getMenuUtil(Player player, String category) {
        MenuUtil menuUtil;
        UUID uuid = player.getUniqueId();

        if (menuUtilMap.containsKey(uuid)) {
            menuUtil = menuUtilMap.get(uuid);
            menuUtil.setOwner(player);
            menuUtil.setCategory(category);
            return menuUtil;
        } else {
            menuUtil = new MenuUtil(player, UserData.getActive(uuid), category);
            menuUtil.setFilter("all");
            menuUtil.setSort("none");
            menuUtilMap.put(uuid, menuUtil);
        }

        return menuUtil;
    }

    public ConcurrentHashMap<UUID, MenuUtil> getMenuUtil() {
        return menuUtilMap;
    }

    public static String getConnectionURL() {
        return connectionURL;
    }

    public H2UserData getUserData() {
        return h2user;
    }

    public static H2Database getH2Database() {
        return h2;
    }

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

        super.reloadConfig();

        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();

        configManager.reloadConfig("categories.yml");
        configManager.reloadConfig("banned-words.yml");
        configManager.reloadConfig("data.yml");
        configManager.reloadConfig("guis.yml");
        configManager.reloadConfig("statistics.yml");
        configManager.reloadConfig("messages.yml");
        configManager.reloadConfig("rarities.yml");

        disabledWorldsTag = getConfig().getBoolean("settings.tag-command-in-disabled-worlds");
        layout = getConfig().getString("settings.layout-type", "BORDER");
        deactivateClick = getConfig().getBoolean("settings.deactivate-click");
        isDBTags = getConfig().getBoolean("settings.db-only-tags", false);
        registerCommand(
                getConfig().getString("settings.commands.main-command", "tags"),
                getConfig().getStringList("settings.commands.aliases")
        );

        rarityManager.unloadRarities();
        rarityManager.loadRarities();

        configManager.reloadTagConfigs();
        tagManager.unloadTags();
        tagManager.loadTags(false);

        tagManager.getDataItem().clear();

        categoryManager.initCategories();

        configManager.reloadConfig("messages.yml");
        if (tagStatisticsManager != null) tagStatisticsManager.load();

        if (autoApplyManager != null) autoApplyManager.applyAll();
        if (fileSyncingManager != null) fileSyncingManager.syncTagFiles();
        if (redisUpdateService != null) redisUpdateService.reload();
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

        if (isDBTags) {
            if (isSQLite()) {
                sqLiteTags = new SQLiteTags(sqlite);
            }

            if (isMySQL() || isMaria()) {
                mySQLTags = new MySQLTags(mysql);
            }
        }
    }

    public void merge() {
        if (!getConfig().getBoolean("settings.auto-merge")) {
            return;
        }

        mergeManager.merge(null, true);
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
        if (dev_build) {
            logger.info("You're using a development build of supremetags! build: #" + build);
        }
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
            logger.info("> Vault: Found!");
            if (setupEconomy()) {
                logger.info("> Vault Economy: Found!");
            } else {
                logger.warning("> Vault Economy: No economy provider found. Vault economy support disabled.");
            }
            if (setupPermissions()) {
                logger.info("> Vault Permissions: Found!");
            } else {
                logger.warning("> Vault Permissions: No permission provider found. Vault permission support disabled.");
            }
        } else {
            logger.info("> Vault: Not Found!");
        }

        if (isExcellentEconomy()) {
            RegisteredServiceProvider<ExcellentEconomyAPI> provider = Bukkit.getServer().getServicesManager().getRegistration(ExcellentEconomyAPI.class);
            if (provider != null) {
                excellentEconomy = provider.getProvider();
            }
            logger.info("> ExcellentEconomy: Found!");
        } else {
            logger.info("> ExcellentEconomy: Not Found!");
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

        if (!dev_build) {
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
    }

    public boolean isBungeeCord() {
        return getConfig().getString("settings.messaging-platform").equalsIgnoreCase("bungeecord");
    }

    public boolean isNoPermissionMenuAction() {
        return getConfig().getBoolean("settings.no-permission-menu-action");
    }

    public static boolean isFoliaFound() {
        if (!foliaChecked) {
            try {
                Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                foliaDetected = true;
            } catch (ClassNotFoundException ignored) {
                foliaDetected = false;
            }
            foliaChecked = true;
        }
        return foliaDetected;
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
        int pluginId = 18038;
        metrics = new Metrics(this, pluginId);

        metrics.addCustomChart(new Metrics.SimplePie("used_language", () -> getConfig().getString("language", "en")));
        metrics.addCustomChart(new Metrics.SingleLineChart("loaded_tags", metrics::getLoadedTagCount));
        metrics.addCustomChart(new Metrics.SingleLineChart("max_loaded_tags", metrics::getMaxLoadedTagCount));
        metrics.addCustomChart(new Metrics.SingleLineChart("loaded_variants", metrics::getLoadedVariantCount));
        metrics.addCustomChart(new Metrics.SingleLineChart("loaded_categories", metrics::getLoadedCategoryCount));
        metrics.addCustomChart(new Metrics.SingleLineChart("animated_tags", metrics::getAnimatedTagCount));
        metrics.addCustomChart(new Metrics.SingleLineChart("economy_tags", metrics::getEconomyTagCount));
        metrics.addCustomChart(new Metrics.SimplePie("largest_category", metrics::getLargestCategoryName));

        metrics.addCustomChart(new Metrics.DrilldownPie("java_version", () -> {
            Map<String, Map<String, Integer>> map = new HashMap<>();
            String javaVersion = System.getProperty("java.version");
            Map<String, Integer> entry = new HashMap<>();
            entry.put(javaVersion, 1);
            map.put("Java", entry);
            return map;
        }));
    }

    private boolean setupEconomy() {
        econ = null;
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
        perms = null;
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        if (rsp == null) {
            return false;
        }
        perms = rsp.getProvider();
        return perms != null;
    }

    public static Economy getEconomy() {
        return econ;
    }

    public static Permission getPermissions() {
        return perms;
    }

    public boolean hasVaultEconomy() {
        return econ != null;
    }

    public boolean hasVaultPermissions() {
        return perms != null;
    }

    public static SupremeTagsAPI getTagAPI() {
        return api;
    }

    public DataCache getDataCache() {
        return dataCache;
    }

    public static java.util.Set<Object> getFoliaScheduledTasks() {
        return foliaScheduledTasks;
    }

    public ConcurrentHashMap<UUID, Editor> getEditorList() {
        return editorList;
    }

    public void removeEditor(Player player) {
        editorList.remove(player.getUniqueId());
    }

    public void removeSetup(Player player) {
        setupList.remove(player.getUniqueId());
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

    public TagStatisticsManager getTagStatisticsManager() {
        return tagStatisticsManager;
    }

    public ConcurrentHashMap<UUID, SetupTag> getSetupList() {
        return setupList;
    }

    public boolean isPlaceholderAPI() {
        return getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    public boolean isProtocolLib() {
        return getServer().getPluginManager().getPlugin("ProtocolLib") != null;
    }

    public boolean isPacketEvents() {
        return getServer().getPluginManager().getPlugin("packetevents") != null;
    }

    public boolean isItemsAdder() {
        return getServer().getPluginManager().getPlugin("ItemsAdder") != null;
    }

    public boolean isVaultAPI() {
        return getServer().getPluginManager().getPlugin("Vault") != null;
    }

    public boolean isExcellentEconomy() {
        return getServer().getPluginManager().getPlugin("ExcellentEconomy") != null;
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

    public static boolean isEssentials() {
        return Bukkit.getPluginManager().getPlugin("Essentials") != null
                && Bukkit.getPluginManager().getPlugin("EssentialsChat") != null;
    }

    public boolean isDBTags() {
        return isDBTags;
    }

    private void startDatabaseTagRefreshTask() {
        if (!isDBTags || tagManager == null) {
            return;
        }

        long intervalTicks = Math.max(20L, getConfig().getLong("settings.db-tags-refresh-interval", 5L) * 20L);

        Runnable refresh = () -> {
            if (refreshingDatabaseTags || tagManager == null || !isDBTags) {
                return;
            }

            refreshingDatabaseTags = true;
            runAsync(() -> {
                try {
                    long currentVersion = TagData.getTagDataVersion();
                    if (currentVersion > lastKnownTagDataVersion) {
                        lastKnownTagDataVersion = currentVersion;
                        tagManager.refreshDatabaseTags(false);
                        if (categoryManager != null) {
                            categoryManager.initCategories();
                        }
                    }
                } catch (Exception exception) {
                    getLogger().warning("[SupremeTags] Failed to refresh database tags: " + exception.getMessage());
                } finally {
                    refreshingDatabaseTags = false;
                }
            });
        };

        if (!isFoliaFound()) {
            Bukkit.getScheduler().runTaskTimer(this, refresh, intervalTicks, intervalTicks);
        } else {
            Bukkit.getServer().getGlobalRegionScheduler().runAtFixedRate(this, task -> refresh.run(), intervalTicks, intervalTicks);
        }
    }

    private void validateDefaultSounds() {
        FileConfiguration config = SupremeTags.getInstance().getConfig();

        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("open-menus.enable", true);
        defaults.put("open-menus.sound", "block.note_block.pling");
        defaults.put("open-menus.volume", 1.0);
        defaults.put("open-menus.pitch", 1.0);

        defaults.put("selected-tag.enable", true);
        defaults.put("selected-tag.sound", "entity.player.levelup");
        defaults.put("selected-tag.volume", 1.0);
        defaults.put("selected-tag.pitch", 1.2);

        defaults.put("reset-tag.enable", true);
        defaults.put("reset-tag.sound", "block.anvil.use");
        defaults.put("reset-tag.volume", 0.9);
        defaults.put("reset-tag.pitch", 1.0);

        defaults.put("error-message.enable", true);
        defaults.put("error-message.sound", "entity.villager.no");
        defaults.put("error-message.volume", 1.0);
        defaults.put("error-message.pitch", 0.9);

        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            String key = "sounds." + entry.getKey();
            if (!config.isSet(key)) {
                config.set(key, entry.getValue());
            }
        }

        SupremeTags.getInstance().saveConfig();
    }

    public MySQLTags getMySQLTags() {
        return mySQLTags;
    }

    public SQLiteTags getSqLiteTags() {
        return sqLiteTags;
    }
}
