package net.noscape.project.supremetags.guis.configeditor;

import com.cryptomorin.xseries.XMaterial;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.handlers.menu.Menu;
import net.noscape.project.supremetags.handlers.menu.MenuUtil;
import net.noscape.project.supremetags.utils.XMaterialUtil;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.noscape.project.supremetags.utils.Utils.color;
import static net.noscape.project.supremetags.utils.Utils.format;

public class ConfigTwoMenu extends Menu {

    private final FileConfiguration guis = SupremeTags.getInstance()
            .getConfigManager().getConfig("guis.yml").get();

    private final ItemStack LIME_DYE_ITEM = XMaterial.LIME_DYE.parseItem();
    private final ItemStack GRAY_DYE_ITEM = XMaterial.GRAY_DYE.parseItem();
    private final Material LIME_DYE = XMaterial.matchXMaterial(LIME_DYE_ITEM).get();
    private final Material GRAY_DYE = XMaterial.matchXMaterial(GRAY_DYE_ITEM).get();

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
        ItemStack item = e.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        // Prevent editing glass filler
        if (item.getType().equals(XMaterialUtil.match(
                guis.getString("gui.items.glass.material"),
                "STAINED_GLASS_PANE:7"
        ))) {
            e.setCancelled(true);
            return;
        }

        int slot = e.getSlot();
        e.setCancelled(true);

        SupremeTags plugin = SupremeTags.getInstance();

        switch (slot) {
            case 53 -> player.closeInventory();
            case 0 -> new ConfigOneMenu(SupremeTags.getMenuUtil(player)).open();
            case 20 -> toggleConfig(plugin, "settings.tag-vouchers", item);
            case 22 -> toggleConfigFile("categories.yml", "categories-menu-fill-empty", item);
            case 24 -> toggleConfigFile("guis.yml", "gui.tag-actions-menu.enable", item);
            case 48 -> toggleConfig(plugin, "settings.update-check", item);
            case 50 -> toggleConfig(plugin, "settings.use-global-lore", item);
        }
    }

    private void toggleConfig(SupremeTags plugin, String path, ItemStack clickedItem) {
        boolean enable = clickedItem.getType().equals(GRAY_DYE);
        plugin.getConfig().set(path, enable);
        plugin.saveConfig();
        plugin.reload();
        super.refresh();
    }

    private void toggleConfigFile(String file, String path, ItemStack clickedItem) {
        boolean enable = clickedItem.getType().equals(GRAY_DYE);
        var manager = SupremeTags.getInstance().getConfigManager();
        manager.getConfig(file).get().set(path, enable);
        manager.saveConfig(file);
        manager.reloadConfig(file);
        super.refresh();
    }

    @Override
    public void setMenuItems() {
        SupremeTags plugin = SupremeTags.getInstance();

        // Tag Vouchers
        List<String> tagVouchers = List.of(
                "&7Tag vouchers allow players to withdraw a tag,",
                "&7providing the ability to gift public tags.",
                "",
                "&7On withdrawing a tag, a voucher item is given."
        );
        inventory.setItem(11, makeItem(Material.PAPER, format("&e&lTag Vouchers"), color(tagVouchers)));
        inventory.setItem(20, makeToggleItem(plugin.getConfig().getBoolean("settings.tag-vouchers")));

        // Category Empty Fill
        List<String> catEmpty = List.of(
                "&7Category Empty Fill fills the main category menu",
                "&7with the glass material defined in config."
        );
        inventory.setItem(13, makeItem(Material.PAPER, format("&a&lCategory Empty Fill"), color(catEmpty)));
        inventory.setItem(22, makeToggleItem(plugin.getConfigManager()
                .getConfig("categories.yml").get().getBoolean("categories-menu-fill-empty")));

        // Tag Actions Menu
        List<String> tagActions = List.of(
                "&7Tag Actions Menu lets players use interactive tag actions,",
                "&7including Bedrock-supported click actions."
        );
        inventory.setItem(15, makeItem(Material.PAPER, format("&a&lTag Actions Menu"), color(tagActions)));
        inventory.setItem(24, makeToggleItem(plugin.getConfigManager()
                .getConfig("guis.yml").get().getBoolean("gui.tag-actions-menu.enable")));

        // Update Checker
        List<String> updateCheck = List.of(
                "&7Update Checker notifies admins when a new",
                "&7version of SupremeTags is available."
        );
        inventory.setItem(39, makeItem(Material.PAPER, format("&a&lUpdate Checker"), color(updateCheck)));
        inventory.setItem(48, makeToggleItem(plugin.getConfig().getBoolean("settings.update-check")));

        // Global Tag Lores
        List<String> globalLores = List.of(
                "&7Global Tag Lores apply the same lore style to all tags.",
                "&7Editable in guis.yml, removing the need to edit each tag manually."
        );
        inventory.setItem(41, makeItem(Material.PAPER, format("&b&lUse Global Tag Lores"), color(globalLores)));
        inventory.setItem(50, makeToggleItem(plugin.getConfig().getBoolean("settings.use-global-lore")));

        // Navigation Buttons
        addNavItems();
    }

    private ItemStack makeToggleItem(boolean enabled) {
        return enabled
                ? makeItem(LIME_DYE, format("&a&lEnabled"), 0, false)
                : makeItem(GRAY_DYE, format("&7&lDisabled"), 0, false);
    }

    private void addNavItems() {
        String backName = guis.getString("gui.items.back.displayname");
        String backMat = guis.getString("gui.items.back.material");
        int backCmd = guis.getInt("gui.items.back.custom-model-data");
        List<String> backLore = guis.getStringList("gui.items.back.lore");

        String closeName = guis.getString("gui.items.close.displayname");
        String closeMat = guis.getString("gui.items.close.material");
        int closeCmd = guis.getInt("gui.items.close.custom-model-data");
        List<String> closeLore = guis.getStringList("gui.items.close.lore");

        inventory.setItem(0, makeItem(XMaterialUtil.match(backMat, backMat), format(backName), backCmd, backLore));
        inventory.setItem(53, makeItem(XMaterialUtil.match(closeMat, closeMat), format(closeName), closeCmd, closeLore));
    }
}
