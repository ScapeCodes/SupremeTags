package net.noscape.project.supremetags.utils;

import org.bukkit.potion.PotionEffectType;

public class CompatUtils {
    private static final boolean HAS_KEY_METHOD;

    static {
        boolean hasKey = false;
        try {
            PotionEffectType.class.getMethod("getKey");
            hasKey = true;
        } catch (NoSuchMethodException ignored) {}
        HAS_KEY_METHOD = hasKey;
    }

    public static String getEffectKey(PotionEffectType effect) {
        if (HAS_KEY_METHOD) {
            return effect.getKey().getKey().toUpperCase();
        } else {
            return effect.getName().toUpperCase();
        }
    }
}