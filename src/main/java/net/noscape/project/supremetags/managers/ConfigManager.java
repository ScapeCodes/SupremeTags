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

    private final List<YamlConfiguration> tagConfigs = new ArrayList<>();
    private final List<File> tagFiles = new ArrayList<>();
    private File tagsFolder;
    private File countriesTagsFile;
    private YamlConfiguration customTagsConfig;

    public ConfigManager(JavaPlugin plugin, boolean firstInstall) {
        this.plugin = plugin;

        loadConfig("statistics.yml");
        loadConfig("rarities.yml");
        loadConfig("messages.yml");
        loadConfig("banned-words.yml");
        loadConfig("categories.yml");
        loadConfig("data.yml");
        loadConfig("guis.yml");

        loadTagsFolder();
    }

    public void loadTagsFolder() {
        tagsFolder = new File(plugin.getDataFolder(), "tags");
        if (!tagsFolder.exists()) {
            tagsFolder.mkdirs();
        }

        saveBundledTagResourceIfMissing("tags/default.yml");
        saveBundledTagResourceIfMissing("tags/countries.yml");

        countriesTagsFile = new File(tagsFolder, "countries.yml");

        reloadTagConfigs();
    }

    private void saveBundledTagResource(String resourcePath) {
        try {
            plugin.saveResource(resourcePath, false);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[SupremeTags] Could not save default " + resourcePath + ": " + e.getMessage());
        }
    }

    private void saveBundledTagResourceIfMissing(String resourcePath) {
        File file = new File(plugin.getDataFolder(), resourcePath);
        if (file.exists()) {
            return;
        }

        saveBundledTagResource(resourcePath);
    }

    public void reloadTagConfigs() {
        tagConfigs.clear();
        tagFiles.clear();

        List<File> ymlFiles = getAllYamlFiles(tagsFolder);

        for (File file : ymlFiles) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            tagConfigs.add(cfg);
            tagFiles.add(file);
        }

        customTagsConfig = getOrCreateCustomTagsConfig();
    }

    private List<File> getAllYamlFiles(File folder) {
        List<File> ymlFiles = new ArrayList<>();
        if (folder == null || !folder.exists()) return ymlFiles;

        File[] files = folder.listFiles();
        if (files == null) return ymlFiles;

        for (File file : files) {
            if (file.isDirectory()) {

                ymlFiles.addAll(getAllYamlFiles(file));
            } else if (file.getName().endsWith(".yml")) {
                ymlFiles.add(file);
            }
        }

        return ymlFiles;
    }

    public List<String> getTagFilePaths() {
        List<String> paths = new ArrayList<>();
        for (File file : getAllYamlFiles(tagsFolder)) {
            String relativePath = tagsFolder.toPath().relativize(file.toPath()).toString().replace(File.separatorChar, '/');
            paths.add(relativePath);
        }
        return paths;
    }

    public List<FileConfiguration> getTagConfigs() {
        return new ArrayList<>(tagConfigs);
    }

    public File getFileForTagConfig(YamlConfiguration cfg) {
        int idx = tagConfigs.indexOf(cfg);
        return (idx >= 0 && idx < tagFiles.size()) ? tagFiles.get(idx) : null;
    }

    public void saveTagConfig(FileConfiguration cfg) {
        YamlConfiguration yamlCfg = (YamlConfiguration) cfg;
        File file = getFileForTagConfig(yamlCfg);
        if (file == null) {

            file = countriesTagsFile;
        }
        try {
            yamlCfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("[SupremeTags] Could not save tag config: " + e.getMessage());
        }
    }

    private YamlConfiguration getOrCreateCustomTagsConfig() {
        if (customTagsConfig == null) {
            customTagsConfig = YamlConfiguration.loadConfiguration(countriesTagsFile);
        }
        return customTagsConfig;
    }

    public YamlConfiguration getTagConfigForWrite() {
        return getOrCreateCustomTagsConfig();
    }

    public YamlConfiguration getOrCreateTagConfig(String fileName) {

        for (int i = 0; i < tagFiles.size(); i++) {
            if (tagFiles.get(i).getName().equals(fileName)) {
                return tagConfigs.get(i);
            }
        }

        File file = new File(tagsFolder, fileName);
        if (!file.exists()) {
            try {
                File parentDir = file.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("[SupremeTags] Could not create tag file: " + fileName + " - " + e.getMessage());
                return getTagConfigForWrite();
            }
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        tagConfigs.add(cfg);
        tagFiles.add(file);
        return cfg;
    }

    public void saveCustomTagsConfig() {
        try {
            customTagsConfig.save(countriesTagsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[SupremeTags] Could not save countries.yml: " + e.getMessage());
        }
    }

    public Config getConfig(String name) {
        return configs.computeIfAbsent(name, Config::new);
    }

    public List<String> getLoadedConfigNames() {
        return new ArrayList<>(configs.keySet());
    }

    public void saveConfig(String name) {
        getConfig(name).save();
    }

    private void loadConfig(String name) {
        Config config = getConfig(name);
        config.saveDefaultConfig();
        config.reload();
    }

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

        public YamlConfiguration get() {
            if (config == null) {
                reload();
            }
            return config;
        }

        public Config saveDefaultConfig() {
            this.file = new File(plugin.getDataFolder(), this.name);
            if (!file.exists()) {
                plugin.saveResource(this.name, false);
            }
            return this;
        }

        public void reload() {
            this.file = new File(plugin.getDataFolder(), this.name);
            this.config = YamlConfiguration.loadConfiguration(file);

            InputStream defConfigStream = plugin.getResource(name);
            if (defConfigStream != null) {
                try (Reader reader = new InputStreamReader(defConfigStream, "UTF8")) {
                    YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(reader);
                    config.setDefaults(defConfig);
                    config.options().copyDefaults(false);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public Config set(String key, Object value) {
            get().set(key, value);
            save();
            return this;
        }

        public Object get(String key) {
            return get().get(key);
        }
    }
}
