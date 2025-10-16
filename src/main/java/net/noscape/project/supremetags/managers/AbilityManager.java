package net.noscape.project.supremetags.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class AbilityManager {

    public static void triggerAbilities(Player player, List<String> abilities) {
        if (abilities == null || abilities.isEmpty()) return;

        for (String ability : abilities) {
            if (ability.startsWith("mythic:")) {
                String skill = ability.substring("mythic:".length());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mythicmobs skills cast " + skill + " " + player.getName());
            } else if (ability.startsWith("auraskills:")) {
                String skill = ability.substring("auraskills:".length());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "auraskills cast " + skill + " " + player.getName());
            }
        }
    }
}