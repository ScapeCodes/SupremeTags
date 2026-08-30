package net.noscape.project.supremetags.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;

import java.util.Locale;

public class SoundUtils {

    public static Sound getSound(String soundName) {
        if (soundName == null || soundName.isEmpty() || soundName.equalsIgnoreCase("none")) {
            return null;
        }

        soundName = soundName.toLowerCase(Locale.ROOT).replace("minecraft:", "");
        return Registry.SOUNDS.get(NamespacedKey.minecraft(soundName));
    }
}
