package net.noscape.project.supremetags.guis.tageditor;

import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.plugin.NBTAPI;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.handlers.menu.Menu;
import net.noscape.project.supremetags.handlers.menu.MenuUtil;
import net.noscape.project.supremetags.utils.ItemResolver;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import static net.noscape.project.supremetags.utils.Utils.format;

public class NewSpecificTagMenu extends Menu {

    public NewSpecificTagMenu(MenuUtil menuUtil) {
        super(menuUtil);
        enableAutoUpdate(false);
    }

    @Override
    public String getMenuName() {
        return format("Editing tag: " + menuUtil.getIdentifier());
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();

        NBTItem nbt = new NBTItem(e.getCurrentItem());


        if (nbt.hasTag("identifier")) {
            String identifier = nbt.getString("identifier");

            // handle identifier editing..
        }
    }

    @Override
    public void setMenuItems() {
        for (String name : SupremeTags.getInstance().getConfig().getConfigurationSection("gui.tag-editor-menu.editor-items").getKeys(false)) {
            String material = "";
            int slot = 0;

            ItemResolver.ResolvedItem itemResolver = ItemResolver.resolveCustomItem(menuUtil.getOwner(), material);
            ItemStack item = itemResolver.item();
            ItemMeta meta = itemResolver.meta();

            NBTItem nbt = new NBTItem(item);

            nbt.setString("name", name);
            nbt.getItem().setItemMeta(meta);

            this.getInventory().setItem(slot, nbt.getItem());
        }
    }
}