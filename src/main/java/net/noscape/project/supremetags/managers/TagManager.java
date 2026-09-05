package net.noscape.project.supremetags.managers;

import net.noscape.project.supremetags.*;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.handlers.TagEconomy;
import net.noscape.project.supremetags.handlers.Variant;
import net.noscape.project.supremetags.handlers.requirements.TagRequirement;
import net.noscape.project.supremetags.handlers.requirements.TagRequirements;
import net.noscape.project.supremetags.storage.TagData;
import net.noscape.project.supremetags.storage.UserData;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static net.noscape.project.supremetags.utils.Utils.*;

public class TagManager {

    private Map<String, Tag> tags = new ConcurrentHashMap<>();
    private final Map<Integer, String> dataItem = new ConcurrentHashMap<>();
    public static final Map<String, Integer> tagUnlockCounts = new ConcurrentHashMap<>();

    private final Map<String, FileConfiguration> tagSourceConfig = new ConcurrentHashMap<>();

    private final Map<String, Variant> variantLookup = new ConcurrentHashMap<>();
    private boolean loadingFileTagsForDatabaseMigration;
    private boolean suppressTagFileSync;
    private boolean pendingSuppressedTagFileSync;

    private final FileConfiguration messages = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get();
    private final String invalidtag = msg("messages.invalid-tag");
    private final String validtag = msg("messages.valid-tag");
    private final String invalidcategory = msg("messages.invalid-category");

    public TagManager() {
        if (tags.isEmpty()) {
            validateTags(false);
            loadTags(false);
        }
    }

    public void createTag(String identifier, String tagText, List<String> description, String permission, double cost) {
        createTagInternal(identifier, "NAME_TAG", tagText, description, permission, cost, 0, null, null);
    }

    public void createTag(String identifier, String material, String tagText, List<String> description, String permission, double cost) {
        createTagInternal(identifier, material, tagText, description, permission, cost, 0, null, null);
    }

    public void createTag(String identifier, String material, String tagText, List<String> description, String permission, double cost, int modelData) {
        createTagInternal(identifier, material, tagText, description, permission, cost, modelData, null, null);
    }

    public void createTag(CommandSender sender, String identifier, String tagText, List<String> description, String permission, double cost, String fileLocation) {
        createTagInternal(identifier, "NAME_TAG", tagText, description, permission, cost, 0, sender, fileLocation);
    }

    private void createTagInternal(String identifier, String material, String tagText, List<String> description, String permission, double cost, int modelData, CommandSender sender, String fileLocation) {
        if (tags.containsKey(identifier)) {
            if (sender != null) msgPlayer(sender, validtag);
            return;
        }

        String defaultCategory = SupremeTags.getInstance().getConfig().getString("settings.default-category");
        int orderID = tags.size() + 1;

        TagEconomy economy = new TagEconomy("VAULT", cost, false);
        Tag tag = new Tag(identifier, Collections.singletonList(tagText), defaultCategory, permission, description, orderID, true, "common", new HashMap<>(), economy, new ArrayList<>());
        tags.put(identifier, tag);

        if (!isDBTags()) {

            FileConfiguration writeConfig;
            if (fileLocation != null && !fileLocation.isEmpty()) {
                writeConfig = SupremeTags.getInstance().getConfigManager().getOrCreateTagConfig(fileLocation);
            } else {
                writeConfig = getTagConfigForWrite();
            }
            saveTagToConfig(writeConfig, tag, material, modelData, tagText);
            saveSpecificTagConfig(writeConfig);
            tagSourceConfig.put(identifier, writeConfig);
        } else {
            TagData.createTag(tag);
        }

        if (sender != null) {
            msgPlayer(sender, "\u00268[\u00266\u0026lTAG\u00268] \u00267New tag created \u00266" + identifier + " \u0026f- " + tagText);
        }

        unloadTags();
        loadTags(true);
        syncTagFiles();
    }

    private void saveTagToConfig(FileConfiguration config, Tag tag, String material, int modelData, String tagText) {
        String id = tag.getIdentifier();

        List<String> voucherLore = Arrays.asList(
                "\u00267\u0026m-----------------------------",
                "\u0026eClick to equip!",
                "\u00267\u0026m-----------------------------"
        );

        config.set("tags." + id + ".tag", tag.getTag());
        config.set("tags." + id + ".permission", tag.getPermission());
        config.set("tags." + id + ".description", tag.getDescription());
        config.set("tags." + id + ".category", tag.getCategory());
        config.set("tags." + id + ".order", tag.getOrder());
        config.set("tags." + id + ".withdrawable", tag.isWithdrawable());
        config.set("tags." + id + ".displayname", "\u00267Tag: %tag%");
        config.set("tags." + id + ".custom-model-data", modelData);
        config.set("tags." + id + ".display-item", material);
        config.set("tags." + id + ".name-wrapper.enabled", false);
        config.set("tags." + id + ".name-wrapper.wrapper-only", false);
        config.set("tags." + id + ".name-wrapper.format", "%player_name%");

        tag.setDisplayName("\u00267Tag: %tag%");
        tag.setCustomModelData(modelData);
        tag.setDisplayItem(material);
        tag.setNameWrapperEnabled(false);
        tag.setNameWrapperOnly(false);
        tag.setNameWrapperFormat("%player_name%");
        tag.setVoucherDisplayName(tagText + " \u0026f\u0026lVoucher");
        tag.setVoucherMaterial("NAME_TAG");
        tag.setVoucherLore(voucherLore);
        tag.setVoucherCustomModelData(0);
        tag.setVoucherGlow(true);

        config.set("tags." + id + ".voucher-item.material", "NAME_TAG");
        config.set("tags." + id + ".voucher-item.displayname", tagText + " \u0026f\u0026lVoucher");
        config.set("tags." + id + ".voucher-item.custom-model-data", 0);
        config.set("tags." + id + ".voucher-item.glow", true);
        config.set("tags." + id + ".voucher-item.lore", voucherLore);
        config.set("tags." + id + ".rarity", "common");
        config.set("tags." + id + ".economy.enabled", tag.getEconomy().isEnabled());
        config.set("tags." + id + ".economy.type", tag.getEconomy().getType());
        config.set("tags." + id + ".economy.amount", tag.getEconomy().getAmount());
    }

    public void deleteTag(CommandSender sender, String identifier) {
        if (!tags.containsKey(identifier)) {
            msgPlayer(sender, invalidtag);
            return;
        }

        deleteTag(identifier, true);
        String deleted = messages.getString("messages.editor.deleted").replace("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
        msgPlayer(sender, deleted);
    }

    public boolean deleteTag(String identifier, boolean reloadTagConfigs) {
        if (!tags.containsKey(identifier)) {
            return false;
        }

        tags.remove(identifier);

        if (isDBTags()) {
            TagData.deleteTag(identifier);
        } else {
            FileConfiguration sourceConfig = tagSourceConfig.remove(identifier);
            if (sourceConfig != null) {
                sourceConfig.set("tags." + identifier, null);
                saveSpecificTagConfig(sourceConfig);
                if (reloadTagConfigs) {
                    reloadTagConfig();
                }
            } else {

                for (FileConfiguration cfg : SupremeTags.getInstance().getConfigManager().getTagConfigs()) {
                    if (cfg.isConfigurationSection("tags." + identifier)) {
                        cfg.set("tags." + identifier, null);
                        saveSpecificTagConfig(cfg);
                        if (reloadTagConfigs) {
                            reloadTagConfig();
                        }
                        break;
                    }
                }
            }
        }
        syncTagFiles();
        return true;
    }

    public boolean moveTag(CommandSender sender, String identifier, String targetFileLocation) {
        if (isDBTags()) {
            msgPlayer(sender, msg("messages.move-tags-database"));
            return false;
        }

        if (!tags.containsKey(identifier)) {
            msgPlayer(sender, invalidtag);
            return false;
        }

        if (targetFileLocation == null || targetFileLocation.isBlank()) {
            msgPlayer(sender, msg("messages.usage.move"));
            return false;
        }

        String normalizedTarget = targetFileLocation.replace('\\', '/');
        if (!normalizedTarget.toLowerCase(Locale.ROOT).endsWith(".yml")) {
            normalizedTarget += ".yml";
        }

        if (normalizedTarget.startsWith("/") || normalizedTarget.contains(":") || normalizedTarget.contains("..")) {
            msgPlayer(sender, msg("messages.move-tags-invalid-target"));
            return false;
        }

        FileConfiguration sourceConfig = getConfigForTag(identifier);
        if (sourceConfig == null || !sourceConfig.isConfigurationSection("tags." + identifier)) {
            msgPlayer(sender, invalidtag);
            return false;
        }

        FileConfiguration targetConfig = SupremeTags.getInstance().getConfigManager().getOrCreateTagConfig(normalizedTarget);
        if (targetConfig == null) {
            msgPlayer(sender, msg("messages.move-tags-target-load-failed")
                    .replace("%file%", normalizedTarget));
            return false;
        }

        if (targetConfig == sourceConfig) {
            msgPlayer(sender, msg("messages.move-tags-already-stored")
                    .replace("%file%", normalizedTarget));
            return false;
        }

        if (targetConfig.isConfigurationSection("tags." + identifier)) {
            msgPlayer(sender, msg("messages.move-tags-duplicate-target")
                    .replace("%identifier%", identifier));
            return false;
        }

        ConfigurationSection sourceSection = sourceConfig.getConfigurationSection("tags." + identifier);
        if (sourceSection == null) {
            msgPlayer(sender, invalidtag);
            return false;
        }

        targetConfig.set("tags." + identifier, null);
        for (Map.Entry<String, Object> entry : sourceSection.getValues(true).entrySet()) {
            targetConfig.set("tags." + identifier + "." + entry.getKey(), entry.getValue());
        }
        sourceConfig.set("tags." + identifier, null);

        saveSpecificTagConfig(targetConfig);
        saveSpecificTagConfig(sourceConfig);

        reloadTagConfig();
        unloadTags();
        loadTags(true);
        SupremeTags.getInstance().getCategoryManager().initCategories();
        syncTagFiles();

        msgPlayer(sender, msg("messages.move-tags-success")
                .replace("%identifier%", identifier)
                .replace("%file%", normalizedTarget));
        return true;
    }

    public void loadTags(boolean silent) {
        if (isDBTags() && !loadingFileTagsForDatabaseMigration) {
            tags.clear();
            tagSourceConfig.clear();
            tags.putAll(TagData.getAllTags());

            if (tags.isEmpty()) {
                loadingFileTagsForDatabaseMigration = true;
                try {
                    loadTags(true);
                    int migrated = tags.size();
                    for (Tag tag : tags.values()) {
                        TagData.createTag(tag);
                    }
                    tags.clear();
                    tags.putAll(TagData.getAllTags());
                    if (migrated > 0) {
                        Bukkit.getConsoleSender().sendMessage("[TAGS] Migrated " + migrated + " file tag(s) into the database because db-only-tags was enabled and the database was empty.");
                    }
                } finally {
                    loadingFileTagsForDatabaseMigration = false;
                }
            }

            variantLookup.clear();
            for (Tag tag : tags.values()) {
                for (Variant variant : tag.getVariants()) {
                    variantLookup.put(variant.getIdentifier().toLowerCase(), variant);
                }
            }
            for (Tag tag : tags.values()) {
                if (tag.getTag().size() > 1) tag.startAnimation();
            }
            for (Variant variant : getVariants()) {
                if (variant.getTag().size() > 1) variant.startAnimation();
            }
            if (!silent) Bukkit.getConsoleSender().sendMessage("[TAGS] Loaded " + tags.size() + " tag(s) from database.");
            return;
        }

        Map<String, Tag> loadedTags = new LinkedHashMap<>();
        tagSourceConfig.clear();
        int count = 0;

        List<FileConfiguration> allTagConfigs = SupremeTags.getInstance().getConfigManager().getTagConfigs();

        for (FileConfiguration tagConfig : allTagConfigs) {
            ConfigurationSection tagsSection = tagConfig.getConfigurationSection("tags");
            if (tagsSection == null) continue;

            for (String identifier : tagsSection.getKeys(false)) {
                if (loadedTags.containsKey(identifier)) {
                    Bukkit.getConsoleSender().sendMessage("[TAGS] Warning: duplicate tag identifier '" + identifier + "' found in a secondary file - skipping.");
                    continue;
                }

                ConfigurationSection section = tagsSection.getConfigurationSection(identifier);
                if (section == null) continue;

                List<String> tag = normalizeList(tagConfig, "tags." + identifier + ".tag");
                List<String> description = normalizeList(tagConfig, "tags." + identifier + ".description");
                String category = section.getString("category");

                Map<PotionEffectType, Integer> effects = parseEffects(tagConfig.getStringList("tags." + identifier + ".effects"));
                List<Variant> variants = new ArrayList<>();
                String rarity = section.getString("rarity", "common");

                ConfigurationSection variantSection = section.getConfigurationSection("variants");
                if (variantSection != null) {
                    for (String var : variantSection.getKeys(false)) {
                        if (variantSection.getBoolean(var + ".enable") || variantSection.getBoolean(var + ".enabled")) {
                            String permission = variantSection.getString(var + ".permission");
                            List<String> variantTag = tagConfig.getStringList("tags." + identifier + ".variants." + var + ".tag");
                            List<String> variantDescription = tagConfig.getStringList("tags." + identifier + ".variants." + var + ".description");
                            if (variantDescription.isEmpty() || !tagConfig.isSet("tags." + identifier + ".variants." + var + ".description")) {
                                variantDescription = description;
                            }

                            String unlocked_material = tagConfig.getString("tags." + identifier + ".variants." + var + ".item.unlocked.material", "NAME_TAG");
                            int unlocked_custom_model_data = tagConfig.getInt("tags." + identifier + ".variants." + var + ".item.unlocked.custom-model-data", 0);
                            String unlocked_displayname = tagConfig.getString("tags." + identifier + ".variants." + var + ".item.unlocked.displayname", "\u00267Variant: %tag%");

                            String locked_material = tagConfig.getString("tags." + identifier + ".variants." + var + ".item.locked.material", "NAME_TAG");
                            int locked_custom_model_data = tagConfig.getInt("tags." + identifier + ".variants." + var + ".item.locked.custom-model-data", 0);
                            String locked_displayname = tagConfig.getString("tags." + identifier + ".variants." + var + ".item.locked.displayname", "\u00267Variant: %tag%");

                            String rarityVariant = tagConfig.getString("tags." + identifier + ".variants." + var + ".rarity", rarity);

                            Variant v = new Variant(var, identifier, variantTag, permission, variantDescription, rarityVariant);
                            v.setUnlocked_material(unlocked_material);
                            v.setUnlocked_custom_model_data(unlocked_custom_model_data);
                            v.setUnlocked_displayname(unlocked_displayname);

                            v.setLocked_material(locked_material);
                            v.setLocked_custom_model_data(locked_custom_model_data);
                            v.setLocked_displayname(locked_displayname);

                            variants.add(v);
                        }
                    }
                }

                String permission = tagConfig.getString("tags." + identifier + ".permission", "none");
                int orderID = tagConfig.getInt("tags." + identifier + ".order");
                boolean withdrawable = tagConfig.getBoolean("tags." + identifier + ".withdrawable");
                String displayName = tagConfig.getString("tags." + identifier + ".displayname", "&7Tag: %tag%");
                String displayItem = tagConfig.getString("tags." + identifier + ".display-item", "NAME_TAG");
                int customModelData = tagConfig.getInt("tags." + identifier + ".custom-model-data", 0);
                boolean nameWrapperEnabled = tagConfig.getBoolean("tags." + identifier + ".name-wrapper.enabled", false);
                boolean nameWrapperOnly = tagConfig.getBoolean("tags." + identifier + ".name-wrapper.wrapper-only", false);
                String nameWrapperFormat = tagConfig.getString("tags." + identifier + ".name-wrapper.format", "%player_name%");
                String voucherDisplayName = tagConfig.getString("tags." + identifier + ".voucher-item.displayname", "%tag% &f&lVoucher");
                String voucherMaterial = tagConfig.getString("tags." + identifier + ".voucher-item.material", "NAME_TAG");
                List<String> voucherLore = tagConfig.getStringList("tags." + identifier + ".voucher-item.lore");
                int voucherCustomModelData = tagConfig.getInt("tags." + identifier + ".voucher-item.custom-model-data", 0);
                boolean voucherGlow = tagConfig.getBoolean("tags." + identifier + ".voucher-item.glow", true);

                String ecoType = tagConfig.getString("tags." + identifier + ".economy.type", "VAULT");
                double ecoAmount = tagConfig.getDouble("tags." + identifier + ".economy.amount", 0.0D);
                boolean ecoEnabled = false;
                if (tagConfig.isSet("tags." + identifier + ".economy.enable")) {
                    ecoEnabled = tagConfig.getBoolean("tags." + identifier + ".economy.enable");
                } else if (tagConfig.isSet("tags." + identifier + ".economy.enabled")) {
                    ecoEnabled = tagConfig.getBoolean("tags." + identifier + ".economy.enabled");
                }

                String take_cmd = tagConfig.getString("tags." + identifier + ".economy.take-cmd");
                String condition = tagConfig.getString("tags." + identifier + ".economy.condition");

                List<String> abilities = tagConfig.getStringList("tags." + identifier + ".abilities");
                List<String> groups = tagConfig.getStringList("tags." + identifier + ".groups");
                TagRequirements requirements = parseRequirements(section.getConfigurationSection("requirements"));

                TagEconomy economy = new TagEconomy(ecoType, ecoAmount, ecoEnabled);
                if (ecoType != null && ecoType.equalsIgnoreCase("CUSTOM")) {
                    economy.setTake_cmd(take_cmd);
                    economy.setCondition(condition);
                }

                Tag t = new Tag(identifier, tag, category, permission, description, orderID, withdrawable, rarity, effects, economy, variants, groups);

                t.setEcoEnabled(ecoEnabled);
                t.setEcoType(ecoType);
                t.setEcoAmount(ecoAmount);

                t.setVariants(variants);
                t.setAbilities(abilities);
                t.setRequirements(requirements);
                t.setDisplayName(displayName);
                t.setDisplayItem(displayItem);
                t.setCustomModelData(customModelData);
                t.setNameWrapperEnabled(nameWrapperEnabled);
                t.setNameWrapperOnly(nameWrapperOnly);
                t.setNameWrapperFormat(nameWrapperFormat);
                t.setVoucherDisplayName(voucherDisplayName);
                t.setVoucherMaterial(voucherMaterial);
                t.setVoucherLore(voucherLore);
                t.setVoucherCustomModelData(voucherCustomModelData);
                t.setVoucherGlow(voucherGlow);
                t.setCustomPlaceholders(readCustomPlaceholders(section.getConfigurationSection("custom-placeholders")));

                loadedTags.put(identifier, t);
                tagSourceConfig.put(identifier, tagConfig);
                count++;
            }
        }

        tags.clear();
        tags.putAll(loadedTags);

        variantLookup.clear();
        for (Tag tag : tags.values()) {
            for (Variant v : tag.getVariants()) {
                variantLookup.put(v.getIdentifier().toLowerCase(), v);
            }
        }

        for (Tag tag : tags.values()) {
            if (tag.getTag().size() > 1) tag.startAnimation();
        }

        for (Variant v : getVariants()) {
            if (v.getTag().size() > 1) v.startAnimation();
        }

        if (!silent) Bukkit.getConsoleSender().sendMessage("[TAGS] Loaded " + count + " tag(s) successfully from " + allTagConfigs.size() + " file(s).");
    }

    public void refreshDatabaseTags(boolean logChanges) {
        if (!isDBTags()) {
            return;
        }

        Map<String, Tag> loaded = TagData.getAllTags();
        Set<String> before = new HashSet<>(tags.keySet());
        Set<String> after = new HashSet<>(loaded.keySet());

        tags.clear();
        tags.putAll(loaded);
        tagSourceConfig.clear();

        variantLookup.clear();
        for (Tag tag : tags.values()) {
            for (Variant variant : tag.getVariants()) {
                variantLookup.put(variant.getIdentifier().toLowerCase(), variant);
            }
        }

        for (Tag tag : tags.values()) {
            if (tag.getTag().size() > 1) tag.startAnimation();
        }
        for (Variant variant : getVariants()) {
            if (variant.getTag().size() > 1) variant.startAnimation();
        }

        if (logChanges && !before.equals(after)) {
            Bukkit.getConsoleSender().sendMessage("[TAGS] Refreshed database tags. Loaded " + tags.size() + " tag(s).");
        }
    }

    public void validateTags(boolean from_tags_list) {
        if (from_tags_list) {
            for (Tag tag : tags.values()) {
                String basePath = "tags." + tag.getIdentifier();
                FileConfiguration cfg = getConfigForTag(tag.getIdentifier());

                if (!cfg.isSet(basePath + ".tag")) {
                    cfg.set(basePath + ".tag", tag.getTag());
                }

                if (!cfg.isSet(basePath + ".custom-placeholders")) {
                    cfg.set(basePath + ".custom-placeholders.nopermission", "&cYou do not have any permission to use " + tag.getTag());
                    cfg.set(basePath + ".custom-placeholders.wheretofind", "&eYou find this tag in &b&lDiamond Crate&e!");
                }

                String permission = tag.getPermission() != null ? tag.getPermission() : "supremetags.tag." + tag.getIdentifier();
                if (!cfg.isSet(basePath + ".permission")) {
                    cfg.set(basePath + ".permission", permission);
                }

                if (!cfg.isSet(basePath + ".custom-model-data")) {
                    cfg.set(basePath + ".custom-model-data", 0);
                }

                if (!cfg.isSet(basePath + ".name-wrapper.enabled")) {
                    cfg.set(basePath + ".name-wrapper.enabled", false);
                }

                if (!cfg.isSet(basePath + ".name-wrapper.format")) {
                    cfg.set(basePath + ".name-wrapper.format", "%player_name%");
                }

                if (!cfg.isSet(basePath + ".description")) {
                    cfg.set(basePath + ".description", tag.getDescription());
                }

                String category = tag.getCategory() != null ? tag.getCategory() : SupremeTags.getInstance().getConfig().getString("settings.default-category");
                if (!cfg.isSet(basePath + ".category")) {
                    cfg.set(basePath + ".category", category);
                }

                if (!cfg.isSet(basePath + ".order")) {
                    cfg.set(basePath + ".order", tag.getOrder());
                }

                if (!cfg.isSet(basePath + ".withdrawable")) {
                    cfg.set(basePath + ".withdrawable", tag.isWithdrawable());
                }

                if (!cfg.isSet(basePath + ".economy")) {
                    cfg.set(basePath + ".economy.enabled", tag.getEconomy().isEnabled());
                    cfg.set(basePath + ".economy.type", tag.getEconomy().getType());
                    cfg.set(basePath + ".economy.amount", tag.getEconomy().getAmount());
                }

                if (!cfg.isSet(basePath + ".rarity")) {
                    cfg.set(basePath + ".rarity", "common");
                }
            }

            for (FileConfiguration cfg : new HashSet<>(tagSourceConfig.values())) {
                saveSpecificTagConfig(cfg);
            }
        } else {
            for (FileConfiguration tagConfig : SupremeTags.getInstance().getConfigManager().getTagConfigs()) {
                ConfigurationSection section = tagConfig.getConfigurationSection("tags");
                if (section == null) continue;

                for (String identifier : section.getKeys(false)) {
                    String basePath = "tags." + identifier;

                    if (!tagConfig.isSet(basePath + ".tag")) {
                        tagConfig.set(basePath + ".tag", "\u00268[\u0026e\u0026l" + identifier.toUpperCase() + "\u00268]");
                    }
                    if (!tagConfig.isSet(basePath + ".permission")) {
                        tagConfig.set(basePath + ".permission", "supremetags.tag." + identifier);
                    }
                    if (!tagConfig.isSet(basePath + ".custom-model-data")) {
                        tagConfig.set(basePath + ".custom-model-data", 0);
                    }
                    if (!tagConfig.isSet(basePath + ".description")) {
                        List<String> description = new ArrayList<>();
                        description.add(identifier + " Tag!");
                        tagConfig.set(basePath + ".description", description);
                    }
                    if (!tagConfig.isSet(basePath + ".category")) {
                        tagConfig.set(basePath + ".category", SupremeTags.getInstance().getConfig().getString("settings.default-category"));
                    }
                    if (!tagConfig.isSet(basePath + ".withdrawable")) {
                        tagConfig.set(basePath + ".withdrawable", true);
                    }
                    if (!tagConfig.isSet(basePath + ".economy")) {
                        tagConfig.set(basePath + ".economy.enabled", false);
                        tagConfig.set(basePath + ".economy.type", "VAULT");
                        tagConfig.set(basePath + ".economy.amount", 200);
                    }
                    if (!tagConfig.isSet(basePath + ".rarity")) {
                        tagConfig.set(basePath + ".rarity", "common");
                    }
                }

                saveSpecificTagConfig(tagConfig);
            }
        }
    }

    public Variant getVariant(String variantIdentifier) {
        if (variantIdentifier == null) return null;
        return variantLookup.get(variantIdentifier.toLowerCase());
    }

    public List<Variant> getVariants() {
        List<Variant> variants = new ArrayList<>();
        for (Tag tag : getTags().values()) variants.addAll(tag.getVariants());
        return variants;
    }

    public boolean isVariant(String variantIdentifier) {
        return getVariant(variantIdentifier) != null;
    }

    public boolean hasVariantTag(OfflinePlayer player) {
        return getVariant(UserData.getActive(player.getUniqueId())) != null;
    }

    public Variant getVariantTag(OfflinePlayer player) {
        return hasVariantTag(player) ? getVariant(UserData.getActive(player.getUniqueId())) : null;
    }

    public Tag getTag(String identifier) {
        if (identifier == null) {
            return null;
        }

        return tags.get(identifier);
    }

    public boolean doesTagExist(String identifier) {
        return getTag(identifier) != null;
    }

    public void unloadTags() {
        tags.clear();
        tagSourceConfig.clear();
        variantLookup.clear();
    }

    public Map<String, Tag> getTags() {
        return tags;
    }

    public Map<Integer, String> getDataItem() {
        return dataItem;
    }

    public void saveTag(Tag tag) {
        if (isDBTags()) {
            TagData.updateTag(tag);
        } else {
            String identifier = tag.getIdentifier();
            FileConfiguration cfg = getConfigForTag(identifier);
            cfg.set("tags." + identifier + ".tag", tag.getTag());
            cfg.set("tags." + identifier + ".permission", tag.getPermission());
            cfg.set("tags." + identifier + ".groups", tag.getGroups());
            cfg.set("tags." + identifier + ".description", tag.getDescription());
            cfg.set("tags." + identifier + ".category", tag.getCategory());
            cfg.set("tags." + identifier + ".order", tag.getOrder());
            cfg.set("tags." + identifier + ".rarity", tag.getRarity());
            cfg.set("tags." + identifier + ".displayname", tag.getDisplayName());
            cfg.set("tags." + identifier + ".display-item", tag.getDisplayItem());
            cfg.set("tags." + identifier + ".custom-model-data", tag.getCustomModelData());
            cfg.set("tags." + identifier + ".name-wrapper.enabled", tag.isNameWrapperEnabled());
            cfg.set("tags." + identifier + ".name-wrapper.wrapper-only", tag.isNameWrapperOnly());
            cfg.set("tags." + identifier + ".name-wrapper.format", tag.getNameWrapperFormat());
            cfg.set("tags." + identifier + ".voucher-item.material", tag.getVoucherMaterial());
            cfg.set("tags." + identifier + ".voucher-item.displayname", tag.getVoucherDisplayName());
            cfg.set("tags." + identifier + ".voucher-item.lore", tag.getVoucherLore());
            cfg.set("tags." + identifier + ".voucher-item.custom-model-data", tag.getVoucherCustomModelData());
            cfg.set("tags." + identifier + ".voucher-item.glow", tag.isVoucherGlow());
            cfg.set("tags." + identifier + ".custom-placeholders", tag.getCustomPlaceholders());
            cfg.set("tags." + identifier + ".effects", serializeEffects(tag));
            saveVariantsToConfig(cfg, tag);
            cfg.set("tags." + identifier + ".economy.enabled", tag.getEconomy().isEnabled());
            cfg.set("tags." + identifier + ".economy.type", tag.getEconomy().getType());
            cfg.set("tags." + identifier + ".economy.amount", tag.getEconomy().getAmount());
            if (tag.getEconomy().getType().equalsIgnoreCase("CUSTOM")) {
                cfg.set("tags." + identifier + ".economy.take-cmd", tag.getEconomy().getTake_cmd());
                cfg.set("tags." + identifier + ".economy.condition", tag.getEconomy().getCondition());
            }
            cfg.set("tags." + identifier + ".withdrawable", tag.isWithdrawable());
            saveRequirementsToConfig(cfg, tag);
            saveSpecificTagConfig(cfg);
        }
        syncTagFiles();
    }

    public void beginTagFileSyncBatch() {
        suppressTagFileSync = true;
        pendingSuppressedTagFileSync = false;
    }

    public void endTagFileSyncBatch(boolean flush) {
        suppressTagFileSync = false;
        boolean shouldFlush = flush && pendingSuppressedTagFileSync;
        pendingSuppressedTagFileSync = false;
        if (shouldFlush) {
            syncTagFiles();
        }
    }

    private List<String> serializeEffects(Tag tag) {
        List<String> serialized = new ArrayList<>();
        for (Map.Entry<PotionEffectType, Integer> entry : tag.getEffects().entrySet()) {
            serialized.add(entry.getKey().getKey().getKey().toUpperCase(Locale.ROOT) + ":" + entry.getValue());
        }
        return serialized;
    }

    private void saveVariantsToConfig(FileConfiguration cfg, Tag tag) {
        String basePath = "tags." + tag.getIdentifier() + ".variants";
        cfg.set(basePath, null);
        for (Variant variant : tag.getVariants()) {
            String path = basePath + "." + variant.getIdentifier();
            cfg.set(path + ".enabled", true);
            cfg.set(path + ".tag", variant.getTag());
            cfg.set(path + ".permission", variant.getPermission());
            cfg.set(path + ".description", variant.getDescription());
            cfg.set(path + ".rarity", variant.getRarity());
            cfg.set(path + ".item.unlocked.material", variant.getUnlocked_material());
            cfg.set(path + ".item.unlocked.displayname", variant.getUnlocked_displayname());
            cfg.set(path + ".item.unlocked.custom-model-data", variant.getUnlocked_custom_model_data());
            cfg.set(path + ".item.locked.material", variant.getLocked_material());
            cfg.set(path + ".item.locked.displayname", variant.getLocked_displayname());
            cfg.set(path + ".item.locked.custom-model-data", variant.getLocked_custom_model_data());
        }
    }

    private void saveRequirementsToConfig(FileConfiguration cfg, Tag tag) {
        String basePath = "tags." + tag.getIdentifier() + ".requirements";
        TagRequirements requirements = tag.getRequirements();

        if (requirements == null) {
            cfg.set(basePath, null);
            return;
        }

        cfg.set(basePath + ".enabled", requirements.isConfiguredEnabled());
        cfg.set(basePath + ".persist-unlock", requirements.isPersistUnlock());
        cfg.set(basePath + ".mode", requirements.getMode().name().toLowerCase());
        cfg.set(basePath + ".list", null);

        for (TagRequirement requirement : requirements.getRequirements()) {
            String path = basePath + ".list." + requirement.getName();
            cfg.set(path + ".type", requirement.getType());
            cfg.set(path + ".permission", requirement.getPermission());
            cfg.set(path + ".placeholder", requirement.getPlaceholder());
            cfg.set(path + ".operator", requirement.getOperator());
            cfg.set(path + ".value", requirement.getValue());
            cfg.set(path + ".tag", requirement.getTag());
            cfg.set(path + ".economy-type", requirement.getEconomyType());
            cfg.set(path + ".amount", requirement.getAmount());
            cfg.set(path + ".display", requirement.getDisplay());
            cfg.set(path + ".lore-display", requirement.getLoreDisplay());
            cfg.set(path + ".message", requirement.getMessage());
        }
    }

    public void setTag(CommandSender sender, String identifier, String tag) {
        if (tags.containsKey(identifier)) {
            Tag t = tags.get(identifier);
            List<String> tagsList = new ArrayList<>();
            tagsList.add(tag);
            t.setTag(tagsList);

            try {
                FileConfiguration cfg = getConfigForTag(identifier);
                cfg.set("tags." + identifier + ".tag", tagsList);
                saveSpecificTagConfig(cfg);
                reloadTagConfig();
                syncTagFiles();
            } catch (Exception e) {
                e.printStackTrace();
            }

            msgPlayer(sender, "\u00268[\u00266\u0026lTAG\u00268] \u00266" + t.getIdentifier() + "'s tag \u00267changed to " + t.getCurrentTag());
        } else {
            msgPlayer(sender, invalidtag);
        }
    }

    public void setCategory(CommandSender sender, String identifier, String category) {
        if (SupremeTags.getInstance().getTagManager().getTag(identifier) == null) {
            msgPlayer(sender, invalidtag);
            return;
        }

        if (!SupremeTags.getInstance().getCategoryManager().getCatorgies().contains(category)) {
            msgPlayer(sender, invalidcategory);
            return;
        }

        Tag t = tags.get(identifier);
        t.setCategory(category);

        try {
            FileConfiguration cfg = getConfigForTag(identifier);
            cfg.set("tags." + identifier + ".category", t.getCategory());
            saveSpecificTagConfig(cfg);
            syncTagFiles();
        } catch (Exception e) {
            e.printStackTrace();
        }

        msgPlayer(sender, "\u00268[\u00266\u0026lTAG\u00268] \u00266" + t.getIdentifier() + "'s category \u00267changed to " + t.getCategory());
    }

    public static Map<PotionEffectType, Integer> parseEffects(List<String> effectList) {
        Map<PotionEffectType, Integer> effectsMap = new HashMap<>();
        for (String entry : effectList) {
            String[] parts = entry.split(":");
            if (parts.length != 2) continue;
            PotionEffectType type = PotionEffectType.getByName(parts[0].toUpperCase());
            if (type == null) continue;
            try {
                int level = Integer.parseInt(parts[1]);
                effectsMap.put(type, level);
            } catch (NumberFormatException ignored) {
            }
        }
        return effectsMap;
    }

    private List<String> normalizeList(FileConfiguration config, String path) {
        Object val = config.get(path);
        if (val instanceof String) return Collections.singletonList((String) val);
        if (val instanceof List) return config.getStringList(path);
        return new ArrayList<>();
    }

    private Map<String, String> readCustomPlaceholders(ConfigurationSection section) {
        Map<String, String> placeholders = new LinkedHashMap<>();
        if (section == null) {
            return placeholders;
        }

        for (String key : section.getKeys(false)) {
            placeholders.put(key, section.getString(key, ""));
        }

        return placeholders;
    }

    private TagRequirements parseRequirements(ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        boolean enabled = section.getBoolean("enabled", true);
        boolean persistUnlock = section.getBoolean("persist-unlock", false);
        TagRequirements.Mode mode = TagRequirements.Mode.from(section.getString("mode", "all"));
        List<TagRequirement> requirements = new ArrayList<>();

        ConfigurationSection listSection = section.getConfigurationSection("list");
        if (listSection != null) {
            for (String name : listSection.getKeys(false)) {
                if (!listSection.isConfigurationSection(name)) {
                    continue;
                }

                ConfigurationSection requirementSection = listSection.getConfigurationSection(name);
                if (requirementSection == null) {
                    continue;
                }

                requirements.add(parseRequirement(name, requirementSection));
            }
        } else if (section.isList("list")) {
            List<Map<?, ?>> list = section.getMapList("list");
            for (int i = 0; i < list.size(); i++) {
                Map<?, ?> requirementMap = list.get(i);
                Object configuredName = requirementMap.get("name");
                String name = configuredName == null ? "requirement-" + (i + 1) : String.valueOf(configuredName);
                requirements.add(parseRequirement(name, requirementMap));
            }
        } else if (section.isSet("type")) {
            requirements.add(parseRequirement("default", section));
        }

        return new TagRequirements(enabled, persistUnlock, mode, requirements);
    }

    private TagRequirement parseRequirement(String name, ConfigurationSection section) {
        Object rawValue = section.get("value");
        String value = rawValue == null ? section.getString("equals", "") : String.valueOf(rawValue);

        return new TagRequirement(
                name,
                section.getString("type", "placeholder"),
                section.getString("permission"),
                section.getString("placeholder"),
                section.getString("operator", "=="),
                value,
                section.getString("tag"),
                section.getString("economy-type", section.getString("economy", "VAULT")),
                section.getDouble("amount", 0.0D),
                section.getString("display"),
                section.getString("lore-display"),
                section.getString("message")
        );
    }

    private TagRequirement parseRequirement(String name, Map<?, ?> map) {
        Object rawValue = map.containsKey("value") ? map.get("value") : map.get("equals");

        return new TagRequirement(
                name,
                getString(map, "type", "placeholder"),
                getString(map, "permission", null),
                getString(map, "placeholder", null),
                getString(map, "operator", "=="),
                rawValue == null ? "" : String.valueOf(rawValue),
                getString(map, "tag", null),
                getString(map, "economy-type", getString(map, "economy", "VAULT")),
                getDouble(map, "amount", 0.0D),
                getString(map, "display", null),
                getString(map, "lore-display", null),
                getString(map, "message", null)
        );
    }

    private String getString(Map<?, ?> map, String key, String fallback) {
        Object value = map.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private double getDouble(Map<?, ?> map, String key, double fallback) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }

        if (value != null) {
            try {
                return Double.parseDouble(String.valueOf(value));
            } catch (NumberFormatException ignored) {
            }
        }

        return fallback;
    }

    private String msg(String path) {
        return Objects.requireNonNull(messages.getString(path))
                .replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
    }

    public FileConfiguration getConfigForTag(String identifier) {
        if (isDBTags()) {
            Tag tag = getTag(identifier);
            if (tag != null) {
                return createConfigForDatabaseTag(tag);
            }
        }

        FileConfiguration src = tagSourceConfig.get(identifier);
        if (src != null) return src;
        for (FileConfiguration cfg : SupremeTags.getInstance().getConfigManager().getTagConfigs()) {
            if (cfg.isConfigurationSection("tags." + identifier)) return cfg;
        }
        return getTagConfigForWrite();
    }

    private FileConfiguration createConfigForDatabaseTag(Tag tag) {
        YamlConfiguration config = new YamlConfiguration();
        String basePath = "tags." + tag.getIdentifier();

        config.set(basePath + ".tag", tag.getTag());
        config.set(basePath + ".permission", tag.getPermission());
        config.set(basePath + ".description", tag.getDescription());
        config.set(basePath + ".category", tag.getCategory());
        config.set(basePath + ".order", tag.getOrder());
        config.set(basePath + ".withdrawable", tag.isWithdrawable());
        config.set(basePath + ".rarity", tag.getRarity());
        config.set(basePath + ".displayname", tag.getDisplayName());
        config.set(basePath + ".display-item", tag.getDisplayItem());
        config.set(basePath + ".custom-model-data", tag.getCustomModelData());
        config.set(basePath + ".name-wrapper.enabled", tag.isNameWrapperEnabled());
        config.set(basePath + ".name-wrapper.wrapper-only", tag.isNameWrapperOnly());
        config.set(basePath + ".name-wrapper.format", tag.getNameWrapperFormat());
        config.set(basePath + ".custom-placeholders", tag.getCustomPlaceholders());
        config.set(basePath + ".voucher-item.material", tag.getVoucherMaterial());
        config.set(basePath + ".voucher-item.displayname", tag.getVoucherDisplayName());
        config.set(basePath + ".voucher-item.lore", tag.getVoucherLore());
        config.set(basePath + ".voucher-item.custom-model-data", tag.getVoucherCustomModelData());
        config.set(basePath + ".voucher-item.glow", tag.isVoucherGlow());
        config.set(basePath + ".economy.enabled", tag.getEconomy().isEnabled());
        config.set(basePath + ".economy.type", tag.getEconomy().getType());
        config.set(basePath + ".economy.amount", tag.getEconomy().getAmount());
        config.set(basePath + ".economy.take-cmd", tag.getEconomy().getTake_cmd());
        config.set(basePath + ".economy.condition", tag.getEconomy().getCondition());
        config.set(basePath + ".groups", tag.getGroups());
        config.set(basePath + ".abilities", tag.getAbilities());

        saveRequirementsToConfig(config, tag);

        for (Variant variant : tag.getVariants()) {
            String variantPath = basePath + ".variants." + variant.getIdentifier();
            config.set(variantPath + ".enabled", true);
            config.set(variantPath + ".tag", variant.getTag());
            config.set(variantPath + ".permission", variant.getPermission());
            config.set(variantPath + ".description", variant.getDescription());
            config.set(variantPath + ".rarity", variant.getRarity());
            config.set(variantPath + ".item.unlocked.material", variant.getUnlocked_material());
            config.set(variantPath + ".item.unlocked.custom-model-data", variant.getUnlocked_custom_model_data());
            config.set(variantPath + ".item.unlocked.displayname", variant.getUnlocked_displayname());
            config.set(variantPath + ".item.locked.material", variant.getLocked_material());
            config.set(variantPath + ".item.locked.custom-model-data", variant.getLocked_custom_model_data());
            config.set(variantPath + ".item.locked.displayname", variant.getLocked_displayname());
        }

        return config;
    }

    public FileConfiguration getTagConfigForWrite() {
        return SupremeTags.getInstance().getConfigManager().getTagConfigForWrite();
    }

    public FileConfiguration getTagConfig() {
        return getTagConfigForWrite();
    }

    public void saveSpecificTagConfig(FileConfiguration cfg) {
        SupremeTags.getInstance().getConfigManager().saveTagConfig((YamlConfiguration) cfg);
    }

    public void reloadTagConfig() {
        SupremeTags.getInstance().getConfigManager().reloadTagConfigs();
    }

    public boolean tagExists(String name) {
        return getTag(name) != null;
    }

    public boolean tagExistsNearName(String name) {
        Pattern pattern = Pattern.compile(Pattern.quote(name), Pattern.CASE_INSENSITIVE);
        return tags.values().stream().map(Tag::getIdentifier).anyMatch(id -> pattern.matcher(id).find());
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public boolean isDBTags() {
        return SupremeTags.getInstance().isDBTags();
    }

    private void syncTagFiles() {
        if (suppressTagFileSync) {
            pendingSuppressedTagFileSync = true;
            return;
        }

        FileSyncingManager manager = SupremeTags.getInstance().getFileSyncingManager();
        if (manager != null) {
            manager.syncTagFiles();
        }
    }
}
