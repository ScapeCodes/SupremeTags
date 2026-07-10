package net.noscape.project.supremetags.handlers;

import net.noscape.project.supremetags.SupremeTags;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.List;

public class Variant {

    private String identifier;
    private String parent_tag_identifier;
    private List<String> tag;
    private String current_tag;
    private String permission;
    private List<String> description;
    private String rarity;
    private BukkitTask animationTask;

    // item
    private String unlocked_material;
    private String unlocked_displayname;
    private int unlocked_custom_model_data;

    private String locked_material;
    private String locked_displayname;
    private int locked_custom_model_data;

    public Variant(String identifier, String parentTagIdentifier, List<String> tag, String permission, List<String> description, String rarity) {
        this.identifier = identifier;
        parent_tag_identifier = parentTagIdentifier;
        this.tag = tag;
        this.permission = permission;
        this.description = description;
        this.rarity = rarity;
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

    public String getParentTagIdentifier() {
        return parent_tag_identifier;
    }

    public void setParentTagIdentifier(String parent_tag_identifier) {
        this.parent_tag_identifier = parent_tag_identifier;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public Tag getSisterTag() {
        return SupremeTags.getInstance().getTagManager().getTag(parent_tag_identifier);
    }

    public List<String> getDescription() {
        return description;
    }

    public void setDescription(List<String> description) {
        this.description = description;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }

    public String getRarity() {
        return rarity;
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
                //plugin.getLogger().warning("Folia scheduler not found: " + e.getMessage());
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

    public String getUnlocked_material() {
        return unlocked_material;
    }

    public void setUnlocked_material(String unlocked_material) {
        this.unlocked_material = unlocked_material;
    }

    public String getUnlocked_displayname() {
        return unlocked_displayname;
    }

    public void setUnlocked_displayname(String unlocked_displayname) {
        this.unlocked_displayname = unlocked_displayname;
    }

    public int getUnlocked_custom_model_data() {
        return unlocked_custom_model_data;
    }

    public void setUnlocked_custom_model_data(int unlocked_custom_model_data) {
        this.unlocked_custom_model_data = unlocked_custom_model_data;
    }

    public String getLocked_material() {
        return locked_material;
    }

    public void setLocked_material(String locked_material) {
        this.locked_material = locked_material;
    }

    public String getLocked_displayname() {
        return locked_displayname;
    }

    public void setLocked_displayname(String locked_displayname) {
        this.locked_displayname = locked_displayname;
    }

    public int getLocked_custom_model_data() {
        return locked_custom_model_data;
    }

    public void setLocked_custom_model_data(int locked_custom_model_data) {
        this.locked_custom_model_data = locked_custom_model_data;
    }
}