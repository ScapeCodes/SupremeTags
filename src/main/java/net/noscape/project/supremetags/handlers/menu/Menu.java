package net.noscape.project.supremetags.handlers.menu;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.inventory.XInventoryView;
import net.noscape.project.supremetags.handlers.Tag;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

import static net.noscape.project.supremetags.utils.Utils.*;

public abstract class Menu implements InventoryHolder {

    protected Inventory inventory;
    protected final MenuUtil menuUtil;

    private boolean autoUpdate = false;
    private boolean updating = false;

    // Default old-style tick refresh
    private int updateInterval = 1;

    protected final Set<Integer> animatedSlots = new HashSet<>();

    public Menu(MenuUtil menuUtil) {
        this.menuUtil = menuUtil;
    }

    public abstract String getMenuName();
    public abstract int getSlots();
    public abstract void handleMenu(org.bukkit.event.inventory.InventoryClickEvent e);

    /** Subclass will build items into "inventory". */
    public abstract void setMenuItems();

    // ==================================================
    // ✅ OPEN MENU
    // ==================================================
    public void open() {
        inventory = Bukkit.createInventory(this, getSlots(), getMenuName());

        setMenuItems(); // build items

        menuUtil.getOwner().openInventory(inventory);

        if (autoUpdate) startAutoUpdate();
    }

    // ==================================================
    // ✅ DIFF-BASED REFRESH
    // ==================================================
    public void refresh() {
        if (inventory == null) return;

        // Build a temporary frame representing what setMenuItems() would produce
        Inventory temp = Bukkit.createInventory(null, inventory.getSize());
        buildVirtualFrame(temp);

        // Diff update
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack oldItem = inventory.getItem(slot);
            ItemStack newItem = temp.getItem(slot);

            if (!isSame(oldItem, newItem)) {
                inventory.setItem(slot, newItem);
            }
        }
    }

    public void refreshAnimatedTags() {
        if (inventory == null || animatedSlots.isEmpty()) return;

        // Build a virtual frame only once
        Inventory temp = Bukkit.createInventory(null, inventory.getSize());
        buildVirtualFrame(temp);

        // Update ONLY animated slots
        for (int slot : animatedSlots) {
            ItemStack oldItem = inventory.getItem(slot);
            ItemStack newItem = temp.getItem(slot);

            if (!isSame(oldItem, newItem)) {
                inventory.setItem(slot, newItem);
            }
        }
    }


    // ==================================================
    // ✅ Build "virtual" version of menu without affecting real inventory
    // ==================================================
    private void buildVirtualFrame(Inventory temp) {
        Inventory original = this.inventory;
        this.inventory = temp;

        setMenuItems();

        this.inventory = original;
    }

    // For subclasses to override if needed
    protected void buildFrame(ItemStack[] frame) {
        setMenuItems();
    }

    private boolean isSame(ItemStack a, ItemStack b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    public void updateItem(int slot, ItemStack item) {
        if (inventory != null)
            inventory.setItem(slot, item);
    }

    public void enableAutoUpdate(boolean enable) {
        this.autoUpdate = enable;
    }

    public void setUpdateInterval(int ticks) {
        this.updateInterval = Math.max(1, ticks);
    }

    // ==================================================
    // ✅ AUTO-UPDATE LOOP
    // ==================================================
    private void startAutoUpdate() {
        if (updating) return;
        updating = true;

        Player player = menuUtil.getOwner();

        Runnable loop = new Runnable() {
            @Override
            public void run() {
                if (!updating) return;

                if (player == null || !player.isOnline()
                        || !(XInventoryView.of(player.getOpenInventory()).getTopInventory().getHolder() instanceof Menu)) {
                    stopAutoUpdate();
                    return;
                }

                refreshAnimatedTags();

                runMainLater(this, updateInterval);
            }
        };

        runMainLater(loop, updateInterval);
    }

    private void stopAutoUpdate() {
        updating = false;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    // ==================================================
    // ✅ ITEM BUILDERS
    // ==================================================

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

            if (customModelData > 0)
                meta.setCustomModelData(customModelData);

            if (hideTooltip && isPaperVersionAtLeast(1, 21, 5))
                meta.setHideTooltip(true);

            meta.setLore(color(lore));
            item.setItemMeta(meta);
        }

        return item;
    }

    public void fillEmpty() {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, makeItem(XMaterial.matchXMaterial("GRAY_STAINED_GLASS_PANE").get().get(), "&6", 0, true));
            }
        }
    }
}
