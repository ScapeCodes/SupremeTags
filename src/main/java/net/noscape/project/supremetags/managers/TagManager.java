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

    private boolean isCost;

    private FileConfiguration messages = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get();

    private final String invalidtag = messages.getString("messages.invalid-tag").replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
    private final String validtag = messages.getString("messages.valid-tag").replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
    private final String invalidcategory = messages.getString("messages.invalid-category").replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));

    public TagManager(boolean isCost) {
        this.isCost = isCost;
    }

    public void createTag(CommandSender player, String identifier, String tag_string, List<String> description, String permission, double cost) {
        if (!tags.containsKey(identifier)) {

            String default_category = SupremeTags.getInstance().getConfig().getString("settings.default-category");

            List<String> tagList = new ArrayList<>();
            tagList.add(tag_string);

            int orderID = tags.size() + 1;

            TagEconomy economy = new TagEconomy("VAULT", 200, false);

            Tag tag = new Tag(identifier, tagList, default_category, permission, description, orderID, false, false, "common", new HashMap<>(), economy);
            tags.put(identifier, tag);

            List<String> voucher_item_lore = new ArrayList<>();
            voucher_item_lore.add("&7&m-----------------------------");
            voucher_item_lore.add("&eClick to equip!");
            voucher_item_lore.add("&7&m-----------------------------");

            List<String> tags = new ArrayList<>();
            tags.add(tag_string);

            getTagConfig().set("tags." + identifier + ".tag", tags);
            getTagConfig().set("tags." + identifier + ".permission", permission);
            getTagConfig().set("tags." + identifier + ".description", description);
            getTagConfig().set("tags." + identifier + ".category", default_category);
            getTagConfig().set("tags." + identifier + ".order", orderID);
            getTagConfig().set("tags." + identifier + ".withdrawable", true);
            getTagConfig().set("tags." + identifier + ".cost-tag", false);
            getTagConfig().set("tags." + identifier + ".displayname", "&7Tag: %tag%");
            getTagConfig().set("tags." + identifier + ".custom-model-data", 0);
            getTagConfig().set("tags." + identifier + ".display-item", "NAME_TAG");
            getTagConfig().set("tags." + identifier + ".voucher-item.material", "NAME_TAG");
            getTagConfig().set("tags." + identifier + ".voucher-item.displayname", tag_string + " &f&lVoucher");
            getTagConfig().set("tags." + identifier + ".voucher-item.custom-model-data", 0);
            getTagConfig().set("tags." + identifier + ".voucher-item.glow", true);
            getTagConfig().set("tags." + identifier + ".voucher-item.lore", voucher_item_lore);
            getTagConfig().set("tags." + identifier + ".locked-tag.displayname", "&7Tag: %tag%");
            getTagConfig().set("tags." + identifier + ".locked-tag.display-item", "BARRIER");
            getTagConfig().set("tags." + identifier + ".locked-tag.custom-model-data", 0);
            getTagConfig().set("tags." + identifier + ".rarity", "common");
            getTagConfig().set("tags." + identifier + ".economy.enabled", false);
            getTagConfig().set("tags." + identifier + ".economy.type", "VAULT");
            getTagConfig().set("tags." + identifier + ".economy.amount", 200);
            saveTagConfig();

            msgPlayer(player, "&8[&6&lTAG&8] &7New tag created &6" + identifier + " &f- " + tag_string);

            unloadTags();
            loadTags(true);
        } else {
            msgPlayer(player, validtag);
        }
    }

    public void createTag(String identifier, String tag_string, List<String> description, String permission, double cost) {
        if (!tags.containsKey(identifier)) {

            String default_category = SupremeTags.getInstance().getConfig().getString("settings.default-category");

            List<String> tagList = new ArrayList<>();
            tagList.add(tag_string);

            int orderID = tags.size() + 1;

            TagEconomy economy = new TagEconomy("VAULT", 200, false);

            Tag tag = new Tag(identifier, tagList, default_category, permission, description, orderID, false, false, "common", new HashMap<>(), economy);
            tags.put(identifier, tag);

            List<String> voucher_item_lore = new ArrayList<>();
            voucher_item_lore.add("&7&m-----------------------------");
            voucher_item_lore.add("&eClick to equip!");
            voucher_item_lore.add("&7&m-----------------------------");

            List<String> tags = new ArrayList<>();
            tags.add(tag_string);

            getTagConfig().set("tags." + identifier + ".tag", tags);
            getTagConfig().set("tags." + identifier + ".permission", permission);
            getTagConfig().set("tags." + identifier + ".description", description);
            getTagConfig().set("tags." + identifier + ".category", default_category);
            getTagConfig().set("tags." + identifier + ".order", orderID);
            getTagConfig().set("tags." + identifier + ".withdrawable", true);
            getTagConfig().set("tags." + identifier + ".displayname", "&7Tag: %tag%");
            getTagConfig().set("tags." + identifier + ".custom-model-data", 0);
            getTagConfig().set("tags." + identifier + ".display-item", "NAME_TAG");
            getTagConfig().set("tags." + identifier + ".voucher-item.material", "NAME_TAG");
            getTagConfig().set("tags." + identifier + ".voucher-item.displayname", tag_string + " &f&lVoucher");
            getTagConfig().set("tags." + identifier + ".voucher-item.custom-model-data", 0);
            getTagConfig().set("tags." + identifier + ".voucher-item.glow", true);
            getTagConfig().set("tags." + identifier + ".voucher-item.lore", voucher_item_lore);
            getTagConfig().set("tags." + identifier + ".locked-tag.displayname", "&7Tag: %tag%");
            getTagConfig().set("tags." + identifier + ".locked-tag.display-item", "BARRIER");
            getTagConfig().set("tags." + identifier + ".locked-tag.custom-model-data", 0);
            getTagConfig().set("tags." + identifier + ".rarity", "common");
            getTagConfig().set("tags." + identifier + ".economy.enabled", false);
            getTagConfig().set("tags." + identifier + ".economy.type", "VAULT");
            getTagConfig().set("tags." + identifier + ".economy.amount", 200);
            saveTagConfig();
        }
    }

    public void createTag(String identifier, String material, String tag_string, List<String> description, String permission, double cost) {
        if (!tags.containsKey(identifier)) {

            String default_category = SupremeTags.getInstance().getConfig().getString("settings.default-category");

            List<String> tagList = new ArrayList<>();
            tagList.add(tag_string);

            int orderID = tags.size() + 1;

            TagEconomy economy = new TagEconomy("VAULT", 200, false);

            Tag tag = new Tag(identifier, tagList, default_category, permission, description, orderID, true, false, "common", new HashMap<>(), economy);
            tags.put(identifier, tag);

            List<String> voucher_item_lore = new ArrayList<>();
            voucher_item_lore.add("&7&m-----------------------------");
            voucher_item_lore.add("&eClick to equip!");
            voucher_item_lore.add("&7&m-----------------------------");

            List<String> tags = new ArrayList<>();
            tags.add(tag_string);

            getTagConfig().set("tags." + identifier + ".tag", tags);
            getTagConfig().set("tags." + identifier + ".permission", permission);
            getTagConfig().set("tags." + identifier + ".description", description);
            getTagConfig().set("tags." + identifier + ".category", default_category);
            getTagConfig().set("tags." + identifier + ".order", orderID);
            getTagConfig().set("tags." + identifier + ".withdrawable", true);
            getTagConfig().set("tags." + identifier + ".displayname", "&7Tag: %tag%");
            getTagConfig().set("tags." + identifier + ".custom-model-data", 0);
            getTagConfig().set("tags." + identifier + ".display-item", material);
            getTagConfig().set("tags." + identifier + ".voucher-item.material", "NAME_TAG");
            getTagConfig().set("tags." + identifier + ".voucher-item.displayname", tag_string + " &f&lVoucher");
            getTagConfig().set("tags." + identifier + ".voucher-item.custom-model-data", 0);
            getTagConfig().set("tags." + identifier + ".voucher-item.glow", true);
            getTagConfig().set("tags." + identifier + ".voucher-item.lore", voucher_item_lore);
            getTagConfig().set("tags." + identifier + ".locked-tag.displayname", "&7Tag: %tag%");
            getTagConfig().set("tags." + identifier + ".locked-tag.display-item", "BARRIER");
            getTagConfig().set("tags." + identifier + ".locked-tag.custom-model-data", 0);
            getTagConfig().set("tags." + identifier + ".rarity", "common");
            getTagConfig().set("tags." + identifier + ".economy.enabled", false);
            getTagConfig().set("tags." + identifier + ".economy.type", "VAULT");
            getTagConfig().set("tags." + identifier + ".economy.amount", 200);
            saveTagConfig();
            reloadTagConfig();
        }
    }

    public void createTag(String identifier, String material, String tag_string, List<String> description, String permission, double cost, int custom_model_data) {
        if (!tags.containsKey(identifier)) {

            String default_category = SupremeTags.getInstance().getConfig().getString("settings.default-category");

            List<String> tagList = new ArrayList<>();
            tagList.add(tag_string);

            int orderID = tags.size() + 1;

            TagEconomy economy = new TagEconomy("VAULT", 200, false);

            Tag tag = new Tag(identifier, tagList, default_category, permission, description, orderID, true, false, "common", new HashMap<>(), economy);
            tags.put(identifier, tag);

            List<String> voucher_item_lore = new ArrayList<>();
            voucher_item_lore.add("&7&m-----------------------------");
            voucher_item_lore.add("&eClick to equip!");
            voucher_item_lore.add("&7&m-----------------------------");

            List<String> tags = new ArrayList<>();
            tags.add(tag_string);

            getTagConfig().set("tags." + identifier + ".tag", tags);
            getTagConfig().set("tags." + identifier + ".permission", permission);
            getTagConfig().set("tags." + identifier + ".description", description);
            getTagConfig().set("tags." + identifier + ".category", default_category);
            getTagConfig().set("tags." + identifier + ".order", orderID);
            getTagConfig().set("tags." + identifier + ".withdrawable", true);
            getTagConfig().set("tags." + identifier + ".displayname", "&7Tag: %tag%");
            getTagConfig().set("tags." + identifier + ".custom-model-data", custom_model_data);
            getTagConfig().set("tags." + identifier + ".display-item", material);
            getTagConfig().set("tags." + identifier + ".voucher-item.material", "NAME_TAG");
            getTagConfig().set("tags." + identifier + ".voucher-item.displayname", tag_string + " &f&lVoucher");
            getTagConfig().set("tags." + identifier + ".voucher-item.custom-model-data", 0);
            getTagConfig().set("tags." + identifier + ".voucher-item.glow", true);
            getTagConfig().set("tags." + identifier + ".voucher-item.lore", voucher_item_lore);
            getTagConfig().set("tags." + identifier + ".locked-tag.displayname", "&7Tag: %tag%");
            getTagConfig().set("tags." + identifier + ".locked-tag.display-item", "BARRIER");
            getTagConfig().set("tags." + identifier + ".locked-tag.custom-model-data", 0);
            getTagConfig().set("tags." + identifier + ".rarity", "common");
            getTagConfig().set("tags." + identifier + ".economy.enabled", false);
            getTagConfig().set("tags." + identifier + ".economy.type", "VAULT");
            getTagConfig().set("tags." + identifier + ".economy.amount", 200);
            saveTagConfig();
            reloadTagConfig();
        }
    }

    public void deleteTag(CommandSender player, String identifier) {
        if (tags.containsKey(identifier)) {
            tags.remove(identifier);

            try {
                getTagConfig().set("tags." + identifier, null);
                saveTagConfig();
                reloadTagConfig();
            } catch (Exception e) {
                e.printStackTrace();
            }

            msgPlayer(player, "&8[&6&lTAG&8] &7Tag &6" + identifier + " &7is now deleted!");
        } else {
            msgPlayer(player, invalidtag);
        }
    }

    public void deleteTag(Player player, String identifier) {
        if (tags.containsKey(identifier)) {
            tags.remove(identifier);

            try {
                getTagConfig().set("tags." + identifier, null);
                saveTagConfig();
                reloadTagConfig();
            } catch (Exception e) {
                e.printStackTrace();
            }

            msgPlayer(player, "&8[&6&lTAG&8] &7Tag &6" + identifier + " &7is now deleted!");
        } else {
            msgPlayer(player, invalidtag);
        }
    }

    public void loadTags(boolean silent) {
        FileConfiguration tagConfig = getTagConfig();
        ConfigurationSection tagsSection = tagConfig.getConfigurationSection("tags");

        if (tagsSection == null) {
            if (!silent) {
                runMain(() ->
                        Bukkit.getConsoleSender().sendMessage("[TAGS] No tags found in configuration.")
                );
            }
            return;
        }

        runAsync(() -> {
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

                            String rarity_variant = tagConfig.getString("tags." + identifier + ".variants." + var + ".rarity");
                            if (rarity == null || !tagConfig.isSet("tags." + identifier + ".variants." + var + ".rarity")) {
                                rarity_variant = rarity;
                            }

                            variants.add(new Variant(var, identifier, variantTag, permission, variantDescription, rarity_variant));
                        }
                    }
                }

                String permission = section.getString("permission", "none");
                int orderID = section.getInt("order");
                boolean withdrawable = section.getBoolean("withdrawable");
                boolean costTag = section.getBoolean("cost-tag");

                String eco_type = section.getString("economy.type", "VAULT");
                double eco_amount = section.getDouble("economy.amount", 200);
                boolean eco_enabled = section.getBoolean("economy.enabled", false);

                TagEconomy economy = new TagEconomy(eco_type, eco_amount, eco_enabled);

                Tag t = new Tag(identifier, tag, category, permission, description, orderID, withdrawable, costTag, rarity, effects, economy);
                t.setVariants(variants);
                loadedTags.put(identifier, t);

                count++;
            }

            int finalCount = count;

            // Apply loaded tags on main thread
            runMain(() -> {
                tags.clear();
                tags.putAll(loadedTags);

                for (Tag tag : tags.values()) {
                    if (tag.getTag().size() > 1) {
                        tag.startAnimation();
                    }
                }

                if (!silent) {
                    Bukkit.getConsoleSender().sendMessage("[TAGS] Loaded " + finalCount + " tag(s) successfully.");
                }
            });
        });
    }

    public Variant getVariant(String variant_identifier) {
        for (Tag tag : getTags().values()) {
            for (Variant var : tag.getVariants()) {
                if (var.getIdentifier().equalsIgnoreCase(variant_identifier)) {
                    return var;
                }
            }
        }

        return null;
    }

    public List<Variant> getVariants() {
        List<Variant> variants = new ArrayList<>();
        for (Tag tag : getTags().values()) {
            for (Variant var : tag.getVariants()) {
                variants.add(var);
            }
        }

        return variants;
    }

    public boolean isVariant(String variant_identifier) {
        return getVariant(variant_identifier) != null;
    }

    public boolean hasVariantTag(OfflinePlayer player) {
        return getVariant(UserData.getActive(player.getUniqueId())) != null;
    }

    public Variant getVariantTag(OfflinePlayer player) {
        String activeTag = UserData.getActive(player.getUniqueId());

        if (hasVariantTag(player)) {
            Variant variant = getVariant(activeTag);

            return variant;
        }

        return null;
    }

    public Tag getTag(String identifier) {
        Tag t = null;

        if (tags.containsKey(identifier)) {
            t = tags.get(identifier);
        }

        return t;
    }

    public boolean doesTagExist(String identifier) {
        return getTag(identifier) != null;
    }

    public void unloadTags() {
        if (!tags.isEmpty()) {
            tags.clear();
        }
    }

    public Map<String, Tag> getTags() {
        return tags;
    }
    public Map<Integer, String> getDataItem() { return dataItem; }

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

    public boolean isCost() {
        return isCost;
    }

    public void setCost(boolean isCost) {
        this.isCost = isCost;
    }

    public void saveTag(Tag tag) {
        String identifier = tag.getIdentifier();
        getTagConfig().set("tags." + identifier + ".tag", tag.getTag());
        getTagConfig().set("tags." + identifier + ".permission", tag.getPermission());
        getTagConfig().set("tags." + identifier + ".description", tag.getDescription());
        getTagConfig().set("tags." + identifier + ".category", tag.getCategory());
        getTagConfig().set("tags." + identifier + ".economy.amount", tag.getEconomy().getAmount());
        getTagConfig().set("tags." + identifier + ".withdrawable", tag.isWithdrawable());

        // Save the changes to the configuration file
        saveTagConfig();
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

        return tags.values().stream()
                .map(Tag::getIdentifier)
                .anyMatch(id -> pattern.matcher(id).find());
    }

    private List<String> normalizeList(FileConfiguration config, String path) {
        Object val = config.get(path);
        if (val instanceof String) {
            return Collections.singletonList((String) val);
        } else if (val instanceof List) {
            return config.getStringList(path);
        }
        return new ArrayList<>();
    }

    public FileConfiguration getMessages() { return messages; }
}