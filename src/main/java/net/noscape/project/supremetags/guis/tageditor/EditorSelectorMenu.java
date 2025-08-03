package net.noscape.project.supremetags.guis.tageditor;

import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.handlers.menu.Menu;
import net.noscape.project.supremetags.handlers.menu.MenuUtil;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;

public class EditorSelectorMenu extends Menu {

    public EditorSelectorMenu(MenuUtil menuUtil) {
        super(menuUtil);
    }

    @Override
    public String getMenuName() {
        return "Which would you like to edit?";
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        int slot = e.getSlot();

        e.setCancelled(true);

        if (slot == 12) {
            new TagEditorMenu(SupremeTags.getMenuUtil(menuUtil.getOwner())).open();
        }

        if (slot == 14) {
            // open categories editor
        }
    }

    @Override
    public void setMenuItems() {
        fillEmpty();

        // tags
        this.inventory.setItem(12, makeItem(Material.NAME_TAG, "&e&lTags", 0, false));

        // categories
        this.inventory.setItem(14, makeItem(Material.BOOK, "&6&lCategories", 0, false));
    }
}