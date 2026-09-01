package net.noscape.project.supremetags.guis.confirm;

import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.api.events.TagAssignEvent;
import net.noscape.project.supremetags.handlers.menu.Menu;
import net.noscape.project.supremetags.handlers.menu.MenuUtil;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.storage.UserData;
import net.noscape.project.supremetags.utils.ItemResolver;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.noscape.project.supremetags.utils.Utils.format;
import static net.noscape.project.supremetags.utils.Utils.msgPlayer;
import static net.noscape.project.supremetags.utils.Utils.playConfigSound;
import static net.noscape.project.supremetags.utils.Utils.replacePlaceholders;

public class ConfirmationMenu extends Menu {

    private final FileConfiguration config =
            SupremeTags.getInstance().getConfigManager().getConfig("guis.yml").get();
    private final String action;

    public ConfirmationMenu(MenuUtil menuUtil, String action) {
        super(menuUtil);
        this.action = action;
    }

    @Override
    public String getMenuName() {
        return format(config.getString("gui.confirmation-menu.title"));
    }

    @Override
    public int getSlots() {
        return config.getInt("gui.confirmation-menu.size");
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        e.setCancelled(true);

        int slot = e.getRawSlot();

        if (config.getIntegerList("gui.confirmation-menu.items.accept.slots").contains(slot)) {
            player.closeInventory();
            runConfirmedAction(player);
            return;
        }

        if (config.getIntegerList("gui.confirmation-menu.items.deny.slots").contains(slot)) {
            player.closeInventory();
            player.sendMessage(format("&cAction cancelled."));
        }
    }

    @Override
    public void setMenuItems() {
        Player player = menuUtil.getOwner();

        setConfiguredItem(player, "gui.confirmation-menu.items.accept.");
        setConfiguredItem(player, "gui.confirmation-menu.items.divider.");
        setConfiguredItem(player, "gui.confirmation-menu.items.deny.");
    }

    private void setConfiguredItem(Player player, String path) {
        ItemResolver.ResolvedItem resolved =
                ItemResolver.resolveCustomItem(player, config.getString(path + "material"));
        ItemStack item = resolved.item();

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(format(config.getString(path + "displayname")));

            int customModelData = config.getInt(path + "custom-model-data");
            if (customModelData > 0) {
                meta.setCustomModelData(customModelData);
            }

            meta.setLore(formatLore(config.getStringList(path + "lore")));
            item.setItemMeta(meta);
        }

        for (int slot : config.getIntegerList(path + "slots")) {
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, item.clone());
            }
        }
    }

    private List<String> formatLore(List<String> lore) {
        List<String> formatted = new ArrayList<>();
        for (String line : lore) {
            formatted.add(format(applyPlaceholders(line)));
        }
        return formatted;
    }

    private String applyPlaceholders(String line) {
        Tag tag = getActionTag();
        String identifier = getActionIdentifier();
        String tagText = tag == null ? identifier : tag.getCurrentTag();
        String cost = tag == null ? "0" : String.valueOf(tag.getEcoAmount());

        return line
                .replace("%action%", getActionLabel())
                .replace("%target%", getActionTarget())
                .replace("%tag%", tagText == null ? "" : tagText)
                .replace("%identifier%", identifier == null ? "" : identifier)
                .replace("%cost%", cost);
    }

    private void runConfirmedAction(Player player) {
        if (action.startsWith("delete-tag:")) {
            SupremeTags.getInstance().getTagManager().deleteTag(player, action.substring("delete-tag:".length()));
        } else if (action.startsWith("delete-category:")) {
            SupremeTags.getInstance().getCategoryManager().deleteCategory(action.substring("delete-category:".length()));
        } else if (action.startsWith("move-tag:")) {
            String[] parts = action.substring("move-tag:".length()).split("\\|", 2);
            if (parts.length == 2) {
                SupremeTags.getInstance().getTagManager().moveTag(player, parts[0], parts[1]);
            }
        } else if (action.startsWith("set-everyone:")) {
            String identifier = action.substring("set-everyone:".length());
            int updated = UserData.setActiveForEveryone(identifier);
            msgPlayer(player, message("messages.set-everyone-command")
                    .replace("%identifier%", identifier)
                    .replace("%updated%", String.valueOf(updated)));
        } else if (action.equals("reset-everyone")) {
            int updated = UserData.setActiveForEveryone("None");
            msgPlayer(player, message("messages.reset-everyone-command")
                    .replace("%updated%", String.valueOf(updated)));
        } else if (action.startsWith("select-tag:")) {
            assignTag(player, action.substring("select-tag:".length()));
        } else if (action.startsWith("purchase-tag:")) {
            Tag tag = SupremeTags.getInstance().getTagManager().getTag(action.substring("purchase-tag:".length()));
            net.noscape.project.supremetags.utils.Utils.purchaseTag(player, tag);
        }
    }

    private void assignTag(Player player, String identifier) {
        Tag tag = SupremeTags.getInstance().getTagManager().getTag(identifier);
        if (tag == null) return;

        TagAssignEvent tagEvent = new TagAssignEvent(player, identifier, false);
        Bukkit.getPluginManager().callEvent(tagEvent);
        if (tagEvent.isCancelled()) return;

        String activeTag = UserData.getActive(player.getUniqueId());
        if (!activeTag.equalsIgnoreCase("none")) {
            Tag oldTag = SupremeTags.getInstance().getTagManager().getTag(activeTag);
            if (oldTag != null) oldTag.removeEffects(player);
        }

        UserData.setActive(player, tagEvent.getTag());
        tag.applyEffects(player);

        if (SupremeTags.getInstance().getConfig().getBoolean("settings.gui-messages")) {
            String select = message("messages.tag-select-message");
            select = replacePlaceholders(player, select);
            msgPlayer(player, select
                    .replace("%identifier%", identifier)
                    .replace("%tag%", tag.getTag().getFirst()));
            playConfigSound(player, "selected-tag");
        }
    }

    private String message(String path) {
        FileConfiguration messages = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get();
        return Objects.requireNonNullElse(messages.getString(path), "")
                .replace("%prefix%", Objects.requireNonNullElse(messages.getString("messages.prefix"), ""));
    }

    private Tag getActionTag() {
        String identifier = getActionIdentifier();
        return identifier == null || identifier.isBlank()
                ? null
                : SupremeTags.getInstance().getTagManager().getTag(identifier);
    }

    private String getActionIdentifier() {
        if (action.startsWith("delete-tag:")) return action.substring("delete-tag:".length());
        if (action.startsWith("move-tag:")) return action.substring("move-tag:".length()).split("\\|", 2)[0];
        if (action.startsWith("set-everyone:")) return action.substring("set-everyone:".length());
        if (action.startsWith("select-tag:")) return action.substring("select-tag:".length());
        if (action.startsWith("purchase-tag:")) return action.substring("purchase-tag:".length());
        if (action.startsWith("delete-category:")) return action.substring("delete-category:".length());
        return "";
    }

    private String getActionLabel() {
        if (action.startsWith("delete-tag:")) return "Delete Tag";
        if (action.startsWith("delete-category:")) return "Delete Category";
        if (action.startsWith("move-tag:")) return "Move Tag";
        if (action.startsWith("set-everyone:")) return "Set Everyone";
        if (action.equals("reset-everyone")) return "Reset Everyone";
        if (action.startsWith("select-tag:")) return "Select Tag";
        if (action.startsWith("purchase-tag:")) return "Purchase Tag";
        return "Confirm Action";
    }

    private String getActionTarget() {
        if (action.startsWith("move-tag:")) {
            String[] parts = action.substring("move-tag:".length()).split("\\|", 2);
            return parts.length == 2 ? parts[1] : "";
        }

        return getActionIdentifier();
    }
}
