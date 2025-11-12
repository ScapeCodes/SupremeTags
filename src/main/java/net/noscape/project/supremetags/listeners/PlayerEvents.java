package net.noscape.project.supremetags.listeners;

import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.checkers.UpdateChecker;
import net.noscape.project.supremetags.enums.TPermissions;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.storage.user.PlayerConfig;
import net.noscape.project.supremetags.storage.UserData;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.Map;

import static net.noscape.project.supremetags.utils.Utils.*;

public class PlayerEvents implements Listener {

    private final Map<String, Tag> tags;

    private FileConfiguration messages = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get();

    public PlayerEvents() {
        tags = SupremeTags.getInstance().getTagManager().getTags();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        SupremeTags plugin = SupremeTags.getInstance();

        // ---------------------------
        //  MAIN THREAD: Light checks
        // ---------------------------
        if (plugin.dev_build) {
            if (player.isOp() || player.hasPermission(TPermissions.ADMIN)) {
                String version = plugin.getDescription().getVersion() + "-DEV-" + plugin.build;
                String link = "https://www.spigotmc.org/resources/103140";

                if (!configMessageList("dev-build-alert", messages).isEmpty()) {
                    for (String msg : configMessageList("dev-build-alert", messages)) {
                        msgPlayer(player, msg.replace("%version%", version).replace("%link%", link));
                    }
                }
            }
        }

        // -----------------------------------------------------------------------------------------
        //  ASYNC SECTION — All database + file I/O must be done here (UserData / PlayerConfig).
        // -----------------------------------------------------------------------------------------
        runAsync(() -> {

            // 1. Load player data from DB (async)
            UserData.createPlayer(player);

            // 2. Load YAML per-player config (async)
            if (plugin.getConfig().getBoolean("settings.personal-tags.enable")) {
                plugin.getPlayerConfig().loadPlayer(player);
            }

            // 3. Load active tag
            String activeTag = UserData.getActive(player.getUniqueId());

            // 4. Apply forced/default tag logic
            if (plugin.getConfig().getBoolean("settings.forced-tag") &&
                    (activeTag == null || activeTag.equalsIgnoreCase("None"))) {

                String defaultTag = plugin.getConfig().getString("settings.default-tag");
                UserData.setActive(player, defaultTag);
                activeTag = defaultTag;
            }

            // Validate tag existence
            boolean isVariant = plugin.getTagManager().isVariant(activeTag);
            boolean tagExists = tags.containsKey(activeTag);

            // 5. PERMISSION CHECK + TAG VALIDATION must be applied back on MAIN THREAD:
            runMain(() -> {

                String currentTag = UserData.getActive(player.getUniqueId());

                if (!tagExists && !isVariant) {
                    UserData.setActive(player, "None");
                    return;
                }

                Tag tag = tags.get(currentTag);
                if (tag != null) {
                    if (!player.hasPermission(tag.getPermission())) {
                        UserData.setActive(player, "None");
                    } else {
                        tag.applyEffects(player); // must be MAIN thread
                    }
                }
            });

            // -----------------------------------------------------------------------
            //  OPTIONAL: Update Checker (safe to stay async except msgPlayer → main)
            // -----------------------------------------------------------------------
            if (plugin.getConfig().getBoolean("settings.update-check") && player.isOp()) {
                new UpdateChecker(plugin, 111481).getVersion(version -> {
                    if (version == null) {
                        Bukkit.getServer().getLogger().warning("> Updater: Failed to retrieve latest version of SupremeTags.");
                        return;
                    }

                    String currentVersion = plugin.getDescription().getVersion();
                    if (compareVersions(version, currentVersion) > 0) {
                        runMain(() -> {
                            msgPlayer(player,
                                    "&6&lSupremeTags-Premium &8&l> &7An update is available! &b" + version,
                                    "&eDownload at &bhttps://www.spigotmc.org/resources/111481/updates");
                        });
                    }
                });
            }

        }); // end runAsync
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        SupremeTags plugin = SupremeTags.getInstance();
        String uuid = player.getUniqueId().toString();

        String active = UserData.getActive(player.getUniqueId());
        Tag activeTag = tags.get(active);

        // -------------------------------------------------------
        // 1. MAIN THREAD — remove tag effects immediately
        // -------------------------------------------------------
        if (activeTag != null && !active.equalsIgnoreCase("none")) {
            activeTag.removeEffects(player); // must be main thread
        }

        // -------------------------------------------------------
        // 2. ASYNC SECTION — save data, cache, configs, cleanup
        // -------------------------------------------------------
        runAsync(() -> {

            // -----------------------------------------
            // Save YAML player tag data (file I/O)
            // -----------------------------------------
            if (plugin.getPlayerManager().getPlayerTags(player.getUniqueId()) != null) {
                try {
                    PlayerConfig.save(player); // file write = async
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                plugin.getPlayerManager().getPlayerTags().remove(player.getUniqueId());
            }

            // -----------------------------------------
            // Use DataCache (if enabled)
            // -----------------------------------------
            if (plugin.isDataCache()) {
                try {
                    String cached = plugin.getDataCache().getCachedData(uuid);
                    UserData.setActiveManual(player, cached); // may write DB -> async
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            // -----------------------------------------
            // Remove setup/editor/voucher entries
            // (pure memory ops → async safe)
            // -----------------------------------------
            plugin.getSetupList().remove(player);
            plugin.getEditorList().remove(player);
            plugin.getVoucherManager().remove(player);

        }); // end runAsync
    }


    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();

        reapplyEffects(player);
    }

    @EventHandler
    public void onMilkDrink(PlayerItemConsumeEvent e) {
        Player player = e.getPlayer();

        if (e.getItem().getType().name().equalsIgnoreCase("MILK_BUCKET")) {
            // Delay 1 tick so effects are actually cleared first
            Bukkit.getScheduler().runTaskLater(SupremeTags.getInstance(), () -> {
                reapplyEffects(player);
            }, 1L);
        }
    }

    private void reapplyEffects(Player player) {
        if (tags.containsKey(UserData.getActive(player.getUniqueId()))
                && !UserData.getActive(player.getUniqueId()).equalsIgnoreCase("none")) {
            Tag tag = tags.get(UserData.getActive(player.getUniqueId()));
            if (tag != null) {
                tag.applyEffects(player);
            }
        }
    }

    public Map<String, Tag> getTags() {
        return tags;
    }
}