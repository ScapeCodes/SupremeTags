package net.noscape.project.supremetags.listeners;

import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.checkers.UpdateChecker;
import net.noscape.project.supremetags.enums.TPermissions;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.storage.user.PlayerConfig;
import net.noscape.project.supremetags.storage.UserData;
import net.noscape.project.supremetags.utils.Utils;
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
import java.util.UUID;

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

        runAsync(() -> {

            UserData.createPlayer(player);
            UserData.getTagCredits(player.getUniqueId());

            if (plugin.getFileSyncingManager() != null) {
                Utils.runMainLater(() -> {
                    plugin.getFileSyncingManager().flushPendingSync();
                    plugin.getFileSyncingManager().requestTagFilesSync();
                }, 20L);
            }

            if ((plugin.isMySQL() || plugin.isMaria()) && plugin.isDataCache()) {
                plugin.getDataCache().removeFromCache(player.getUniqueId().toString());
            }

            if (plugin.getConfig().getBoolean("settings.personal-tags.enable")) {
                plugin.getPlayerConfig().loadPlayer(player);
            }

            String activeTag = UserData.getActive(player.getUniqueId());

            if (plugin.getConfig().getBoolean("settings.forced-tag") &&
                    (activeTag == null || activeTag.equalsIgnoreCase("None"))) {

                String defaultTag = plugin.getConfig().getString("settings.default-tag");
                UserData.setActive(player, defaultTag);
                activeTag = defaultTag;
            }

            runMain(() -> {
                String currentTag = UserData.getActive(player.getUniqueId());
                if (currentTag == null || currentTag.isBlank()) {
                    currentTag = "None";
                }

                boolean isVariant = plugin.getTagManager().isVariant(currentTag);
                boolean tagExists = tags.containsKey(currentTag);
                boolean isPersonalTag = plugin.getPlayerManager().doesTagExist(player.getUniqueId(), currentTag);

                if (!tagExists && !isVariant && !isPersonalTag) {
                    UserData.setActive(player, "None");
                    if (plugin.getAutoApplyManager() != null) plugin.getAutoApplyManager().apply(player, "None");
                    return;
                }

                if (isPersonalTag && !tagExists && !isVariant) {
                    Tag personalTag =
                            plugin.getPlayerManager().getTag(player.getUniqueId(), currentTag);

                    if (personalTag != null) {
                        UserData.setActive(player, personalTag.getIdentifier());
                        if (plugin.getAutoApplyManager() != null) plugin.getAutoApplyManager().apply(player, personalTag.getIdentifier());
                    }

                    return;
                }

                Tag tag = tags.get(currentTag);

                if (tag != null) {
                    if (!player.hasPermission(tag.getPermission())) {
                        UserData.setActive(player, "None");
                    } else {
                        tag.applyEffects(player);
                        if (plugin.getAutoApplyManager() != null) plugin.getAutoApplyManager().apply(player, currentTag);
                    }
                }
            });

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

        });
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        SupremeTags plugin = SupremeTags.getInstance();
        UUID uuid = player.getUniqueId();

        runAsync(() -> {
            String active = UserData.getActive(uuid);
            Tag activeTag = tags.get(active);

            runMain(() -> {
                if (plugin.getAutoApplyManager() != null) plugin.getAutoApplyManager().restore(player);
                if (activeTag != null && !active.equalsIgnoreCase("none")) {
                    activeTag.removeEffects(player);
                }
            });

            if (plugin.getPlayerManager().getPlayerTags(uuid) != null) {
                try {
                    PlayerConfig.save(player.getUniqueId());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                plugin.getPlayerManager().getPlayerTags().remove(uuid);
            }

            if (plugin.isDataCache()) {
                try {
                    String cached = plugin.getDataCache().getCachedData(uuid.toString());
                    UserData.setActiveManual(player, cached);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            plugin.getSetupList().remove(uuid);
            plugin.getEditorList().remove(uuid);
            plugin.getVoucherManager().remove(player);

        });
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

            Utils.runMainLater(() -> reapplyEffects(player), 1L);
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
