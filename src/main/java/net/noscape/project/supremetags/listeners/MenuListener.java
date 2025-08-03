package net.noscape.project.supremetags.listeners;

import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.handlers.menu.Menu;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;

public class MenuListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getClickedInventory() != null) {
            InventoryHolder holder = e.getClickedInventory().getHolder();

            Player player = (Player) e.getWhoClicked();

            if (SupremeTags.getInstance().getMenuUtil().containsKey(player) && e.getClickedInventory() == e.getWhoClicked().getInventory()) {
                e.setCancelled(true); // Cancel the event to prevent any actions within the player's inventory
                return;
            }

            if (holder instanceof Menu) {
                e.setCancelled(true);

                if (e.isShiftClick()) {
                    e.setCancelled(true); // Cancel the event to prevent shift-clicking into the menu
                    return;
                }

                if (e.getCurrentItem() == null) {
                    return;
                }

                Menu menu = (Menu) holder;
                menu.handleMenu(e);
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();

        if (e.getInventory().getHolder() != null) {
            InventoryHolder holder = e.getInventory().getHolder();

            if (holder instanceof Menu) {
                SupremeTags.getInstance().getMenuUtil().remove(p);
            }
        }
    }
}