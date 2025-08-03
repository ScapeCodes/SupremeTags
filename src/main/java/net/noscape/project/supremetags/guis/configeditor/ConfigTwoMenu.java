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

public class ConfigTwoMenu extends Menu {

    private FileConfiguration guis = SupremeTags.getInstance().getConfigManager().getConfig("guis.yml").get();

    public ConfigTwoMenu(MenuUtil menuUtil) {
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

        // tag vouchers
        if (e.getSlot() == 23) {
            if (e.getCurrentItem().getType().equals(Material.LIME_DYE)) {
                SupremeTags.getInstance().getConfig().set("settings.tag-vouchers", false);
                SupremeTags.getInstance().saveConfig();

                SupremeTags.getInstance().reload();
                super.open();
            } else if (e.getCurrentItem().getType().equals(Material.GRAY_DYE)) {
                SupremeTags.getInstance().getConfig().set("settings.tag-vouchers", true);
                SupremeTags.getInstance().saveConfig();

                SupremeTags.getInstance().reload();
                super.open();
            }
        }

        // category fill empty
        if (e.getSlot() == 25) {
            if (e.getCurrentItem().getType().equals(Material.LIME_DYE)) {
                SupremeTags.getInstance().getConfig().set("categories-menu-fill-empty", false);
                SupremeTags.getInstance().saveConfig();

                SupremeTags.getInstance().reload();
                super.open();
            } else if (e.getCurrentItem().getType().equals(Material.GRAY_DYE)) {
                SupremeTags.getInstance().getConfig().set("categories-menu-fill-empty", true);
                SupremeTags.getInstance().saveConfig();

                SupremeTags.getInstance().reload();
                super.open();
            }
        }

        // update checker
        if (e.getSlot() == 47) {
            if (e.getCurrentItem().getType().equals(Material.LIME_DYE)) {
                SupremeTags.getInstance().getConfig().set("settings.update-check", false);
                SupremeTags.getInstance().saveConfig();

                SupremeTags.getInstance().reload();
                super.open();
            } else if (e.getCurrentItem().getType().equals(Material.GRAY_DYE)) {
                SupremeTags.getInstance().getConfig().set("settings.update-check", true);
                SupremeTags.getInstance().saveConfig();

                SupremeTags.getInstance().reload();
                super.open();
            }
        }

        // global tag lores
        if (e.getSlot() == 49) {
            if (e.getCurrentItem().getType().equals(Material.LIME_DYE)) {
                SupremeTags.getInstance().getConfig().set("settings.use-global-lore", false);
                SupremeTags.getInstance().saveConfig();

                SupremeTags.getInstance().reload();
                super.open();
            } else if (e.getCurrentItem().getType().equals(Material.GRAY_DYE)) {
                SupremeTags.getInstance().getConfig().set("settings.use-global-lore", true);
                SupremeTags.getInstance().saveConfig();

                SupremeTags.getInstance().reload();
                super.open();
            }
        }

        if (e.getSlot() == 0) {
            if (e.getCurrentItem().getType().equals(Material.ARROW)) {
                new ConfigOneMenu(SupremeTags.getMenuUtil(player)).open();
            }
        }
    }

    @Override
    public void setMenuItems() {

        /// add all items needed.
        List<String> tag_vouchers = new ArrayList<>();
        tag_vouchers.add("&7Tag vouchers allows players to withdraw ");
        tag_vouchers.add("&7a tag, providing the ability to gift public ");
        tag_vouchers.add("&7tags. ");
        tag_vouchers.add("");
        tag_vouchers.add("&7On withdrawing a tag, an tag voucher item ");
        tag_vouchers.add("&7is given to the player.");
        inventory.setItem(14, makeItem(Material.PAPER, format("&e&lTag Vouchers"), color(tag_vouchers)));
        if (SupremeTags.getInstance().getConfig().getBoolean("settings.tag-vouchers")) {
            inventory.setItem(23, makeItem(Material.LIME_DYE, format("&a&lEnabled"), 0, false));
        } else {
            inventory.setItem(23, makeItem(Material.GRAY_DYE, format("&7&lDisabled"), 0, false));
        }

        List<String> cat_empty = new ArrayList<>();
        cat_empty.add("&7Category Empty Fill simply fills the category ");
        cat_empty.add("&7main menu with the glass material stated in config. ");
        inventory.setItem(16, makeItem(Material.PAPER, format("&a&lCategory Empty Fill"), color(cat_empty)));
        if (SupremeTags.getInstance().getConfig().getBoolean("categories-menu-fill-empty")) {
            inventory.setItem(25, makeItem(Material.LIME_DYE, format("&a&lEnabled"), 0, false));
        } else {
            inventory.setItem(25, makeItem(Material.GRAY_DYE, format("&7&lDisabled"), 0, false));
        }

        List<String> update_check = new ArrayList<>();
        update_check.add("&7Update Checker allows admins/ops to ");
        update_check.add("&7be notified when a new update is out. ");
        inventory.setItem(38, makeItem(Material.PAPER, format("&a&lUpdate Checker"), color(update_check)));
        if (SupremeTags.getInstance().getConfig().getBoolean("settings.update-check")) {
            inventory.setItem(47, makeItem(Material.LIME_DYE, format("&a&lEnabled"), 0, false));
        } else {
            inventory.setItem(47, makeItem(Material.GRAY_DYE, format("&7&lDisabled"), 0, false));
        }

        List<String> use_global_lores = new ArrayList<>();
        use_global_lores.add("&7Global tag lores can be edit in guis.yml, ");
        use_global_lores.add("&7this makes all tags apply the same styled lores ");
        use_global_lores.add("&7without having to edit them all. ");
        inventory.setItem(40, makeItem(Material.PAPER, format("&b&lUse Global Tag Lores"), color(use_global_lores)));
        if (SupremeTags.getInstance().getConfig().getBoolean("settings.use-global-lore")) {
            inventory.setItem(49, makeItem(Material.LIME_DYE, format("&a&lEnabled"), 0, false));
        } else {
            inventory.setItem(49, makeItem(Material.GRAY_DYE, format("&7&lDisabled"), 0, false));
        }

        String back = guis.getString("gui.items.back.displayname");
        int back_custom_model_data = guis.getInt("gui.items.next.custom-model-data");
        String back_material = guis.getString("gui.items.back.material");
        List<String> back_lore = guis.getStringList("gui.items.back.lore");

        String close = guis.getString("gui.items.close.displayname");
        String close_material = guis.getString("gui.items.close.material");
        int close_custom_model_data = guis.getInt("gui.items.next.custom-model-data");
        List<String> close_lore = guis.getStringList("gui.items.close.lore");
        
        inventory.setItem(42, makeItem(Material.RED_DYE, format("&c"), 0, false));

        inventory.setItem(0, makeItem(Material.valueOf(back_material.toUpperCase()), format(back), back_custom_model_data, back_lore));
        inventory.setItem(53, makeItem(Material.valueOf(close_material.toUpperCase()), format(close), close_custom_model_data, close_lore));
    }
}