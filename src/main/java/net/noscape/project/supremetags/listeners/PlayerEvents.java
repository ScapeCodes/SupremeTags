package net.noscape.project.supremetags.listeners;

import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.checkers.UpdateChecker;
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

        if (SupremeTags.getInstance().dev_build) {
            if (player.isOp()) {
                String version = SupremeTags.getInstance().getDescription().getVersion() + "-DEV-" + SupremeTags.getInstance().build;
                String link = "https://www.spigotmc.org/resources/103140";

                for (String msg : configMessageList("dev-build-alert", messages)) {
                    msgPlayer(player, msg.replace("%version%", version).replace("%link%", link));
                }
            }
        }

        // 1. Load player data safely
        UserData.createPlayer(player);
        SupremeTags plugin = SupremeTags.getInstance();

        // 2. Load personal/player-specific data before changing anything
        if (plugin.getConfig().getBoolean("settings.personal-tags.enable")) {
            plugin.getPlayerConfig().loadPlayer(player);
        }

        // 3. Load active tag from cache or file
        String activeTag = UserData.getActive(player.getUniqueId());

        // 4. Apply forced tag only if no tag found AFTER loading
        if (plugin.getConfig().getBoolean("settings.forced-tag") &&
                (activeTag == null || activeTag.equalsIgnoreCase("None"))) {
            String defaultTag = plugin.getConfig().getString("settings.default-tag");
            UserData.setActive(player, defaultTag);
        }

        // 5. Validate and reapply tag effects
        activeTag = UserData.getActive(player.getUniqueId());
        if (!tags.containsKey(activeTag) && !SupremeTags.getInstance().getTagManager().isVariant(activeTag)) {
            UserData.setActive(player, "None");
        } else {
            Tag tag = tags.get(activeTag);
            if (!player.hasPermission(tag.getPermission())) {
                UserData.setActive(player, "None");
            } else {
                tag.applyEffects(player);
            }
        }

        if (SupremeTags.getInstance().getConfig().getBoolean("settings.update-check")) {
            if (player.isOp()) {
                new UpdateChecker(SupremeTags.getInstance(), 111481).getVersion(version -> {
                    if (version == null) {
                        Bukkit.getServer().getLogger().warning("> Updater: Failed to retrieve latest version of SupremeTags.");
                        return;
                    }

                    String currentVersion = SupremeTags.getInstance().getDescription().getVersion();
                    if (compareVersions(version, currentVersion) > 0) {
                        msgPlayer(player, "&6&lSupremeTags-Premium &8&l> &7An update is available! &b" + version,
                                "&eDownload at &bhttps://www.spigotmc.org/resources/111481/updates");
                    }
                });
            }
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        Player player = e.getPlayer();

        if (tags.containsKey(UserData.getActive(player.getUniqueId())) && !UserData.getActive(player.getUniqueId()).equalsIgnoreCase("none")) {
            Tag tag = tags.get(UserData.getActive(player.getUniqueId()));
            tag.removeEffects(player);
        }

        if (SupremeTags.getInstance().getPlayerManager().getPlayerTags(player.getUniqueId()) != null) {
            PlayerConfig.save(player);
            SupremeTags.getInstance().getPlayerManager().getPlayerTags().remove(player.getUniqueId());
        }

        if (SupremeTags.getInstance().isDataCache()) {
            UserData.setActiveManual(player, SupremeTags.getInstance().getDataCache().getCachedData(player.getUniqueId().toString()));
        }

        SupremeTags.getInstance().getSetupList().remove(player);
        SupremeTags.getInstance().getEditorList().remove(player);
        SupremeTags.getInstance().getVoucherManager().remove(player);
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