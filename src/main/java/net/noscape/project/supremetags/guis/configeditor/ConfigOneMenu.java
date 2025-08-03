package net.noscape.project.supremetags.guis.configeditor;

import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.handlers.menu.Menu;
import net.noscape.project.supremetags.handlers.menu.MenuUtil;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.noscape.project.supremetags.utils.Utils.color;
import static net.noscape.project.supremetags.utils.Utils.format;

public class ConfigOneMenu extends Menu {

    private FileConfiguration guis = SupremeTags.getInstance().getConfigManager().getConfig("guis.yml").get();

    public ConfigOneMenu(MenuUtil menuUtil) {
        super(menuUtil);
    }

    @Override
    public String getMenuName() {
        return format("&8SupremeTags Configuration");
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();

        if (Objects.requireNonNull(e.getCurrentItem()).getType().equals(Material.valueOf(Objects.requireNonNull(guis.getString("gui.items.glass.material")).toUpperCase()))) {
            e.setCancelled(true);
        }

        if (e.getSlot() == 53) {
            e.setCancelled(true);
            player.closeInventory();
        }

        // categories
        if (e.getSlot() == 19) {
            if (e.getCurrentItem().getType().equals(Material.LIME_DYE)) {
                SupremeTags.getInstance().getConfig().set("settings.categories", false);
                SupremeTags.getInstance().saveConfig();

                SupremeTags.getInstance().reload();
                super.open();
            } else if (e.getCurrentItem().getType().equals(Material.GRAY_DYE)) {
                SupremeTags.getInstance().getConfig().set("settings.categories", true);
                SupremeTags.getInstance().saveConfig();

                SupremeTags.getInstance().reload();
                super.open();
            }
        }

        // cost system
        if (e.getSlot() == 21) {
            if (e.getCurrentItem().getType().equals(Material.LIME_DYE)) {
                SupremeTags.getInstance().getConfig().set("settings.cost-system", false);
                SupremeTags.getInstance().saveConfig();

                SupremeTags.getInstance().reload();
                super.open();
            } else if (e.getCurrentItem().getType().equals(Material.GRAY_DYE)) {
                SupremeTags.getInstance().getConfig().set("settings.cost-system", true);
                SupremeTags.getInstance().saveConfig();

                SupremeTags.getInstance().reload();
                super.open();
            }
        }

        // locked view
        if (e.getSlot() == 23) {
            if (e.getCurrentItem().getType().equals(Material.LIME_DYE)) {
                SupremeTags.getInstance().getConfig().set("settings.locked-view", false);
                SupremeTags.getInstance().saveConfig();

                SupremeTags.getInstance().reload();
                super.open();
            } else if (e.getCurrentItem().getType().equals(Material.GRAY_DYE)) {
                SupremeTags.getInstance().getConfig().set("settings.locked-view", true);
                SupremeTags.getInstance().saveConfig();

                SupremeTags.getInstance().reload();
                super.open();
            }
        }

        // personal tags
        if (e.getSlot() == 25) {
            if (e.getCurrentItem().getType().equals(Material.LIME_DYE)) {
                SupremeTags.getInstance().getConfig().set("settings.personal-tags.enable", false);
                SupremeTags.getInstance().saveConfig();

                SupremeTags.getInstance().reload();
                super.open();
            } else if (e.getCurrentItem().getType().equals(Material.GRAY_DYE)) {
                SupremeTags.getInstance().getConfig().set("settings.personal-tags.enable", true);
                SupremeTags.getInstance().saveConfig();

                SupremeTags.getInstance().reload();
                super.open();
            }
        }

        // active tag glow
        if (e.getSlot() == 47) {
            if (e.getCurrentItem().getType().equals(Material.LIME_DYE)) {
                SupremeTags.getInstance().getConfig().set("settings.active-tag-glow", false);
                SupremeTags.getInstance().saveConfig();

                SupremeTags.getInstance().reload();
                super.open();
            } else if (e.getCurrentItem().getType().equals(Material.GRAY_DYE)) {
                SupremeTags.getInstance().getConfig().set("settings.active-tag-glow", true);
                SupremeTags.getInstance().saveConfig();

                SupremeTags.getInstance().reload();
                super.open();
            }
        }

        // layout type
        if (e.getSlot() == 49) {
            if (e.getCurrentItem().getType().equals(Material.WHITE_STAINED_GLASS_PANE)) {
                SupremeTags.getInstance().getConfig().set("settings.layout-type", "BORDER");
                SupremeTags.getInstance().saveConfig();

                SupremeTags.getInstance().reload();

                super.open();
            } else if (e.getCurrentItem().getType().equals(Material.GRAY_STAINED_GLASS_PANE)) {
                SupremeTags.getInstance().getConfig().set("settings.layout-type", "FULL");
                SupremeTags.getInstance().saveConfig();

                SupremeTags.getInstance().reload();
                super.open();
            }
        }

        // forced tag
        if (e.getSlot() == 51) {
            if (e.getCurrentItem().getType().equals(Material.LIME_DYE)) {
                SupremeTags.getInstance().getConfig().set("settings.forced-tag", false);
                SupremeTags.getInstance().saveConfig();

                SupremeTags.getInstance().reload();
                super.open();
            } else if (e.getCurrentItem().getType().equals(Material.GRAY_DYE)) {
                SupremeTags.getInstance().getConfig().set("settings.forced-tag", true);
                SupremeTags.getInstance().saveConfig();

                SupremeTags.getInstance().reload();
                super.open();
            }
        }

        if (e.getSlot() == 8) {
            if (e.getCurrentItem().getType().equals(Material.ARROW)) {
                new ConfigTwoMenu(SupremeTags.getMenuUtil(player)).open();
            }
        }
    }

    @Override
    public void setMenuItems() {

        /// add all items needed.

        List<String> cat = new ArrayList<>();
        cat.add("&7Categories enhance your tagging system, ");
        cat.add("&7providing an organized and professional look.");
        inventory.setItem(10, makeItem(Material.PAPER, format("&c&lCategories"), color(cat)));
        if (SupremeTags.getInstance().getConfig().getBoolean("settings.categories")) {
            inventory.setItem(19, makeItem(Material.LIME_DYE, format("&a&lEnabled"), 0, false));
        } else {
            inventory.setItem(19, makeItem(Material.GRAY_DYE, format("&7&lDisabled"), 0, false));
        }

        List<String> cost = new ArrayList<>();
        cost.add("&7Cost System allows tags to become buyable, ");
        cost.add("&7providing players to purchase tags.");
        inventory.setItem(12, makeItem(Material.PAPER, format("&6&lCost System"), color(cost)));
        if (SupremeTags.getInstance().getConfig().getBoolean("settings.cost-system")) {
            inventory.setItem(21, makeItem(Material.LIME_DYE, format("&a&lEnabled"), 0, false));
        } else {
            inventory.setItem(21, makeItem(Material.GRAY_DYE, format("&7&lDisabled"), 0, false));
        }

        List<String> locked = new ArrayList<>();
        locked.add("&7Locked View allows all tags to become visible ");
        locked.add("&7in the gui, providing players to see tags before ");
        locked.add("&7unlocked them. ");
        inventory.setItem(14, makeItem(Material.PAPER, format("&e&lLocked View"), color(locked)));
        if (SupremeTags.getInstance().getConfig().getBoolean("settings.locked-view")) {
            inventory.setItem(23, makeItem(Material.LIME_DYE, format("&a&lEnabled"), 0, false));
        } else {
            inventory.setItem(23, makeItem(Material.GRAY_DYE, format("&7&lDisabled"), 0, false));
        }

        List<String> personal = new ArrayList<>();
        personal.add("&7Personal tags allows players to build their ");
        personal.add("&7own tags with /mytags, you can set player limits ");
        personal.add("&7in you config.yml. ");
        inventory.setItem(16, makeItem(Material.PAPER, format("&a&lPersonal Tags"), color(personal)));
        if (SupremeTags.getInstance().getConfig().getBoolean("settings.personal-tags.enable")) {
            inventory.setItem(25, makeItem(Material.LIME_DYE, format("&a&lEnabled"), 0, false));
        } else {
            inventory.setItem(25, makeItem(Material.GRAY_DYE, format("&7&lDisabled"), 0, false));
        }

        List<String> activeglow = new ArrayList<>();
        activeglow.add("&7Active Tag Glow allows the player's selected tag ");
        activeglow.add("&7to assign an enchanted effect in the gui, ");
        activeglow.add("&7indicating their active tag. ");
        inventory.setItem(38, makeItem(Material.PAPER, format("&b&lActive Tag Glow"), color(activeglow)));
        if (SupremeTags.getInstance().getConfig().getBoolean("settings.active-tag-glow")) {
            inventory.setItem(47, makeItem(Material.LIME_DYE, format("&a&lEnabled"), 0, false));
        } else {
            inventory.setItem(47, makeItem(Material.GRAY_DYE, format("&7&lDisabled"), 0, false));
        }

        List<String> layoutype = new ArrayList<>();
        layoutype.add("&7There are 2 layout styles you can pick from, ");
        layoutype.add("&7I designed these 2 layout types to fit ");
        layoutype.add("&7professionalism and similar experience. ");
        layoutype.add("");
        layoutype.add("&fOptions:");
        if (SupremeTags.getInstance().getConfig().getString("settings.layout-type").equalsIgnoreCase("FULL")) {
            layoutype.add("&a➟ &fFull");
            layoutype.add("   &7Border");
        } else {
            layoutype.add("   &7Full");
            layoutype.add("&a➟ &fBorder");
        }
        inventory.setItem(40, makeItem(Material.PAPER, format("&3&lLayout Type"), color(layoutype)));
        if (SupremeTags.getInstance().getConfig().getString("settings.layout-type").equalsIgnoreCase("FULL")) {
            inventory.setItem(49, makeItem(Material.WHITE_STAINED_GLASS_PANE, format("&7Type: &f&lFull"), 0, false));
        } else {
            inventory.setItem(49, makeItem(Material.GRAY_STAINED_GLASS_PANE, format("&7Type: &f&lBorder"), 0, false));
        }

        List<String> forced = new ArrayList<>();
        forced.add("&7Forced tag allow you to essentially force ");
        forced.add("&7tags upon players, this means that the reset ");
        forced.add("&7gui button will not displayed. ");
        inventory.setItem(42, makeItem(Material.PAPER, format("&4&lForced Tag"), color(forced)));
        if (SupremeTags.getInstance().getConfig().getBoolean("settings.forced-tag")) {
            inventory.setItem(51, makeItem(Material.LIME_DYE, format("&a&lEnabled"), 0, false));
        } else {
            inventory.setItem(51, makeItem(Material.GRAY_DYE, format("&7&lDisabled"), 0, false));
        }

        String close = guis.getString("gui.items.close.displayname");
        String close_material = guis.getString("gui.items.close.material");
        int close_custom_model_data = guis.getInt("gui.items.next.custom-model-data");
        List<String> close_lore = guis.getStringList("gui.items.close.lore");

        String next = guis.getString("gui.items.next.displayname");
        String next_material = guis.getString("gui.items.next.material");
        int next_custom_model_data = guis.getInt("gui.items.next.custom-model-data");
        List<String> next_lore = guis.getStringList("gui.items.next.lore");

        inventory.setItem(8, makeItem(Material.valueOf(next_material.toUpperCase()), format(next), next_custom_model_data, next_lore));
        inventory.setItem(53, makeItem(Material.valueOf(close_material.toUpperCase()), format(close), close_custom_model_data, close_lore));

        ///fillEmpty();
    }
}
