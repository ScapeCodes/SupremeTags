package net.noscape.project.supremetags.listeners;

import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.enums.EditingType;
import net.noscape.project.supremetags.guis.personaltags.PersonalTagEditorMenu;
import net.noscape.project.supremetags.guis.tageditor.SpecificTagMenu;
import net.noscape.project.supremetags.handlers.Editor;
import net.noscape.project.supremetags.handlers.Tag;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.noscape.project.supremetags.utils.Utils.msgPlayer;

public class EditorListener implements Listener {

    private FileConfiguration messages = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get();

    @EventHandler(priority = EventPriority.HIGH)
    public void onChat(AsyncPlayerChatEvent e) {
        edit(e);
    }

    public void edit(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        if (!SupremeTags.getInstance().getEditorList().containsKey(player)) return;

        String message = e.getMessage();

        Editor editor = SupremeTags.getInstance().getEditorList().get(player);
        EditingType type = editor.getType();

        e.setCancelled(true);

        if (!editor.isPersonalEdit()) {
            Tag tag = SupremeTags.getInstance().getTagManager().getTag(editor.getIdentifier());
            switch (type) {
                case CHANGING_TAG:
                    List<String> tagList = tag.getTag();
                    tagList.add(message);
                    tag.setTag(tagList);
                    break;
                case CHANGING_PERMISSION:
                    tag.setPermission(message);
                    break;
                case CHANGING_CATEGORY:
                    tag.setCategory(message);
                    break;
                case CHANGING_COST:
                    tag.getEconomy().setAmount(Double.parseDouble(message));
                    break;
                case CHANGING_DESCRIPTION:
                    List<String> desc = tag.getDescription();
                    desc.add(message);
                    tag.setDescription(desc);
                    break;
                case CHANGING_ORDER:
                    tag.setOrder(Integer.parseInt(message));
                    break;
            }

            SupremeTags.getInstance().getTagManager().saveTag(tag);
            SupremeTags.getInstance().getTagManager().unloadTags();
            SupremeTags.getInstance().getTagManager().loadTags(true);

            SupremeTags.getInstance().getCategoryManager().initCategories();

            runTaskLater(() -> new SpecificTagMenu(SupremeTags.getMenuUtilIdentifier(player, editor.getIdentifier())).open(), 1L);

        } else {
            Tag tag = SupremeTags.getInstance().getPlayerManager().getTag(player.getUniqueId(), editor.getIdentifier());
            switch (type) {
                case CHANGING_TAG:
                    List<String> tagList = new ArrayList<>();
                    tagList.add(message);
                    tag.setTag(tagList);
                    break;
                case CHANGING_DESCRIPTION:
                    List<String> desc = tag.getDescription();
                    desc.add(message);
                    tag.setDescription(desc);
                    break;
            }

            SupremeTags.getInstance().getPlayerManager().save(tag, player);

            runTaskLater(() -> new PersonalTagEditorMenu(SupremeTags.getMenuUtilIdentifier(player, editor.getIdentifier())).open(), 1L);
        }

        SupremeTags.getInstance().getEditorList().remove(player);

        String tag_updated = messages.getString("messages.tag-updated").replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
        msgPlayer(player, tag_updated);
    }

    /**
     * Runs a task later supporting Folia & Bukkit.
     */
    private void runTaskLater(Runnable task, long delayTicks) {
        Plugin plugin = SupremeTags.getInstance();

        if (SupremeTags.getInstance().isFoliaFound()) {
            Bukkit.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin,
                    scheduledTask -> task.run(),
                    delayTicks,
                    Long.MAX_VALUE // run once effectively, could cancel immediately after if needed
            );
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }
}