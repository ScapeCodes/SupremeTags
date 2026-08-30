package net.noscape.project.supremetags.handlers;

import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.handlers.requirements.TagRequirements;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

public class Tag {

    private static final String DEFAULT_TAG = "";
    private static final String DEFAULT_CATEGORY = "default";
    private static final String DEFAULT_PERMISSION = "none";
    private static final String DEFAULT_RARITY = "common";
    private static final TagEconomy DEFAULT_ECONOMY = new TagEconomy("VAULT", 0.0D, false);

    private String identifier;
    private List<String> tag;
    private String category;
    private String permission;
    private List<String> description;
    private String current_tag;
    private int order;
    private boolean isWithdrawable;
    private List<Variant> variants;
    private List<String> groups;

    private String rarity;

    private BukkitTask animationTask;
    private Object foliaAnimationTask;

    private Map<PotionEffectType, Integer> effects;

    private TagEconomy economy;

    private List<String> abilities;

    private TagRequirements requirements;

    private String displayName;
    private String displayItem;
    private int customModelData;
    private String voucherDisplayName;
    private String voucherMaterial;
    private List<String> voucherLore;
    private int voucherCustomModelData;
    private boolean voucherGlow = true;
    private Map<String, String> customPlaceholders;

    public Tag(String identifier, List<String> tag, String category, String permission, List<String> description, int order, boolean isWithdrawable, String rarity, Map<PotionEffectType, Integer> effects, TagEconomy economy, List<String> groups) {
        this.identifier = identifier;
        this.tag = tag;
        this.category = category;
        this.permission = permission;
        this.description = description;
        this.order = order;
        this.isWithdrawable = isWithdrawable;
        this.rarity = rarity;
        this.effects = effects;
        this.economy = economy;
        this.groups = groups != null ? groups : new ArrayList<>();
    }

    public Tag(String identifier, List<String> tag, String category, String permission, List<String> description, int order, boolean isWithdrawable, String rarity, Map<PotionEffectType, Integer> effects, TagEconomy economy, List<Variant> variants, List<String> groups) {
        this.identifier = identifier;
        this.tag = tag;
        this.category = category;
        this.permission = permission;
        this.description = description;
        this.order = order;
        this.isWithdrawable = isWithdrawable;
        this.rarity = rarity;
        this.effects = effects;
        this.economy = economy;
        this.variants = variants;
        this.groups = groups != null ? groups : new ArrayList<>();
    }

    public Tag(String identifier, List<String> tag, String category, String permission, List<String> description, boolean isWithdrawable, String rarity, TagEconomy economy, List<String> groups) {
        this.identifier = identifier;
        this.tag = tag;
        this.category = category;
        this.permission = permission;
        this.description = description;
        this.isWithdrawable = isWithdrawable;
        this.rarity = rarity;
        this.economy = economy;
        this.groups = groups != null ? groups : new ArrayList<>();
    }

    public Tag(String identifier, List<String> tag, List<String> description) {
        this.identifier = identifier;
        this.tag = tag;
        this.description = description;
        this.groups = new ArrayList<>();
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public List<String> getTag() {
        if (tag == null) {
            tag = new ArrayList<>(Collections.singletonList(DEFAULT_TAG));
        } else if (tag.isEmpty()) {
            tag.add(DEFAULT_TAG);
        }

        return tag;
    }

    public void setTag(List<String> tag) {
        this.tag = tag;
    }

    public String getCategory() {
        if (category == null || category.isBlank()) {
            String configuredDefault = SupremeTags.getInstance().getConfig().getString("settings.default-category");
            return configuredDefault == null || configuredDefault.isBlank() ? DEFAULT_CATEGORY : configuredDefault;
        }

        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPermission() {
        return permission == null || permission.isBlank() ? DEFAULT_PERMISSION : permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public List<String> getDescription() {
        if (description == null) {
            description = new ArrayList<>();
        }

        return description;
    }

    public void setDescription(List<String> description) {
        this.description = description;
    }

    public void startAnimation() {
        SupremeTags plugin = SupremeTags.getInstance();
        int defaultSpeed = plugin.getConfig().getInt("settings.animated-tag-speed");

        ConfigurationSection tagConfig = null;
        for (org.bukkit.configuration.file.FileConfiguration cfg : plugin.getConfigManager().getTagConfigs()) {
            tagConfig = cfg.getConfigurationSection("tags." + identifier);
            if (tagConfig != null) break;
        }
        int animationSpeed = (tagConfig != null) ? tagConfig.getInt("animated-tag-speed", defaultSpeed) : defaultSpeed;

        if (animationSpeed <= 0 || animationSpeed > 9999) {
            return;
        }

        stopAnimation();

        Runnable animationTaskRunnable = new Runnable() {
            int currentIndex = 0;

            @Override
            public void run() {
                currentIndex = (currentIndex + 1) % tag.size();
                current_tag = tag.get(currentIndex);
            }
        };

        if (!plugin.isFoliaFound()) {

            animationTask = new BukkitRunnable() {
                @Override
                public void run() {
                    animationTaskRunnable.run();
                }
            }.runTaskTimerAsynchronously(plugin, 0L, animationSpeed);
        } else {

            try {
                Object server = Bukkit.getServer();
                Method getScheduler = server.getClass().getMethod("getGlobalRegionScheduler");
                Object scheduler = getScheduler.invoke(server);

                Method runAtFixedRate = scheduler.getClass().getMethod(
                        "runAtFixedRate", Plugin.class, Runnable.class, long.class, long.class
                );

                Object result = runAtFixedRate.invoke(scheduler, plugin, animationTaskRunnable, 0L, animationSpeed);
                foliaAnimationTask = result;
            } catch (Exception e) {

            }
        }
    }

    public void stopAnimation() {
        if (animationTask != null) {
            animationTask.cancel();
            animationTask = null;
        }
        if (foliaAnimationTask != null) {
            try {
                java.lang.reflect.Method cancelMethod = foliaAnimationTask.getClass().getMethod("cancel");
                cancelMethod.invoke(foliaAnimationTask);
            } catch (Exception e) {

                if (foliaAnimationTask instanceof java.util.concurrent.ScheduledFuture) {
                    ((java.util.concurrent.ScheduledFuture<?>) foliaAnimationTask).cancel(false);
                }
            }
            foliaAnimationTask = null;
        }
    }

    public String getCurrentTag() {
        if (current_tag == null) return current_tag = getTag().getFirst();
        return current_tag;
    }

    public int getOrder() {
        return order;
    }

    public boolean isWithdrawable() {
        return isWithdrawable;
    }

    public void setWithdrawable(boolean withdrawable) {
        this.isWithdrawable = withdrawable;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public List<Variant> getVariants() {
        if (variants == null) {
            variants = new ArrayList<>();
        }

        return variants;
    }

    public void setVariants(List<Variant> variants) {
        this.variants = variants;
    }

    public Variant getVariant(String var_identifier) {
        if (var_identifier == null) {
            return null;
        }

        for (Variant var : getVariants()) {
            if (var.getIdentifier() != null && var.getIdentifier().equalsIgnoreCase(var_identifier)) {
                return var;
            }
        }

        return null;
    }

    public boolean isCostTag() {
        return getEconomy().isEnabled();
    }

    public String getCustomPlaceholder(String identifier, String placeholder) {
        org.bukkit.configuration.file.FileConfiguration tagConfig = SupremeTags.getInstance().getTagManager().getConfigForTag(identifier);
        if (!tagConfig.isSet("tags." + identifier + ".custom-placeholders." + placeholder)) {
            return SupremeTags.getInstance().getTagManager().getMessages().getString("invalid-custom-placeholder", "&cUnknown Placeholder");
        }

        return tagConfig.getString("tags." + identifier + ".custom-placeholders." + placeholder);
    }

    public Map<PotionEffectType, Integer> getEffects() {
        if (effects == null) {
            effects = new HashMap<>();
        }

        return effects;
    }

    public void applyEffects(Player player) {
        Map<PotionEffectType, Integer> effects = getEffects();

        if (!effects.isEmpty()) {
            for (Map.Entry<PotionEffectType, Integer> entry : effects.entrySet()) {
                player.addPotionEffect(new PotionEffect(entry.getKey(), Integer.MAX_VALUE, entry.getValue() - 1, true, false));
            }

        }
    }

    public void removeEffects(Player player) {
        Map<PotionEffectType, Integer> effects = getEffects();

        if (!effects.isEmpty()) {
            for (PotionEffectType type : effects.keySet()) {
                player.removePotionEffect(type);
            }

        }
    }

    public boolean hasVariants() {
        return !getVariants().isEmpty();
    }

    public String getRarity() {
        return rarity == null || rarity.isBlank() ? DEFAULT_RARITY : rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }

    public TagEconomy getEconomy() {
        if (economy == null) {
            economy = new TagEconomy(DEFAULT_ECONOMY.getType(), DEFAULT_ECONOMY.getAmount(), DEFAULT_ECONOMY.isEnabled());
        }

        return economy;
    }

    public String getEcoType() {
        return getEconomy().getType();
    }

    public void setEcoType(String ecoType) {
        getEconomy().setType(ecoType);
    }

    public double getEcoAmount() {
        return getEconomy().getAmount();
    }

    public void setEcoAmount(double ecoAmount) {
        getEconomy().setAmount(ecoAmount);
    }

    public boolean isEcoEnabled() {
        return getEconomy().isEnabled();
    }

    public void setEcoEnabled(boolean ecoEnabled) {
        getEconomy().setEnabled(ecoEnabled);
    }

    public List<String> getAbilities() {
        if (abilities == null) {
            abilities = new ArrayList<>();
        }

        return abilities;
    }

    public void setAbilities(List<String> abilities) {
        this.abilities = abilities;
    }

    public TagRequirements getRequirements() {
        return requirements;
    }

    public void setRequirements(TagRequirements requirements) {
        this.requirements = requirements;
    }

    public boolean hasRequirements() {
        return requirements != null && requirements.isEnabled();
    }

    public List<String> getGroups() {
        return groups != null ? groups : new ArrayList<>();
    }

    public void setGroups(List<String> groups) {
        this.groups = groups != null ? groups : new ArrayList<>();
    }

    public boolean isAnimated() {
        return getTag().size() > 1;
    }

    public String getDisplayName() {
        return displayName == null || displayName.isBlank() ? "&7Tag: %tag%" : displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayItem() {
        return displayItem == null || displayItem.isBlank() ? "NAME_TAG" : displayItem;
    }

    public void setDisplayItem(String displayItem) {
        this.displayItem = displayItem;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public void setCustomModelData(int customModelData) {
        this.customModelData = customModelData;
    }

    public String getVoucherDisplayName() {
        return voucherDisplayName == null || voucherDisplayName.isBlank() ? getCurrentTag() + " &f&lVoucher" : voucherDisplayName;
    }

    public void setVoucherDisplayName(String voucherDisplayName) {
        this.voucherDisplayName = voucherDisplayName;
    }

    public String getVoucherMaterial() {
        return voucherMaterial == null || voucherMaterial.isBlank() ? "NAME_TAG" : voucherMaterial;
    }

    public void setVoucherMaterial(String voucherMaterial) {
        this.voucherMaterial = voucherMaterial;
    }

    public List<String> getVoucherLore() {
        if (voucherLore == null) {
            voucherLore = new ArrayList<>();
            voucherLore.add("&7&m-----------------------------");
            voucherLore.add("&eClick to equip!");
            voucherLore.add("&7&m-----------------------------");
        }

        return voucherLore;
    }

    public void setVoucherLore(List<String> voucherLore) {
        this.voucherLore = voucherLore == null ? new ArrayList<>() : voucherLore;
    }

    public int getVoucherCustomModelData() {
        return voucherCustomModelData;
    }

    public void setVoucherCustomModelData(int voucherCustomModelData) {
        this.voucherCustomModelData = voucherCustomModelData;
    }

    public boolean isVoucherGlow() {
        return voucherGlow;
    }

    public void setVoucherGlow(boolean voucherGlow) {
        this.voucherGlow = voucherGlow;
    }

    public Map<String, String> getCustomPlaceholders() {
        if (customPlaceholders == null) {
            customPlaceholders = new HashMap<>();
        }
        return customPlaceholders;
    }

    public void setCustomPlaceholders(Map<String, String> customPlaceholders) {
        this.customPlaceholders = customPlaceholders == null ? new HashMap<>() : customPlaceholders;
    }
}
