package net.noscape.project.supremetags.guis.categoryeditor;

import com.cryptomorin.xseries.XMaterial;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.enums.EditingType;
import net.noscape.project.supremetags.guis.confirm.ConfirmationMenu;
import net.noscape.project.supremetags.guis.tageditor.EditorSelectorMenu;
import net.noscape.project.supremetags.handlers.Editor;
import net.noscape.project.supremetags.handlers.menu.Menu;
import net.noscape.project.supremetags.handlers.menu.MenuUtil;
import net.noscape.project.supremetags.managers.CategoryManager;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.noscape.project.supremetags.utils.Utils.*;

public class SpecificCategoryMenu extends Menu {

    private FileConfiguration messages = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get();
    private FileConfiguration guis = SupremeTags.getInstance().getConfigManager().getConfig("guis.yml").get();
    private FileConfiguration catConfig = SupremeTags.getInstance().getCategoryManager().getCatConfig();

    public SpecificCategoryMenu(MenuUtil menuUtil) {
        super(menuUtil);
    }

    @Override
    public String getMenuName() {
        String title = format(guis.getString("gui.category-editor-menu.specific-category-editor-title").replaceAll("%identifier%", menuUtil.getIdentifier()));
        title = globalPlaceholders(menuUtil.getOwner(), title);
        return title;
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        ItemStack i = e.getCurrentItem();

        if (i == null) return;
        if (i.getItemMeta() == null) return;
        if (SupremeTags.getInstance().getEditorList().containsKey(player)) return;

        String category = messages.getString("messages.editor.category").replace("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
        String displayname = messages.getString("messages.editor.displayname").replace("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
        String material = messages.getString("messages.editor.material").replace("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
        String slot = messages.getString("messages.editor.slot").replace("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
        String permission = messages.getString("messages.editor.permission").replace("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
        String permissionSee = messages.getString("messages.editor.permission-see-category").replace("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
        String costCategory = messages.getString("messages.editor.cost-category").replace("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
        String glow = messages.getString("messages.editor.glow").replace("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
        String deleted = messages.getString("messages.editor.deleted").replace("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));

        String cat = menuUtil.getIdentifier();

        if (e.getSlot() == 13) {
            Editor editor = new Editor(cat, EditingType.CHANGING_DISPLAYNAME, false);
            SupremeTags.getInstance().getEditorList().put(player, editor);
            msgPlayer(player, displayname.replace("%current%", catConfig.getString("categories." + cat + ".id_display", cat)));
            player.closeInventory();
        } else if (e.getSlot() == 14) {
            Editor editor = new Editor(cat, EditingType.CHANGING_DESCRIPTION, false);
            SupremeTags.getInstance().getEditorList().put(player, editor);
            msgPlayer(player, category.replace("%current%", cat));
            player.closeInventory();
        } else if (e.getSlot() == 15) {
            Editor editor = new Editor(cat, EditingType.CHANGING_MATERIAL, false);
            SupremeTags.getInstance().getEditorList().put(player, editor);
            msgPlayer(player, material.replace("%current%", catConfig.getString("categories." + cat + ".material", "NAME_TAG")));
            player.closeInventory();
        } else if (e.getSlot() == 16) {
            Editor editor = new Editor(cat, EditingType.CHANGING_SLOT, false);
            SupremeTags.getInstance().getEditorList().put(player, editor);
            msgPlayer(player, slot.replace("%current%", String.valueOf(catConfig.getInt("categories." + cat + ".slot", 0))));
            player.closeInventory();
        } else if (e.getSlot() == 22) {
            Editor editor = new Editor(cat, EditingType.CHANGING_PERMISSION, false);
            SupremeTags.getInstance().getEditorList().put(player, editor);
            msgPlayer(player, permission.replace("%current%", catConfig.getString("categories." + cat + ".permission", "none")));
            player.closeInventory();
        } else if (e.getSlot() == 23) {
            Editor editor = new Editor(cat, EditingType.CHANGING_PERMISSION_SEE_CATEGORY, false);
            SupremeTags.getInstance().getEditorList().put(player, editor);
            msgPlayer(player, permissionSee.replace("%current%", String.valueOf(catConfig.getBoolean("categories." + cat + ".permission-see-category", false))));
            player.closeInventory();
        } else if (e.getSlot() == 24) {
            Editor editor = new Editor(cat, EditingType.CHANGING_COST_CATEGORY, false);
            SupremeTags.getInstance().getEditorList().put(player, editor);
            msgPlayer(player, costCategory.replace("%current%", String.valueOf(catConfig.getBoolean("categories." + cat + ".cost-category", false))));
            player.closeInventory();
        } else if (e.getSlot() == 31) {
            Editor editor = new Editor(cat, EditingType.CHANGING_GLOW, false);
            SupremeTags.getInstance().getEditorList().put(player, editor);
            msgPlayer(player, glow.replace("%current%", String.valueOf(catConfig.getBoolean("categories." + cat + ".glow", false))));
            player.closeInventory();
        } else if (e.getSlot() == 49) {
            String identifier = menuUtil.getIdentifier();
            player.closeInventory();
            new ConfirmationMenu(SupremeTags.getMenuUtil(player), "delete-category:" + identifier).open();
        } else {
            e.setCancelled(true);
        }
    }

    @Override
    public void setMenuItems() {
        if (menuUtil.getIdentifier() != null) {
            if (SupremeTags.getInstance().getCategoryManager().isCategory(menuUtil.getIdentifier())) {
                String cat = menuUtil.getIdentifier();
                int tagCount = SupremeTags.getInstance().getCategoryManager().getCatorgiesTags().getOrDefault(cat, 0);

                List<String> lore = new ArrayList<>();

                lore.add("&8[&e➜&8] &fAll category settings can be edited by this editor.");
                lore.add("&8[&e➜&8] &fYou can also edit the categories in the categories.yml and then reload.");
                lore.add("");
                lore.add("&7Identifier: &6" + cat);
                lore.add("&7Display Name: &6" + catConfig.getString("categories." + cat + ".id_display", cat));
                lore.add("&7Material: &6" + catConfig.getString("categories." + cat + ".material", "NAME_TAG"));
                lore.add("&7Slot: &6" + catConfig.getInt("categories." + cat + ".slot", 0));
                lore.add("&7Permission: &6" + catConfig.getString("categories." + cat + ".permission", "none"));
                lore.add("&7Permission See Category: &6" + catConfig.getBoolean("categories." + cat + ".permission-see-category", false));
                lore.add("&7Cost Category: &6" + catConfig.getBoolean("categories." + cat + ".cost-category", false));
                lore.add("&7Glow: &6" + catConfig.getBoolean("categories." + cat + ".glow", false));
                lore.add("&7Tags in category: &6" + tagCount);
                lore.add("");
                lore.add("&f[&6★&f] &7Use the items on the right to change specific settings/values.");

                String displayname = catConfig.getString("categories." + cat + ".id_display", "&7" + cat);
                getInventory().setItem(19, makeItem(Material.BOOK, format(displayname), lore));

                List<String> c_displayname = new ArrayList<>();
                c_displayname.add("&7Current: &6" + catConfig.getString("categories." + cat + ".id_display", cat));
                c_displayname.add("");
                c_displayname.add("&8[&e➜&8] &fThe display name of the category in menus.");
                c_displayname.add("");
                c_displayname.add("&f[&6★&f] &eClick to change!");
                getInventory().setItem(13, makeItem(Material.NAME_TAG, format("&e&lChange Display Name"), c_displayname));

                List<String> c_identifier = new ArrayList<>();
                c_identifier.add("&7Current: &6" + cat);
                c_identifier.add("");
                c_identifier.add("&8[&e➜&8] &fThe identifier of the category (used in configs).");
                c_identifier.add("");
                c_identifier.add("&f[&6★&f] &eClick to change!");
                getInventory().setItem(14, makeItem(Material.PAPER, format("&e&lChange Identifier"), c_identifier));

                List<String> c_material = new ArrayList<>();
                c_material.add("&7Current: &6" + catConfig.getString("categories." + cat + ".material", "NAME_TAG"));
                c_material.add("");
                c_material.add("&8[&e➜&8] &fThe material used for the category item in menus.");
                c_material.add("");
                c_material.add("&f[&6★&f] &eClick to change!");
                getInventory().setItem(15, makeItem(Material.GRASS_BLOCK, format("&e&lChange Material"), c_material));

                List<String> c_slot = new ArrayList<>();
                c_slot.add("&7Current: &6" + catConfig.getInt("categories." + cat + ".slot", 0));
                c_slot.add("");
                c_slot.add("&8[&e➜&8] &fThe slot this category item appears in the main menu.");
                c_slot.add("");
                c_slot.add("&f[&6★&f] &eClick to change!");
                getInventory().setItem(16, makeItem(Material.ARROW, format("&e&lChange Slot"), c_slot));

                List<String> c_perm = new ArrayList<>();
                c_perm.add("&7Current: &6" + catConfig.getString("categories." + cat + ".permission", "none"));
                c_perm.add("");
                c_perm.add("&8[&e➜&8] &fThe permission required to open this category menu.");
                c_perm.add("");
                c_perm.add("&f[&6★&f] &eClick to change!");
                getInventory().setItem(22, makeItem(Material.REDSTONE_TORCH, format("&e&lChange Permission"), c_perm));

                List<String> c_permSee = new ArrayList<>();
                c_permSee.add("&7Current: &6" + catConfig.getBoolean("categories." + cat + ".permission-see-category", false));
                c_permSee.add("");
                c_permSee.add("&8[&e➜&8] &fIf true, players without permission won't see this category.");
                c_permSee.add("");
                c_permSee.add("&f[&6★&f] &eClick to change!");
                getInventory().setItem(23, makeItem(Material.ENDER_EYE, format("&e&lPermission See Category"), c_permSee));

                List<String> c_cost = new ArrayList<>();
                c_cost.add("&7Current: &6" + catConfig.getBoolean("categories." + cat + ".cost-category", false));
                c_cost.add("");
                c_cost.add("&8[&e➜&8] &fIf true, tags in this category require economy to purchase.");
                c_cost.add("");
                c_cost.add("&f[&6★&f] &eClick to change!");
                getInventory().setItem(24, makeItem(Material.SUNFLOWER, format("&e&lCost Category"), c_cost));

                List<String> c_glow = new ArrayList<>();
                c_glow.add("&7Current: &6" + catConfig.getBoolean("categories." + cat + ".glow", false));
                c_glow.add("");
                c_glow.add("&8[&e➜&8] &fIf true, the category item will glow in the menu.");
                c_glow.add("");
                c_glow.add("&f[&6★&f] &eClick to change!");
                getInventory().setItem(31, makeItem(Material.GLOWSTONE, format("&e&lGlow"), c_glow));

                getInventory().setItem(25, makeItem(Material.BARRIER, format("&c&lComing Soon!"), 0, false));
                getInventory().setItem(32, makeItem(Material.BARRIER, format("&c&lComing Soon!"), 0, false));
                getInventory().setItem(33, makeItem(Material.BARRIER, format("&c&lComing Soon!"), 0, false));
                getInventory().setItem(34, makeItem(Material.BARRIER, format("&c&lComing Soon!"), 0, false));

                List<String> c_delete = new ArrayList<>();
                c_delete.add("&7This cannot be undone!");
                getInventory().setItem(49, makeItem(Material.RED_WOOL, format("&c&lDelete Category"), c_delete));
            }
        }

        // Apply border layout consistent with other menus
        String layout = SupremeTags.getInstance().getLayout();
        if (layout == null) {
            return;
        }

        if (layout.equalsIgnoreCase("FULL")) {
            if (SupremeTags.getInstance().getConfig().getBoolean("gui.items.glass.enable")) {
                for (int i = 36; i <= 44; i++) {
                    String item_material = guis.getString("gui.items.glass.material");
                    String item_displayname = guis.getString("gui.items.glass.displayname");
                    int item_custom_model_data = guis.getInt("gui.items.glass.custom-model-data");

                    boolean hideToolTip = guis.getBoolean("gui.items.glass.hide-tooltip");

                    if (item_material != null) {
                        getInventory().setItem(i, makeItem(XMaterial.matchXMaterial(item_material.toUpperCase()).get().get(), item_displayname, item_custom_model_data, hideToolTip));
                    }
                }
            }
        } else if (layout.equalsIgnoreCase("BORDER")) {
            for (int i = 0; i < 54; i++) {
                if (getInventory().getItem(i) == null) {
                    if (i < 9 || i >= 45 || i % 9 == 0 || (i + 1) % 9 == 0) {
                        String item_material = guis.getString("gui.items.glass.material");
                        String item_displayname = guis.getString("gui.items.glass.displayname");
                        int item_custom_model_data = guis.getInt("gui.items.glass.custom-model-data");

                        boolean hideToolTip = guis.getBoolean("gui.items.glass.hide-tooltip");

                        if (item_material != null) {
                            getInventory().setItem(i, makeItem(XMaterial.matchXMaterial(item_material.toUpperCase()).get().get(), item_displayname, item_custom_model_data, hideToolTip));
                        }
                    }
                }
            }
        }
    }
}
