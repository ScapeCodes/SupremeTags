package net.noscape.project.supremetags.managers;

import net.noscape.project.supremetags.*;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.handlers.TagEconomy;
import net.noscape.project.supremetags.handlers.Variant;
import net.noscape.project.supremetags.storage.UserData;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static net.noscape.project.supremetags.utils.Utils.*;

public class TagManager {

    private final Map<String, Tag> tags = new HashMap<>();
    private final Map<Integer, String> dataItem = new HashMap<>();
    public static final Map<String, Integer> tagUnlockCounts = new ConcurrentHashMap<>();

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

        saveTagToConfig(tag, material, modelData, tagText);
        saveTagConfig();

        if (sender != null) {
            msgPlayer(sender, "&8[&6&lTAG&8] &7New tag created &6" + identifier + " &f- " + tagText);
        }

        unloadTags();
        loadTags(true);
    }

    private void saveTagToConfig(Tag tag, String material, int modelData, String tagText) {
        String id = tag.getIdentifier();

        List<String> voucherLore = Arrays.asList(
                "&7&m-----------------------------",
                "&eClick to equip!",
                "&7&m-----------------------------"
        );

        getTagConfig().set("tags." + id + ".tag", tag.getTag());
        getTagConfig().set("tags." + id + ".permission", tag.getPermission());
        getTagConfig().set("tags." + id + ".description", tag.getDescription());
        getTagConfig().set("tags." + id + ".category", tag.getCategory());
        getTagConfig().set("tags." + id + ".order", tag.getOrder());
        getTagConfig().set("tags." + id + ".withdrawable", tag.isWithdrawable());
        getTagConfig().set("tags." + id + ".displayname", "&7Tag: %tag%");
        getTagConfig().set("tags." + id + ".custom-model-data", modelData);
        getTagConfig().set("tags." + id + ".display-item", material);
        getTagConfig().set("tags." + id + ".voucher-item.material", "NAME_TAG");
        getTagConfig().set("tags." + id + ".voucher-item.displayname", tagText + " &f&lVoucher");
        getTagConfig().set("tags." + id + ".voucher-item.custom-model-data", 0);
        getTagConfig().set("tags." + id + ".voucher-item.glow", true);
        getTagConfig().set("tags." + id + ".voucher-item.lore", voucherLore);
        getTagConfig().set("tags." + id + ".rarity", "common");
        getTagConfig().set("tags." + id + ".economy.enabled", tag.getEconomy().isEnabled());
        getTagConfig().set("tags." + id + ".economy.type", tag.getEconomy().getType());
        getTagConfig().set("tags." + id + ".economy.amount", tag.getEconomy().getAmount());
    }

    /* ---------------------- DELETE TAGS ---------------------- */

    public void deleteTag(CommandSender sender, String identifier) {
        if (!tags.containsKey(identifier)) {
            msgPlayer(sender, invalidtag);
            return;
        }

        tags.remove(identifier);
        getTagConfig().set("tags." + identifier, null);
        saveTagConfig();
        reloadTagConfig();

        msgPlayer(sender, "&8[&6&lTAG&8] &7Tag &6" + identifier + " &7is now deleted!");
    }

    /* ---------------------- LOAD & VALIDATE ---------------------- */

    public void loadTags(boolean silent) {
        FileConfiguration tagConfig = getTagConfig();
        ConfigurationSection tagsSection = tagConfig.getConfigurationSection("tags");

        if (tagsSection == null) {
            if (!silent) Bukkit.getConsoleSender().sendMessage("[TAGS] No tags found in configuration.");
            return;
        }

        Map<String, Tag> loadedTags = new LinkedHashMap<>();
        int count = 0;

        for (String identifier : tagsSection.getKeys(false)) {
            ConfigurationSection section = tagsSection.getConfigurationSection(identifier);
            if (section == null) continue;

            List<String> tag = normalizeList(tagConfig, "tags." + identifier + ".tag");
            List<String> description = normalizeList(tagConfig, "tags." + identifier + ".description");
            String category = section.getString("category");

            Map<PotionEffectType, Integer> effects = parseEffects(tagConfig.getStringList("tags." + identifier + ".effects"));
            List<Variant> variants = new ArrayList<>();
            String rarity = section.getString("rarity");

            ConfigurationSection variantSection = section.getConfigurationSection("variants");
            if (variantSection != null) {
                for (String var : variantSection.getKeys(false)) {
                    if (variantSection.getBoolean(var + ".enable")) {
                        String permission = variantSection.getString(var + ".permission");
                        List<String> variantTag = tagConfig.getStringList("tags." + identifier + ".variants." + var + ".tag");
                        List<String> variantDescription = tagConfig.getStringList("tags." + identifier + ".variants." + var + ".description");
                        if (variantDescription.isEmpty() || !tagConfig.isSet("tags." + identifier + ".variants." + var + ".description")) {
                            variantDescription = description;
                        }

                        String rarityVariant = tagConfig.getString("tags." + identifier + ".variants." + var + ".rarity", rarity);
                        variants.add(new Variant(var, identifier, variantTag, permission, variantDescription, rarityVariant));
                    }
                }
            }

            String permission = tagConfig.getString("tags." + identifier + ".permission", "none");
            int orderID = tagConfig.getInt("tags." + identifier + ".order");
            boolean withdrawable = tagConfig.getBoolean("tags." + identifier + ".withdrawable");

            String ecoType = tagConfig.getString("tags." + identifier + ".economy.type");
            double ecoAmount = tagConfig.getInt("tags." + identifier + ".economy.amount");
            boolean ecoEnabled = tagConfig.getBoolean("tags." + identifier + ".economy.enabled");

            TagEconomy economy = new TagEconomy(ecoType, ecoAmount, ecoEnabled);
            Tag t = new Tag(identifier, tag, category, permission, description, orderID, withdrawable, rarity, effects, economy);
            t.setVariants(variants);

            loadedTags.put(identifier, t);
            count++;
        }

        tags.clear();
        tags.putAll(loadedTags);

        for (Tag tag : tags.values()) {
            if (tag.getTag().size() > 1) tag.startAnimation();
        }

        if (!silent) Bukkit.getConsoleSender().sendMessage("[TAGS] Loaded " + count + " tag(s) successfully.");
    }

    public void validateTags(boolean from_tags_list) {
        if (from_tags_list) {
            for (Tag tag : tags.values()) {
                String basePath = "tags." + tag.getIdentifier();

                // Tag check
                if (!getTagConfig().isSet(basePath + ".tag")) {
                    getTagConfig().set(basePath + ".tag", tag.getTag());
                }

                if (!getTagConfig().isSet(basePath + ".custom-placeholders")) {
                    getTagConfig().set(basePath + ".custom-placeholders.nopermission", "&cYou do not have any permission to use " + tag.getTag());
                    getTagConfig().set(basePath + ".custom-placeholders.wheretofind", "&eYou find this tag in &b&lDiamond Crate&e!");
                }

                // Permission check
                String permission = tag.getPermission() != null ? tag.getPermission() : "supremetags.tag." + tag.getIdentifier();
                if (!getTagConfig().isSet(basePath + ".permission")) {
                    getTagConfig().set(basePath + ".permission", permission);
                }

                // Custom Model Data check
                if (!getTagConfig().isSet(basePath + ".custom-model-data")) {
                    getTagConfig().set(basePath + ".custom-model-data", 0);
                }

                // Description check
                if (!getTagConfig().isSet(basePath + ".description")) {
                    getTagConfig().set(basePath + ".description", tag.getDescription());
                }

                // Category check
                String category = tag.getCategory() != null ? tag.getCategory() : SupremeTags.getInstance().getConfig().getString("settings.default-category");
                if (!getTagConfig().isSet(basePath + ".category")) {
                    getTagConfig().set(basePath + ".category", category);
                }

                // Order check
                if (!getTagConfig().isSet(basePath + ".order")) {
                    getTagConfig().set(basePath + ".order", tag.getOrder());
                }

                // Withdrawable check
                if (!getTagConfig().isSet(basePath + ".withdrawable")) {
                    getTagConfig().set(basePath + ".withdrawable", tag.isWithdrawable());
                }

                // Economy check
                if (!getTagConfig().isSet(basePath + ".economy")) {
                    getTagConfig().set(basePath + ".economy.enabled", tag.getEconomy().isEnabled());
                    getTagConfig().set(basePath + ".economy.type", tag.getEconomy().getType());
                    getTagConfig().set(basePath + ".economy.amount", tag.getEconomy().getAmount());
                }

                if (!getTagConfig().isSet(basePath + ".rarity")) {
                    getTagConfig().set(basePath + ".rarity", "common");
                }
            }

            saveTagConfig();
        } else {
            for (String identifier : getTagConfig().getConfigurationSection("tags").getKeys(false)) {
                String basePath = "tags." + identifier;

                // Tag check
                if (!getTagConfig().isSet(basePath + ".tag")) {
                    getTagConfig().set(basePath + ".tag", "&8[&e&l" + identifier.toUpperCase() + "&8]");
                }

                // Permission check
                if (!getTagConfig().isSet(basePath + ".permission")) {
                    getTagConfig().set(basePath + ".permission", "supremetags.tag." + identifier);
                }

                // Custom Model Data check
                if (!getTagConfig().isSet(basePath + ".custom-model-data")) {
                    getTagConfig().set(basePath + ".custom-model-data", 0);
                }

                // Description check
                List<String> description = new ArrayList<>();
                description.add(identifier + " Tag!");
                if (!getTagConfig().isSet(basePath + ".description")) {
                    getTagConfig().set(basePath + ".description", description);
                }

                // Category check
                if (!getTagConfig().isSet(basePath + ".category")) {
                    getTagConfig().set(basePath + ".category", SupremeTags.getInstance().getConfig().getString("settings.default-category"));
                }

                // Withdrawable check
                if (!getTagConfig().isSet(basePath + ".withdrawable")) {
                    getTagConfig().set(basePath + ".withdrawable", true);
                }

                if (!getTagConfig().isSet(basePath + ".economy")) {
                    getTagConfig().set(basePath + ".economy.enabled", false);
                    getTagConfig().set(basePath + ".economy.type", "VAULT");
                    getTagConfig().set(basePath + ".economy.amount", 200);
                }

                if (!getTagConfig().isSet(basePath + ".rarity")) {
                    getTagConfig().set(basePath + ".rarity", "common");
                }
            }

            saveTagConfig();
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
    }

    public Map<String, Tag> getTags() {
        return tags;
    }

    public Map<Integer, String> getDataItem() {
        return dataItem;
    }

    public void saveTag(Tag tag) {
        String identifier = tag.getIdentifier();
        getTagConfig().set("tags." + identifier + ".tag", tag.getTag());
        getTagConfig().set("tags." + identifier + ".permission", tag.getPermission());
        getTagConfig().set("tags." + identifier + ".description", tag.getDescription());
        getTagConfig().set("tags." + identifier + ".category", tag.getCategory());
        getTagConfig().set("tags." + identifier + ".economy.amount", tag.getEconomy().getAmount());
        getTagConfig().set("tags." + identifier + ".withdrawable", tag.isWithdrawable());
        saveTagConfig();
    }

    public void setTag(CommandSender sender, String identifier, String tag) {
        if (tags.containsKey(identifier)) {
            Tag t = tags.get(identifier);
            List<String> tagsList = t.getTag();
            tagsList.add(tag);
            t.setTag(tagsList);

            try {
                getTagConfig().set("tags." + identifier + ".tag", tagsList);
                saveTagConfig();
                reloadTagConfig();
            } catch (Exception e) {
                e.printStackTrace();
            }

            msgPlayer(sender, "&8[&6&lTAG&8] &6" + t.getIdentifier() + "'s tag &7changed to " + t.getCurrentTag());
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
            SupremeTags.getInstance().getTagManager().getTagConfig().set("tags." + identifier + ".category", t.getCategory());
            SupremeTags.getInstance().getTagManager().saveTagConfig();
        } catch (Exception e) {
            e.printStackTrace();
        }

        msgPlayer(sender, "&8[&6&lTAG&8] &6" + t.getIdentifier() + "'s category &7changed to " + t.getCategory());
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

    public void saveTagConfig() {
        SupremeTags.getInstance().getConfigManager().saveConfig("tags.yml");
    }

    public FileConfiguration getTagConfig() {
        return SupremeTags.getInstance().getConfigManager().getConfig("tags.yml").get();
    }

    public void reloadTagConfig() {
        SupremeTags.getInstance().getConfigManager().reloadConfig("tags.yml");
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
}