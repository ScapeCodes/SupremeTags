package net.noscape.project.supremetags.handlers.menu;

import org.bukkit.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;

import java.util.*;

import static net.noscape.project.supremetags.utils.Utils.*;

public abstract class Menu implements InventoryHolder {

    protected Inventory inventory;
    protected MenuUtil menuUtil;

    public Menu(MenuUtil menuUtil) {
        this.menuUtil = menuUtil;
    }

    public abstract String getMenuName();

    public abstract int getSlots();

    public abstract void handleMenu(InventoryClickEvent e);

    public abstract void setMenuItems();

    public void open() {
        inventory = Bukkit.createInventory(this, getSlots(), getMenuName());
        setMenuItems();
        menuUtil.getOwner().openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public ItemStack makeItem(Material material, String displayName, int custom_model_data, boolean hideTooltip, String... lore) {
        return buildItem(material, displayName, custom_model_data, hideTooltip, Arrays.asList(lore));
    }

    public ItemStack makeItem(Material material, String displayName, int custom_model_data, List<String> lore) {
        return buildItem(material, displayName, custom_model_data, false, lore);
    }

    public ItemStack makeItem(Material material, String displayName, List<String> lore) {
        return buildItem(material, displayName, 0, false, lore);
    }

    private ItemStack buildItem(Material material, String displayName, int customModelData, boolean hideTooltip, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(format(displayName));

            if (customModelData > 0) {
                meta.setCustomModelData(customModelData);
            }

            if (hideTooltip && isPaperVersionAtLeast(1, 21, 5)) {
                meta.setHideTooltip(true);
            }

            meta.setLore(color(lore));
            item.setItemMeta(meta);
        }

        return item;
    }

    public void fillEmpty() {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, makeItem(Material.GRAY_STAINED_GLASS_PANE, "&6", 0, true));
            }
        }
    }
}
