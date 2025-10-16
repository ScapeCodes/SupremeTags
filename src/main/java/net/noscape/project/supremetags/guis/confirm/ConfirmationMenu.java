package net.noscape.project.supremetags.guis.confirm;

import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.handlers.menu.Menu;
import net.noscape.project.supremetags.handlers.menu.MenuUtil;
import net.noscape.project.supremetags.utils.ItemResolver;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import static net.noscape.project.supremetags.utils.Utils.format;

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

        int acceptSlot = config.getInt("gui.confirmation-menu.items.accept.slot");
        int denySlot = config.getInt("gui.confirmation-menu.items.deny.slot");

        if (slot == acceptSlot) {
            if (action.startsWith("delete-tag:")) {
                String identifier = action.replace("delete-tag:", "");
                player.closeInventory();

                SupremeTags.getInstance().getTagManager().deleteTag(player, identifier);
            }
        }

        if (slot == denySlot) {
            player.closeInventory();
            player.sendMessage(format("&cAction cancelled."));
        }
    }

    @Override
    public void setMenuItems() {
        Player player = menuUtil.getOwner();

        // Accept item
        String acceptPath = "gui.confirmation-menu.items.accept.";
        ItemResolver.ResolvedItem acceptResolved =
                ItemResolver.resolveCustomItem(player, config.getString(acceptPath + "material"));
        ItemStack accept = acceptResolved.item();

        var meta = accept.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(format(config.getString(acceptPath + "displayname")));
            accept.setItemMeta(meta);
        }

        this.inventory.setItem(config.getInt(acceptPath + "slot"), accept);

        // Deny item
        String denyPath = "gui.confirmation-menu.items.deny.";
        ItemResolver.ResolvedItem denyResolved =
                ItemResolver.resolveCustomItem(player, config.getString(denyPath + "material"));
        ItemStack deny = denyResolved.item();

        meta = deny.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(format(config.getString(denyPath + "displayname")));
            deny.setItemMeta(meta);
        }

        this.inventory.setItem(config.getInt(denyPath + "slot"), deny);
    }
}