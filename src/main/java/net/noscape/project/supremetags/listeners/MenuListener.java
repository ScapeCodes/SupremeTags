package net.noscape.project.supremetags.listeners;

import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.handlers.menu.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MenuListener implements Listener {

    public static Inventory getTopInventory(InventoryEvent event) {
        try {
            Object view = event.getView();
            Method getTopInventory = view.getClass().getMethod("getTopInventory");
            getTopInventory.setAccessible(true);
            return (Inventory) getTopInventory.invoke(view);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;

        Inventory topInventory = getTopInventory(e);
        if (topInventory == null) return;

        InventoryHolder topHolder = topInventory.getHolder();
        if (topHolder instanceof Menu menu) {
            e.setCancelled(true);

            if (e.getClickedInventory() != null && e.getClickedInventory().equals(topInventory)) {
                ItemStack clickedItem = e.getCurrentItem();
                if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                    menu.handleMenu(e);
                }
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;

        Inventory topInventory = getTopInventory(e);
        if (topInventory == null) return;

        InventoryHolder topHolder = topInventory.getHolder();
        if (topHolder instanceof Menu) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();

        if (e.getInventory().getHolder() instanceof Menu) {
            SupremeTags.getInstance().getMenuUtil().remove(p);
        }
    }
}
