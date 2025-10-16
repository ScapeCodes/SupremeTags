package net.noscape.project.supremetags.handlers;

import net.noscape.project.supremetags.SupremeTags;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class Tag {

    private String identifier;
    private List<String> tag;
    private String category;
    private String permission;
    private List<String> description;
    private String current_tag;
    private int order;
    private boolean isWithdrawable;
    private List<Variant> variants;

    private String rarity;

    //          key      value
    // %supremetags.tag.custom-placeholder.<key>% - returns the value(message) in map from key<placeholder-name>.
    //private Map<String, String> custom_placeholders;

    private BukkitTask animationTask;

    private Map<PotionEffectType, Integer> effects;

    // economy
    private TagEconomy economy;

    // abilities
    private List<String> abilities;

    public Tag(String identifier, List<String> tag, String category, String permission, List<String> description, int order, boolean isWithdrawable, String rarity, Map<PotionEffectType, Integer> effects, TagEconomy economy) {
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
    }

    public Tag(String identifier, List<String> tag, String category, String permission, List<String> description, boolean isWithdrawable, String rarity, TagEconomy economy) {
        this.identifier = identifier;
        this.tag = tag;
        this.category = category;
        this.permission = permission;
        this.description = description;
        this.isWithdrawable = isWithdrawable;
        this.rarity = rarity;
        this.economy = economy;
    }

    public Tag(String identifier, List<String> tag, List<String> description) {
        this.identifier = identifier;
        this.tag = tag;
        this.description = description;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public List<String> getTag() {
        return tag;
    }

    public void setTag(List<String> tag) {
        this.tag = tag;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public List<String> getDescription() {
        return description;
    }

    public void setDescription(List<String> description) {
        this.description = description;
    }

    public void startAnimation() {
        SupremeTags plugin = SupremeTags.getInstance();
        int defaultSpeed = plugin.getConfig().getInt("settings.animated-tag-speed");

        // Get the tag-specific speed, if present
        ConfigurationSection tagConfig = plugin.getTagManager().getTagConfig().getConfigurationSection("tags." + identifier);
        int animationSpeed = (tagConfig != null) ? tagConfig.getInt("animated-tag-speed", defaultSpeed) : defaultSpeed;

        // Validate speed
        if (animationSpeed <= 0 || animationSpeed > 9999) {
            return;
        }

        // Stop previous animation
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
            // Use BukkitRunnable for non-Folia environments
            animationTask = new BukkitRunnable() {
                @Override
                public void run() {
                    animationTaskRunnable.run();
                }
            }.runTaskTimerAsynchronously(plugin, 0L, animationSpeed);
        } else {
            // Folia scheduler via reflection
            try {
                Object server = Bukkit.getServer();
                Method getScheduler = server.getClass().getMethod("getGlobalRegionScheduler");
                Object scheduler = getScheduler.invoke(server);

                Method runAtFixedRate = scheduler.getClass().getMethod(
                        "runAtFixedRate", Plugin.class, Runnable.class, long.class, long.class
                );

                runAtFixedRate.invoke(scheduler, plugin, animationTaskRunnable, 0L, animationSpeed);
            } catch (Exception e) {
                plugin.getLogger().warning("Folia scheduler not found: " + e.getMessage());
            }
        }
    }

    public void stopAnimation() {
        if (animationTask != null) {
            animationTask.cancel();
            animationTask = null;
        }
    }

    public String getCurrentTag() {
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
        return variants;
    }

    public void setVariants(List<Variant> variants) {
        this.variants = variants;
    }

    public Variant getVariant(String var_identifier) {
        for (Variant var : getVariants()) {
            if (var.getIdentifier().equalsIgnoreCase(var_identifier)) {
                return var;
            }
        }

        return null;
    }

    public boolean isCostTag() {
        return this.economy.isEnabled();
    }

    public String getCustomPlaceholder(String identifier, String placeholder) {
        if (!SupremeTags.getInstance().getTagManager().getTagConfig().isSet("tags." + identifier + ".custom-placeholders." + placeholder)) {
            return SupremeTags.getInstance().getTagManager().getMessages().getString("invalid-custom-placeholder", "&cUnknown Placeholder");
        }

        return SupremeTags.getInstance().getTagManager().getTagConfig().getString("tags." + identifier + ".custom-placeholders." + placeholder);
    }

    public Map<PotionEffectType, Integer> getEffects() {
        return effects;
    }

    public void applyEffects(Player player) {
        Map<PotionEffectType, Integer> effects = getEffects();

        //player.sendMessage("trying to add effects (" + effects.size() + ")");

        if (!effects.isEmpty()) {
            for (Map.Entry<PotionEffectType, Integer> entry : effects.entrySet()) {
                player.addPotionEffect(new PotionEffect(entry.getKey(), Integer.MAX_VALUE, entry.getValue() - 1, true, false));
            }
            //player.sendMessage("added effects: " + effects.size());
        }
    }

    public void removeEffects(Player player) {
        Map<PotionEffectType, Integer> effects = getEffects();

        //player.sendMessage("trying to remove effects (" + effects.size() + ")");

        if (!effects.isEmpty()) {
            for (PotionEffectType type : effects.keySet()) {
                player.removePotionEffect(type);
            }
            //player.sendMessage("removed effects: " + effects.size());
        }
    }

    public boolean hasVariants() {
        return !variants.isEmpty();
    }

    public String getRarity() {
        return rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }

    public TagEconomy getEconomy() {
        return economy;
    }

    public String getEcoType() {
        return this.economy.getType();
    }

    public void setEcoType(String ecoType) {
        this.economy.setType(ecoType);
    }

    public double getEcoAmount() {
        return this.economy.getAmount();
    }

    public void setEcoAmount(double ecoAmount) {
        this.economy.setAmount(ecoAmount);
    }

    public boolean isEcoEnabled() {
        return this.economy.isEnabled();
    }

    public void setEcoEnabled(boolean ecoEnabled) {
        this.economy.setEnabled(ecoEnabled);
    }

    public List<String> getAbilities() {
        return abilities;
    }

    public void setAbilities(List<String> abilities) {
        this.abilities = abilities;
    }
}