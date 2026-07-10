package net.noscape.project.supremetags.checkers;

import net.noscape.project.supremetags.SupremeTags;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Scanner;
import java.util.function.Consumer;

// From: https://www.spigotmc.org/wiki/creating-an-update-checker-that-checks-for-updates
public class UpdateChecker {

    private final JavaPlugin plugin;
    private final int resourceId;

    public UpdateChecker(JavaPlugin plugin, int resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;
    }

    public void getVersion(final Consumer<String> consumer) {
        Runnable task = () -> {
            try (InputStream is = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + this.resourceId).openStream();
                 Scanner scann = new Scanner(is)) {
                if (scann.hasNext()) {
                    consumer.accept(scann.next());
                }
            } catch (IOException e) {
                plugin.getLogger().info("Unable to check for updates: " + e.getMessage());
            }
        };

        if (!SupremeTags.getInstance().isFoliaFound()) {
            // Non-Folia (Spigot/Paper) support
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, task);
        } else {
            // Folia support using reflection
            try {
                Object server = Bukkit.getServer();
                Method getSchedulerMethod = server.getClass().getMethod("getGlobalRegionScheduler");
                Object scheduler = getSchedulerMethod.invoke(server);
                Method runMethod = scheduler.getClass().getMethod("run", Plugin.class, Runnable.class);
                runMethod.invoke(scheduler, this.plugin, task);
            } catch (Exception e) {
                //plugin.getLogger().warning("Folia scheduler not found: " + e.getMessage());
            }
        }
    }
}