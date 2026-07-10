package net.noscape.project.supremetags.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class XMaterialUtil {

    private static final boolean LEGACY = detectLegacy();

    private static boolean detectLegacy() {
        try {
            // 1.13+ servers will have "LIME_DYE"
            Material.valueOf("LIME_DYE");
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    public static boolean isLegacy() {
        return LEGACY;
    }

    /**
     * Matches a material between modern (1.13+) and legacy (<1.13) systems.
     * Supports optional data values for legacy materials (e.g. "INK_SACK:10").
     */
    public static ItemStack item(String modern, String legacy) {
        return item(modern, legacy, 1);
    }

    public static ItemStack item(String modern, String legacy, int amount) {
        if (LEGACY && legacy != null) {
            try {
                if (legacy.contains(":")) {
                    String[] split = legacy.split(":");
                    Material mat = Material.valueOf(split[0]);
                    short data = Short.parseShort(split[1]);
                    return new ItemStack(mat, amount, data);
                }
                return new ItemStack(Material.valueOf(legacy), amount);
            } catch (Exception ignored) {}
        }

        try {
            return new ItemStack(Material.valueOf(modern), amount);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[SupremeTags] Unknown material: " + modern + " / " + legacy);
            return new ItemStack(Material.STONE);
        }
    }

    /**
     * Returns only the Material (for GUI slots where data isn't needed).
     */
    public static Material match(String modern, String legacy) {
        if (LEGACY && legacy != null && legacy.contains(":")) {
            legacy = legacy.split(":")[0]; // Strip data if only Material is needed
        }

        try {
            return LEGACY && legacy != null
                    ? Material.valueOf(legacy)
                    : Material.valueOf(modern);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[SupremeTags] Unknown material: " + modern + " / " + legacy);
            return Material.STONE;
        }
    }
}
