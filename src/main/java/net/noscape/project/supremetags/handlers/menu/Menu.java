package net.noscape.project.supremetags.handlers.menu;

import com.cryptomorin.xseries.XMaterial;
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

    private int updateInterval = 10;

    protected final Set<Integer> animatedSlots = new HashSet<>();
    protected final Map<Integer, String> animatedTagSlots = new HashMap<>();

    public Menu(MenuUtil menuUtil) {
        this.menuUtil = menuUtil;
    }

    public abstract String getMenuName();
    public abstract int getSlots();
    public abstract void handleMenu(org.bukkit.event.inventory.InventoryClickEvent e);

    public abstract void setMenuItems();

    public void open() {
        inventory = Bukkit.createInventory(this, getSlots(), getMenuName());

        clearAnimatedSlots();
        setMenuItems();

        menuUtil.getOwner().openInventory(inventory);

        if (autoUpdate && !animatedSlots.isEmpty()) startAutoUpdate();
    }

    public void refresh() {
        if (inventory == null) return;

        Player player = menuUtil.getOwner();
        if (player != null
                && player.getOpenInventory().getTopInventory().getHolder() == this) {
            player.getOpenInventory().setTitle(getMenuName());
        }

        Inventory temp = Bukkit.createInventory(null, inventory.getSize());
        clearAnimatedSlots();
        buildVirtualFrame(temp);

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

        Set<Integer> slotsToRefresh = new HashSet<>(animatedSlots);
        Inventory temp = null;

        for (int slot : slotsToRefresh) {
            if (slot < 0 || slot >= inventory.getSize()) continue;

            ItemStack oldItem = inventory.getItem(slot);
            ItemStack newItem = buildAnimatedItem(slot);

            if (newItem == null) {
                if (temp == null) {
                    temp = Bukkit.createInventory(null, inventory.getSize());
                    buildVirtualFrame(temp);
                }
                newItem = temp.getItem(slot);
            }

            if (!isSame(oldItem, newItem)) {
                inventory.setItem(slot, newItem);
            }
        }
    }

    protected ItemStack buildAnimatedItem(int slot) {
        return null;
    }

    protected void clearAnimatedSlots() {
        animatedSlots.clear();
        animatedTagSlots.clear();
    }

    protected void registerAnimatedTagSlot(int slot, String identifier) {
        animatedSlots.add(slot);
        animatedTagSlots.put(slot, identifier);
    }

    private void buildVirtualFrame(Inventory temp) {
        Inventory original = this.inventory;
        this.inventory = temp;

        setMenuItems();

        this.inventory = original;
    }

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

    private void startAutoUpdate() {
        if (updating) return;
        updating = true;

        Player player = menuUtil.getOwner();

        Runnable loop = new Runnable() {
            @Override
            public void run() {
                if (!updating) return;

                if (player == null || !player.isOnline()
                        || !(player.getOpenInventory().getTopInventory().getHolder() instanceof Menu)) {
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
