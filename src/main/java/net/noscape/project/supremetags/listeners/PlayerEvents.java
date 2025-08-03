package net.noscape.project.supremetags.listeners;

import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.checkers.UpdateChecker;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.storage.PlayerConfig;
import net.noscape.project.supremetags.storage.UserData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;

import static net.noscape.project.supremetags.utils.Utils.*;

public class PlayerEvents implements Listener {

    private final Map<String, Tag> tags;

    public PlayerEvents() {
        tags = SupremeTags.getInstance().getTagManager().getTags();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        UserData.createPlayer(player);

        if (SupremeTags.getInstance().getConfig().getBoolean("settings.personal-tags.enable")) {
            SupremeTags.getInstance().getPlayerConfig().loadPlayer(player);
        }

        if (SupremeTags.getInstance().getConfig().getBoolean("settings.forced-tag")) {
            String activeTag = UserData.getActive(player.getUniqueId());
            if (activeTag.equalsIgnoreCase("None")) {
                String defaultTag = SupremeTags.getInstance().getConfig().getString("settings.default-tag");
                UserData.setActive(player, defaultTag);
            }
        }

        if (SupremeTags.getInstance().isDataCache()) {
            SupremeTags.getInstance().getDataCache().removeFromCache(player.getUniqueId().toString());
            UserData.getActive(player.getUniqueId());
        }

        /*
         * CHECK IF TAG STILL EXIST!
         */
        if (!tags.containsKey(UserData.getActive(player.getUniqueId())) && !SupremeTags.getInstance().getPlayerManager().listAllStringTags(player.getUniqueId()).contains(UserData.getActive(player.getUniqueId()))) {
            UserData.setActive(player, "None");
        }

        if (tags.containsKey(UserData.getActive(player.getUniqueId())) && !player.hasPermission(tags.get(UserData.getActive(player.getUniqueId())).getPermission())) {
            UserData.setActive(player, "None");
        }

        if (tags.containsKey(UserData.getActive(player.getUniqueId())) && !UserData.getActive(player.getUniqueId()).equalsIgnoreCase("none")) {
            Tag tag = tags.get(UserData.getActive(player.getUniqueId()));
            tag.applyEffects(player);
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

    public Map<String, Tag> getTags() {
        return tags;
    }
}