package net.noscape.project.supremetags.utils;

import org.bukkit.Sound;

import java.lang.reflect.Method;
import java.util.Locale;

public class SoundUtils {

    public static Sound getSound(String soundName) {
        if (soundName == null || soundName.isEmpty() || soundName.equalsIgnoreCase("none"))
            return null;

        soundName = soundName.toLowerCase(Locale.ROOT).replace("minecraft:", "");

        // Try 1.21+ Registry API (via reflection)
        try {
            Class<?> registryClass = Class.forName("org.bukkit.Registry");
            Class<?> namespacedKeyClass = Class.forName("org.bukkit.NamespacedKey");

            Method minecraftMethod = namespacedKeyClass.getMethod("minecraft", String.class);
            Object namespacedKey = minecraftMethod.invoke(null, soundName);

            Object soundsRegistry = registryClass.getField("SOUNDS").get(null);

            Method getMethod = registryClass.getMethod("get", namespacedKeyClass);
            Object soundObj = getMethod.invoke(soundsRegistry, namespacedKey);

            if (soundObj instanceof Sound) return (Sound) soundObj;
        } catch (ClassNotFoundException ignored) {
            // Not a 1.21+ server
        } catch (Throwable ignored) {
        }

        // Try legacy enum reflection (≤ 1.20)
        try {
            Method valueOf = Sound.class.getDeclaredMethod("valueOf", String.class);
            return (Sound) valueOf.invoke(null, soundName.toUpperCase(Locale.ROOT));
        } catch (Throwable ignored) {
        }

        // Optional aliases for old sound names
        switch (soundName.toUpperCase(Locale.ROOT)) {
            case "ITEM_PICKUP":
                try {
                    Method valueOf = Sound.class.getDeclaredMethod("valueOf", String.class);
                    return (Sound) valueOf.invoke(null, "ENTITY_ITEM_PICKUP");
                } catch (Throwable ignored) {
                }
                break;
        }

        return null;
    }
}
