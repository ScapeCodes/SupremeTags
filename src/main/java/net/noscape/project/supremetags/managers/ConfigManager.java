package net.noscape.project.supremetags.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ConfigManager {

    private final JavaPlugin plugin;
    private HashMap<String, Config> configs = new HashMap<>();

    // --- Multi-file tag support ---
    private final List<YamlConfiguration> tagConfigs = new ArrayList<>();
    private final List<File> tagFiles = new ArrayList<>();
    private File tagsFolder;
    private File customTagsFile;
    private YamlConfiguration customTagsConfig;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;

        // Load each config, ensuring defaults are only copied if the file does not exist
        loadConfig("rarities.yml");
        loadConfig("messages.yml");
        loadConfig("banned-words.yml");
        loadConfig("categories.yml");
        loadConfig("data.yml");
        loadConfig("guis.yml");

        // Load the tags/ folder (replaces the old single tags.yml)
        loadTagsFolder();
    }

    // -----------------------------------------------------------------------
    // Tags folder management
    // -----------------------------------------------------------------------

    /**
     * Initialises the tags/ folder, migrates any legacy tags.yml, and loads
     * all .yml files found inside the folder.
     */
    public void loadTagsFolder() {
        tagsFolder = new File(plugin.getDataFolder(), "tags");
        if (!tagsFolder.exists()) {
            tagsFolder.mkdirs();
        }

        // --- Migration: move old tags.yml into tags/default.yml ---
        File legacyTagsFile = new File(plugin.getDataFolder(), "tags.yml");
        File defaultTagsFile = new File(tagsFolder, "default.yml");

        if (legacyTagsFile.exists() && !defaultTagsFile.exists()) {
            boolean moved = legacyTagsFile.renameTo(defaultTagsFile);
            if (moved) {
                plugin.getLogger().info("[SupremeTags] Migrated tags.yml -> tags/default.yml");
            } else {
                plugin.getLogger().warning("[SupremeTags] Could not migrate tags.yml to tags/default.yml. Please move it manually.");
            }
        }

        // --- If the folder is still empty, copy the bundled default ---
        File[] existingFiles = tagsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (existingFiles == null || existingFiles.length == 0) {
            // Save the default resource (tags/default.yml inside the JAR)
            try {
                plugin.saveResource("tags/default.yml", false);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[SupremeTags] Could not save default tags/default.yml: " + e.getMessage());
            }
        }

        // --- Prepare the custom-tags.yml file used for runtime-created tags ---
        customTagsFile = new File(tagsFolder, "custom-tags.yml");
        if (!customTagsFile.exists()) {
            try {
                customTagsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("[SupremeTags] Could not create tags/custom-tags.yml: " + e.getMessage());
            }
        }

        // Load all yml files in the folder
        reloadTagConfigs();
    }

    /**
     * Rescans the tags/ folder and reloads every .yml file in it,
     * including subdirectories.
     */
    public void reloadTagConfigs() {
        tagConfigs.clear();
        tagFiles.clear();

        List<File> ymlFiles = getAllYamlFiles(tagsFolder);

        for (File file : ymlFiles) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            tagConfigs.add(cfg);
            tagFiles.add(file);
        }

        // Refresh the custom-tags reference
        customTagsConfig = getOrCreateCustomTagsConfig();
    }

    /**
     * Recursively finds all .yml files in a directory and its subdirectories.
     */
    private List<File> getAllYamlFiles(File folder) {
        List<File> ymlFiles = new ArrayList<>();
        if (folder == null || !folder.exists()) return ymlFiles;

        File[] files = folder.listFiles();
        if (files == null) return ymlFiles;

        for (File file : files) {
            if (file.isDirectory()) {
                // Recursively search subdirectories
                ymlFiles.addAll(getAllYamlFiles(file));
            } else if (file.getName().endsWith(".yml")) {
                ymlFiles.add(file);
            }
        }

        return ymlFiles;
    }

    /**
     * Returns all FileConfiguration objects loaded from the tags/ folder.
     */
    public List<FileConfiguration> getTagConfigs() {
        return new ArrayList<>(tagConfigs);
    }

    /**
     * Returns the matching File for a given YamlConfiguration loaded from the
     * tags/ folder, or null if not found.
     */
    public File getFileForTagConfig(YamlConfiguration cfg) {
        int idx = tagConfigs.indexOf(cfg);
        return (idx >= 0 && idx < tagFiles.size()) ? tagFiles.get(idx) : null;
    }

    /**
     * Saves a specific tag config file to its original location.
     */
    public void saveTagConfig(FileConfiguration cfg) {
        YamlConfiguration yamlCfg = (YamlConfiguration) cfg;
        File file = getFileForTagConfig(yamlCfg);
        if (file == null) {
            // Fallback to custom-tags.yml if not found
            file = customTagsFile;
        }
        try {
            yamlCfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("[SupremeTags] Could not save tag config: " + e.getMessage());
        }
    }

    /**
     * Gets or creates the custom-tags.yml configuration.
     */
    private YamlConfiguration getOrCreateCustomTagsConfig() {
        if (customTagsConfig == null) {
            customTagsConfig = YamlConfiguration.loadConfiguration(customTagsFile);
        }
        return customTagsConfig;
    }

    /**
     * Returns the config for writing new tags (custom-tags.yml).
     */
    public YamlConfiguration getTagConfigForWrite() {
        return getOrCreateCustomTagsConfig();
    }

    /**
     * Saves the custom-tags.yml file.
     */
    public void saveCustomTagsConfig() {
        try {
            customTagsConfig.save(customTagsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[SupremeTags] Could not save custom-tags.yml: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Standard config management
    // -----------------------------------------------------------------------

    /**
     * Get the config by the name (Don't forget the .yml)
     *
     * @param name the name of the config file
     * @return the Config object
     */
    public Config getConfig(String name) {
        return configs.computeIfAbsent(name, Config::new);
    }

    /**
     * Save the config by the name (Don't forget the .yml)
     *
     * @param name the name of the config file
     */
    public void saveConfig(String name) {
        getConfig(name).save();
    }

    /**
     * Load the config, ensuring defaults are copied if the file does not exist
     *
     * @param name the name of the config file
     */
    private void loadConfig(String name) {
        Config config = getConfig(name);
        config.saveDefaultConfig(); // Only saves the default config if the file does not exist
        config.reload(); // Reload to ensure the config is properly loaded
    }

    /**
     * Reload the config by the name (Don't forget the .yml)
     *
     * @param name the name of the config file
     */
    public void reloadConfig(String name) {
        Config config = configs.get(name);
        if (config == null) {
            System.err.println("Config not found: " + name);
            return;
        }
        config.reload();
    }

    public class Config {

        private final String name;
        private File file;
        private YamlConfiguration config;

        public Config(String name) {
            this.name = name;
        }

        /**
         * Saves the config to file
         *
         * @return this Config object
         */
        public Config save() {
            if (config == null || file == null) {
                return this;
            }
            try {
                config.save(file);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return this;
        }

        /**
         * Gets the YamlConfiguration instance of this config, loading from file if necessary
         *
         * @return YamlConfiguration instance
         */
        public YamlConfiguration get() {
            if (config == null) {
                reload();
            }
            return config;
        }

        /**
         * Saves the default config if it doesn't exist
         *
         * @return this Config object
         */
        public Config saveDefaultConfig() {
            this.file = new File(plugin.getDataFolder(), this.name);
            if (!file.exists()) {
                plugin.saveResource(this.name, false);
            }
            return this;
        }

        /**
         * Reloads the config from file
         */
        public void reload() {
            this.file = new File(plugin.getDataFolder(), this.name);
            this.config = YamlConfiguration.loadConfiguration(file);

            // Load defaults from resources if the config file doesn't exist
            try (Reader defConfigStream = new InputStreamReader(plugin.getResource(name), "UTF8")) {
                if (defConfigStream != null) {
                    YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
                    config.setDefaults(defConfig);
                    config.options().copyDefaults(false); // Do not overwrite existing values
                }
            } catch (IOException | NullPointerException e) {
                e.printStackTrace();
            }
        }

        /**
         * An easy way to set a value into the config
         *
         * @param key   the key
         * @param value the value
         * @return this Config object
         */
        public Config set(String key, Object value) {
            get().set(key, value);
            save(); // Save changes immediately
            return this;
        }

        /**
         * An easy way to get a value from the config
         *
         * @param key the key
         * @return the value associated with the key
         */
        public Object get(String key) {
            return get().get(key);
        }
    }
}