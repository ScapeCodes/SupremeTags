package net.noscape.project.supremetags.managers;

import net.noscape.project.supremetags.*;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.handlers.TagEconomy;
import net.noscape.project.supremetags.handlers.Variant;
import net.noscape.project.supremetags.storage.TagData;
import net.noscape.project.supremetags.storage.UserData;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static net.noscape.project.supremetags.utils.Utils.*;

public class TagManager {

    private Map<String, Tag> tags = new HashMap<>();
    private final Map<Integer, String> dataItem = new HashMap<>();
    public static final Map<String, Integer> tagUnlockCounts = new ConcurrentHashMap<>();

    /**
     * Tracks which FileConfiguration each tag was loaded from.
     * Used so that saves and deletes go back to the correct file.
     */
    private final Map<String, FileConfiguration> tagSourceConfig = new HashMap<>();

    private final FileConfiguration messages = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get();
    private final String invalidtag = msg("messages.invalid-tag");
    private final String validtag = msg("messages.valid-tag");
    private final String invalidcategory = msg("messages.invalid-category");

    public TagManager() {}

    /* ---------------------- CREATE TAGS ---------------------- */

    public void createTag(CommandSender sender, String identifier, String tagText, List<String> description, String permission, double cost) {
        createTagInternal(identifier, "NAME_TAG", tagText, description, permission, cost, 0, sender);
    }

    public void createTag(String identifier, String tagText, List<String> description, String permission, double cost) {
        createTagInternal(identifier, "NAME_TAG", tagText, description, permission, cost, 0, null);
    }

    public void createTag(String identifier, String material, String tagText, List<String> description, String permission, double cost) {
        createTagInternal(identifier, material, tagText, description, permission, cost, 0, null);
    }

    public void createTag(String identifier, String material, String tagText, List<String> description, String permission, double cost, int modelData) {
        createTagInternal(identifier, material, tagText, description, permission, cost, modelData, null);
    }

    private void createTagInternal(String identifier, String material, String tagText, List<String> description, String permission, double cost, int modelData, CommandSender sender) {
        if (tags.containsKey(identifier)) {
            if (sender != null) msgPlayer(sender, validtag);
            return;
        }

        String defaultCategory = SupremeTags.getInstance().getConfig().getString("settings.default-category");
        int orderID = tags.size() + 1;

        TagEconomy economy = new TagEconomy("VAULT", cost, false);
        Tag tag = new Tag(identifier, Collections.singletonList(tagText), defaultCategory, permission, description, orderID, true, "common", new HashMap<>(), economy);
        tags.put(identifier, tag);

        if (!isDBTags()) {
            // New tags always go to the custom-tags.yml write target
            FileConfiguration writeConfig = getTagConfigForWrite();
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

    /* ---------------------- DELETE TAGS ---------------------- */

    public void deleteTag(CommandSender sender, String identifier) {
        if (!tags.containsKey(identifier)) {
            msgPlayer(sender, invalidtag);
            return;
        }

        tags.remove(identifier);

        if (isDBTags()) {
            TagData.deleteTag(identifier);
        } else {
            // Find which config holds this tag and remove it from there
            FileConfiguration sourceConfig = tagSourceConfig.remove(identifier);
            if (sourceConfig != null) {
                sourceConfig.set("tags." + identifier, null);
                saveSpecificTagConfig(sourceConfig);
                reloadTagConfig();
            } else {
                // Fallback: search all configs
                for (FileConfiguration cfg : SupremeTags.getInstance().getConfigManager().getTagConfigs()) {
                    if (cfg.isConfigurationSection("tags." + identifier)) {
                        cfg.set("tags." + identifier, null);
                        saveSpecificTagConfig(cfg);
                        reloadTagConfig();
                        break;
                    }
                }
            }
        }
        String deleted = messages.getString("messages.editor.deleted").replace("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
        msgPlayer(sender, deleted);
    }

    /* ---------------------- LOAD & VALIDATE ---------------------- */

    public void loadTags(boolean silent) {
        if (isDBTags()) {
            tags.clear();
            tagSourceConfig.clear();
            TagData.getAllTags();
            if (!silent) Bukkit.getConsoleSender().sendMessage("[TAGS] Loaded " + tags.size() + " tag(s) from database.");
            return;
        }

        Map<String, Tag> loadedTags = new LinkedHashMap<>();
        tagSourceConfig.clear();
        int count = 0;

        // Iterate over ALL tag config files
        List<FileConfiguration> allTagConfigs = SupremeTags.getInstance().getConfigManager().getTagConfigs();

        for (FileConfiguration tagConfig : allTagConfigs) {
            ConfigurationSection tagsSection = tagConfig.getConfigurationSection("tags");
            if (tagsSection == null) continue;

            for (String identifier : tagsSection.getKeys(false)) {
                // If a tag with this identifier was already loaded from a previous file, skip it
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

                String ecoType = tagConfig.getString("tags." + identifier + ".economy.type");
                double ecoAmount = tagConfig.getInt("tags." + identifier + ".economy.amount");
                boolean ecoEnabled = false;
                if (tagConfig.isSet("tags." + identifier + ".economy.enable")) {
                    ecoEnabled = tagConfig.getBoolean("tags." + identifier + ".economy.enable");
                } else if (tagConfig.isSet("tags." + identifier + ".economy.enabled")) {
                    ecoEnabled = tagConfig.getBoolean("tags." + identifier + ".economy.enabled");
                }

                String take_cmd = tagConfig.getString("tags." + identifier + ".economy.take-cmd");
                String condition = tagConfig.getString("tags." + identifier + ".economy.condition");

                List<String> abilities = tagConfig.getStringList("tags." + identifier + ".abilities");

                TagEconomy economy = new TagEconomy(ecoType, ecoAmount, ecoEnabled);
                if (ecoType != null && ecoType.equalsIgnoreCase("CUSTOM")) {
                    economy.setTake_cmd(take_cmd);
                    economy.setCondition(condition);
                }

                Tag t = new Tag(identifier, tag, category, permission, description, orderID, withdrawable, rarity, effects, economy, variants);

                t.setEcoEnabled(ecoEnabled);
                t.setEcoType(ecoType);
                t.setEcoAmount(ecoAmount);

                t.setVariants(variants);
                t.setAbilities(abilities);

                loadedTags.put(identifier, t);
                tagSourceConfig.put(identifier, tagConfig); // track the source file
                count++;
            }
        }

        tags.clear();
        tags.putAll(loadedTags);

        for (Tag tag : tags.values()) {
            if (tag.getTag().size() > 1) tag.startAnimation();
        }

        for (Variant v : getVariants()) {
            if (v.getTag().size() > 1) v.startAnimation();
        }

        if (!silent) Bukkit.getConsoleSender().sendMessage("[TAGS] Loaded " + count + " tag(s) successfully from " + allTagConfigs.size() + " file(s).");
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

            // Save all configs that were modified
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

                // Save this config after validating all tags in it
                saveSpecificTagConfig(tagConfig);
            }
        }
    }

    /* ---------------------- GETTERS & UTIL ---------------------- */

    public Variant getVariant(String variantIdentifier) {
        for (Tag tag : getTags().values()) {
            for (Variant var : tag.getVariants()) {
                if (var.getIdentifier().equalsIgnoreCase(variantIdentifier)) return var;
            }
        }
        return null;
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
        return tags.get(identifier);
    }

    public boolean doesTagExist(String identifier) {
        return getTag(identifier) != null;
    }

    public void unloadTags() {
        tags.clear();
        tagSourceConfig.clear();
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
            cfg.set("tags." + identifier + ".description", tag.getDescription());
            cfg.set("tags." + identifier + ".category", tag.getCategory());
            cfg.set("tags." + identifier + ".economy.amount", tag.getEconomy().getAmount());
            cfg.set("tags." + identifier + ".withdrawable", tag.isWithdrawable());
            saveSpecificTagConfig(cfg);
        }
    }

    public void setTag(CommandSender sender, String identifier, String tag) {
        if (tags.containsKey(identifier)) {
            Tag t = tags.get(identifier);
            List<String> tagsList = t.getTag();
            tagsList.add(tag);
            t.setTag(tagsList);

            try {
                FileConfiguration cfg = getConfigForTag(identifier);
                cfg.set("tags." + identifier + ".tag", tagsList);
                saveSpecificTagConfig(cfg);
                reloadTagConfig();
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

    private String msg(String path) {
        return Objects.requireNonNull(messages.getString(path))
                .replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
    }

    // -----------------------------------------------------------------------
    // Config access helpers
    // -----------------------------------------------------------------------

    /**
     * Returns the FileConfiguration that contains the given tag identifier.
     * Falls back to the write config (custom-tags.yml) if not found.
     */
    public FileConfiguration getConfigForTag(String identifier) {
        FileConfiguration src = tagSourceConfig.get(identifier);
        if (src != null) return src;
        // Search through all loaded configs
        for (FileConfiguration cfg : SupremeTags.getInstance().getConfigManager().getTagConfigs()) {
            if (cfg.isConfigurationSection("tags." + identifier)) return cfg;
        }
        return getTagConfigForWrite();
    }

    /**
     * Returns the write target: tags/custom-tags.yml.
     * Used when creating new tags at runtime.
     */
    public FileConfiguration getTagConfigForWrite() {
        return SupremeTags.getInstance().getConfigManager().getTagConfigForWrite();
    }

    /**
     * Legacy compatibility: returns the write target config.
     * Code that uses this directly will write to custom-tags.yml.
     */
    public FileConfiguration getTagConfig() {
        return getTagConfigForWrite();
    }

    /**
     * Saves the config file that corresponds to a given FileConfiguration.
     */
    public void saveSpecificTagConfig(FileConfiguration cfg) {
        SupremeTags.getInstance().getConfigManager().saveTagConfig((YamlConfiguration) cfg);
    }

    /**
     * Legacy compatibility shim. Saves the custom-tags.yml.
     */
    public void saveTagConfig() {
        SupremeTags.getInstance().getConfigManager().saveCustomTagsConfig();
    }

    /**
     * Reloads ALL tag config files from the tags/ folder.
     */
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

    public void setTagsMap(Map<String, Tag> tags) {
        this.tags = tags;
    }
}