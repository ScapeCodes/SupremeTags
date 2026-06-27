package net.noscape.project.supremetags.guis.categoryeditor;

import de.tr7zw.nbtapi.NBTItem;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.guis.tageditor.EditorSelectorMenu;
import net.noscape.project.supremetags.handlers.menu.MenuUtil;
import net.noscape.project.supremetags.handlers.menu.Paged;
import net.noscape.project.supremetags.managers.CategoryManager;
import net.noscape.project.supremetags.utils.ItemResolver;
import net.noscape.project.supremetags.utils.Utils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

import static net.noscape.project.supremetags.utils.Utils.*;

public class CategoryEditorMenu extends Paged {

    private final List<String> categories;
    private FileConfiguration guis = SupremeTags.getInstance().getConfigManager().getConfig("guis.yml").get();
    private FileConfiguration catConfig = SupremeTags.getInstance().getCategoryManager().getCatConfig();

    public CategoryEditorMenu(MenuUtil menuUtil) {
        super(menuUtil);
        this.categories = new ArrayList<>(SupremeTags.getInstance().getCategoryManager().getCatorgies());
    }

    @Override
    public String getMenuName() {
        String title = format(Objects.requireNonNull(guis.getString("gui.category-editor-menu.title")).replaceAll("%page%", String.valueOf(this.getPage())));
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
        ArrayList<String> categoryList = new ArrayList<>(this.categories);
        String back = this.guis.getString("gui.items.back.displayname");
        String close = this.guis.getString("gui.items.close.displayname");
        String next = this.guis.getString("gui.items.next.displayname");
        NBTItem nbt = new NBTItem(e.getCurrentItem());

        if (nbt.hasTag("category")) {
            String category = nbt.getString("category");
            this.menuUtil.setIdentifier(category);
            (new SpecificCategoryMenu(SupremeTags.getMenuUtilIdentifier(player, category))).open();
        }

        if (e.getCurrentItem().getItemMeta().getDisplayName().equalsIgnoreCase(format(close)))
            player.closeInventory();
        if (e.getCurrentItem().getItemMeta().getDisplayName().equalsIgnoreCase(format(back))) {
            if (this.page != 0) {
                this.page--;
                open();
            } else {
                player.closeInventory();
                (new EditorSelectorMenu(SupremeTags.getMenuUtil(player))).open();
            }
        } else if (e.getCurrentItem().getItemMeta().getDisplayName().equalsIgnoreCase(format(next))) {
            if ((((categoryList.size() > this.maxItems) ? 1 : 0) & ((currentItemsOnPage >= this.maxItems) ? 1 : 0)) != 0) {
                if (this.index + 1 < categoryList.size()) {
                    this.page++;
                    open();
                } else {
                    e.setCancelled(true);
                }
            } else {
                e.setCancelled(true);
            }
        }
    }

    @Override
    public void setMenuItems() {
        getCategoriesCountOnPage();
        applyEditorLayout();
        getCategoryItems();
    }

    public void getCategoriesCountOnPage() {
        int maxItemsPerPage = guis.getInt("gui.category-editor-menu.categories-per-page");
        int startIndex = page * maxItemsPerPage;
        int endIndex = Math.min(startIndex + maxItemsPerPage, categories.size());
        this.index = startIndex;
        this.maxItems = maxItemsPerPage;
        this.currentItemsOnPage = 0;
        this.totalItems = categories.size();
    }

    public void getCategoryItems() {
        if (categories.isEmpty()) {
            return;
        }

        int maxItemsPerPage = guis.getInt("gui.category-editor-menu.categories-per-page");
        int startIndex = page * maxItemsPerPage;
        int endIndex = Math.min(startIndex + maxItemsPerPage, categories.size());

        List<String> slots = guis.getStringList("gui.category-editor-menu.slots-category.slots");

        for (int i = startIndex; i < endIndex; i++) {
            String category = categories.get(i);
            if (category == null) continue;

            String displayname = catConfig.getString("categories." + category + ".id_display", "&7" + category);
            String material = catConfig.getString("categories." + category + ".material", "NAME_TAG");
            int customModelData = catConfig.getInt("categories." + category + ".custom-model-data", 0);
            int tagCount = SupremeTags.getInstance().getCategoryManager().getCatorgiesTags().getOrDefault(category, 0);

            displayname = globalPlaceholders(menuUtil.getOwner(), displayname);

            ItemResolver.ResolvedItem resolved = ItemResolver.resolveCustomItem(menuUtil.getOwner(), material);
            ItemStack categoryItem = resolved.item();
            ItemMeta categoryMeta = resolved.meta();
            NBTItem nbt = new NBTItem(categoryItem);

            nbt.setString("category", category);

            if (categoryMeta != null) {
                categoryMeta.setCustomModelData(customModelData);
                categoryMeta.setDisplayName(format(displayname));
                categoryMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                try {
                    ItemFlag hideDye = ItemFlag.valueOf("HIDE_DYE");
                    categoryMeta.addItemFlags(hideDye);
                } catch (IllegalArgumentException ignored) {
                    // HIDE_DYE not available in this version — skip
                }
                categoryMeta.addItemFlags(ItemFlag.HIDE_DESTROYS);
                categoryMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                categoryMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            }

            List<String> lore = new ArrayList<>();
            lore.add("&8[&e➜&8] &fCategory settings can be edited by this editor.");
            lore.add("");
            lore.add("&7Identifier: &6" + category);
            lore.add("&7Tags in category: &6" + tagCount);
            lore.add("&7Material: &6" + material);
            lore.add("&7Slot: &6" + catConfig.getInt("categories." + category + ".slot", 0));
            lore.add("&7Permission: &6" + catConfig.getString("categories." + category + ".permission", "none"));
            lore.add("&7Permission See Category: &6" + catConfig.getBoolean("categories." + category + ".permission-see-category", false));
            lore.add("&7Cost Category: &6" + catConfig.getBoolean("categories." + category + ".cost-category", false));
            lore.add("&7Glow: &6" + catConfig.getBoolean("categories." + category + ".glow", false));
            lore.add("");
            lore.add("&f[&6★&f] &7Click to edit this category.");

            categoryMeta.setLore(color(lore));

            nbt.getItem().setItemMeta(categoryMeta);
            nbt.setString("category", category);

            int placementSlot;
            boolean useDefinedSlots = guis.getBoolean("gui.category-editor-menu.slots-category.enable");

            if (useDefinedSlots && currentItemsOnPage < slots.size()) {
                String rawSlot = slots.get(currentItemsOnPage);
                try {
                    placementSlot = Integer.parseInt(rawSlot);
                } catch (NumberFormatException ex) {
                    placementSlot = inventory.firstEmpty();
                }
            } else {
                placementSlot = inventory.firstEmpty();
            }

            if (placementSlot != -1) {
                inventory.setItem(placementSlot, nbt.getItem());
            }

            currentItemsOnPage++;
        }
    }
}
