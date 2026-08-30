package net.noscape.project.supremetags.managers;

import net.noscape.project.supremetags.SupremeTags;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static net.noscape.project.supremetags.utils.Utils.toMiniMessage;

public class ColorMigrationManager {

    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile(
            "(?i)(?:[&\u00A7][0-9A-FK-OR])|(?:[&\u00A7]x(?:[&\u00A7][0-9A-F]){6})|(?:[&\u00A7]#[0-9A-F]{6})"
    );

    private final SupremeTags plugin;
    private final ConfigManager configManager;

    public ColorMigrationManager(SupremeTags plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void migrate() {
        int changedValues = 0;

        for (String configName : configManager.getLoadedConfigNames()) {
            FileConfiguration config = configManager.getConfig(configName).get();
            int changed = migrateSection(config);
            if (changed > 0) {
                configManager.saveConfig(configName);
                configManager.reloadConfig(configName);
                changedValues += changed;
            }
        }

        for (FileConfiguration tagConfig : configManager.getTagConfigs()) {
            int changed = migrateSection(tagConfig);
            if (changed > 0) {
                configManager.saveTagConfig(tagConfig);
                changedValues += changed;
            }
        }

        if (changedValues > 0) {
            plugin.getLogger().info("[SupremeTags] Converted " + changedValues + " legacy color value(s) to MiniMessage.");
            configManager.reloadTagConfigs();
        }
    }

    public boolean needsMigration() {
        for (String configName : configManager.getLoadedConfigNames()) {
            if (sectionContainsLegacy(configManager.getConfig(configName).get())) {
                return true;
            }
        }

        for (FileConfiguration tagConfig : configManager.getTagConfigs()) {
            if (sectionContainsLegacy(tagConfig)) {
                return true;
            }
        }

        return false;
    }

    private int migrateSection(ConfigurationSection section) {
        int changed = 0;

        for (String key : section.getKeys(true)) {
            Object value = section.get(key);

            if (value instanceof String stringValue && containsLegacyColor(stringValue)) {
                section.set(key, toMiniMessage(stringValue));
                changed++;
            } else if (value instanceof List<?> listValue) {
                List<Object> migrated = new ArrayList<>();
                boolean listChanged = false;

                for (Object entry : listValue) {
                    if (entry instanceof String stringEntry && containsLegacyColor(stringEntry)) {
                        migrated.add(toMiniMessage(stringEntry));
                        listChanged = true;
                    } else {
                        migrated.add(entry);
                    }
                }

                if (listChanged) {
                    section.set(key, migrated);
                    changed++;
                }
            }
        }

        return changed;
    }

    private boolean sectionContainsLegacy(ConfigurationSection section) {
        for (String key : section.getKeys(true)) {
            Object value = section.get(key);

            if (value instanceof String stringValue && containsLegacyColor(stringValue)) {
                return true;
            }

            if (value instanceof List<?> listValue) {
                for (Object entry : listValue) {
                    if (entry instanceof String stringEntry && containsLegacyColor(stringEntry)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean containsLegacyColor(String value) {
        return value != null && LEGACY_COLOR_PATTERN.matcher(value).find();
    }
}
