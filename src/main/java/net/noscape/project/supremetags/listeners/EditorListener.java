package net.noscape.project.supremetags.listeners;

import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.enums.EditingType;
import net.noscape.project.supremetags.guis.categoryeditor.SpecificCategoryMenu;
import net.noscape.project.supremetags.guis.personaltags.PersonalTagEditorMenu;
import net.noscape.project.supremetags.guis.tageditor.SpecificTagMenu;
import net.noscape.project.supremetags.handlers.Editor;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.utils.Utils;
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

import static net.noscape.project.supremetags.utils.Utils.deformat;
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
        String deformat_message = deformat(message);
        Editor editor = SupremeTags.getInstance().getEditorList().get(player);
        EditingType type = editor.getType();

        e.setCancelled(true);

        // === Handle cancel input ===
        if (deformat_message.equalsIgnoreCase("cancel")) {
            SupremeTags.getInstance().removeEditor(player);

            // Return to correct menu depending on edit type
            if (!editor.isPersonalEdit()) {
                Tag tag = SupremeTags.getInstance().getTagManager().getTag(editor.getIdentifier());
                if (tag != null) {
                    runTaskLater(() -> new SpecificTagMenu(
                            SupremeTags.getMenuUtilIdentifier(player, editor.getIdentifier())).open(), 1L);
                } else {
                    runTaskLater(() -> new SpecificCategoryMenu(
                            SupremeTags.getMenuUtilIdentifier(player, editor.getIdentifier())).open(), 1L);
                }
            } else {
                runTaskLater(() -> new PersonalTagEditorMenu(
                        SupremeTags.getMenuUtilIdentifier(player, editor.getIdentifier())).open(), 1L);
            }

            String cancelled = messages.getString("messages.editor.cancelled", "%prefix% &7edit cancelled, no changes have taken place.")
                    .replace("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
            msgPlayer(player, cancelled);
            return;
        } else {

            // === Continue normal editing ===
            if (!editor.isPersonalEdit()) {
                Tag tag = SupremeTags.getInstance().getTagManager().getTag(editor.getIdentifier());
                if (tag != null) {
                    switch (type) {
                        case CHANGING_TAG:
                            List<String> tagList = tag.getTag();
                            tagList.add(message);
                            tag.setTag(tagList);
                            break;
                        case CHANGING_PERMISSION:
                            tag.setPermission(deformat_message);
                            break;
                        case CHANGING_CATEGORY:
                            tag.setCategory(deformat_message);
                            break;
                        case CHANGING_RARITY:
                            tag.setRarity(deformat_message);
                            break;
                        case CHANGING_COST:
                            tag.getEconomy().setAmount(Double.parseDouble(deformat_message));
                            break;
                        case CHANGING_DESCRIPTION:
                            List<String> desc = tag.getDescription();
                            desc.add(message);
                            tag.setDescription(desc);
                            break;
                        case CHANGING_ORDER:
                            tag.setOrder(Integer.parseInt(deformat_message));
                            break;
                        default:
                            break;
                    }

                    SupremeTags.getInstance().getTagManager().saveTag(tag);
                    SupremeTags.getInstance().getTagManager().unloadTags();
                    SupremeTags.getInstance().getTagManager().loadTags(true);
                    SupremeTags.getInstance().getCategoryManager().initCategories();

                    runTaskLater(() -> new SpecificTagMenu(
                            SupremeTags.getMenuUtilIdentifier(player, editor.getIdentifier())).open(), 1L);
                } else {
                    // Category editing
                    String category = editor.getIdentifier();
                    FileConfiguration catConfig = SupremeTags.getInstance().getCategoryManager().getCatConfig();
                    switch (type) {
                        case CHANGING_DISPLAYNAME:
                            catConfig.set("categories." + category + ".id_display", message);
                            break;
                        case CHANGING_DESCRIPTION:
                            catConfig.set("categories." + category + ".description", message);
                            break;
                        case CHANGING_MATERIAL:
                            catConfig.set("categories." + category + ".material", deformat_message.toUpperCase());
                            break;
                        case CHANGING_SLOT:
                            try {
                                int slotValue = Integer.parseInt(deformat_message);
                                catConfig.set("categories." + category + ".slot", slotValue);
                            } catch (NumberFormatException ex) {
                                msgPlayer(player, messages.getString("messages.editor.invalid-number")
                                        .replace("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix"))));
                            }
                            break;
                        case CHANGING_PERMISSION:
                            catConfig.set("categories." + category + ".permission", deformat_message);
                            break;
                        case CHANGING_PERMISSION_SEE_CATEGORY:
                            catConfig.set("categories." + category + ".permission-see-category", Boolean.parseBoolean(deformat_message));
                            break;
                        case CHANGING_COST_CATEGORY:
                            catConfig.set("categories." + category + ".cost-category", Boolean.parseBoolean(deformat_message));
                            break;
                        case CHANGING_GLOW:
                            catConfig.set("categories." + category + ".glow", Boolean.parseBoolean(deformat_message));
                            break;
                        default:
                            break;
                    }

                    SupremeTags.getInstance().getConfigManager().saveConfig("categories.yml");
                    SupremeTags.getInstance().getCategoryManager().initCategories();

                    runTaskLater(() -> new SpecificCategoryMenu(
                            SupremeTags.getMenuUtilIdentifier(player, category)).open(), 1L);
                }

            } else {
                Tag tag = SupremeTags.getInstance().getPlayerManager()
                        .getTag(player.getUniqueId(), editor.getIdentifier());
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
                runTaskLater(() -> new PersonalTagEditorMenu(
                        SupremeTags.getMenuUtilIdentifier(player, editor.getIdentifier())).open(), 1L);
            }

            SupremeTags.getInstance().removeEditor(player);

            String tag_updated = messages.getString("messages.tag-updated")
                    .replace("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
            msgPlayer(player, tag_updated);
        }
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
            Utils.runMainLater(task, delayTicks);
        }
    }
}