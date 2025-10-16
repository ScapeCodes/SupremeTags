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

public class ConfigOneMenu extends Menu {

    private final FileConfiguration guis = SupremeTags.getInstance()
            .getConfigManager().getConfig("guis.yml").get();

    // Cross-version item definitions
    private final ItemStack LIME_DYE_ITEM = XMaterial.LIME_DYE.parseItem();
    private final ItemStack GRAY_DYE_ITEM = XMaterial.GRAY_DYE.parseItem();
    private final ItemStack WHITE_GLASS_ITEM = XMaterial.WHITE_STAINED_GLASS_PANE.parseItem();
    private final ItemStack GRAY_GLASS_ITEM = XMaterial.GRAY_STAINED_GLASS_PANE.parseItem();

    private final Material LIME_DYE = XMaterial.matchXMaterial(LIME_DYE_ITEM).get();
    private final Material GRAY_DYE = XMaterial.matchXMaterial(GRAY_DYE_ITEM).get();
    private final Material WHITE_GLASS = XMaterial.matchXMaterial(WHITE_GLASS_ITEM).get();
    private final Material GRAY_GLASS = XMaterial.matchXMaterial(GRAY_GLASS_ITEM).get();

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

        // Prevent editing filler panes
        if (Objects.requireNonNull(e.getCurrentItem()).getType()
                .equals(XMaterialUtil.match(guis.getString("gui.items.glass.material"), "STAINED_GLASS_PANE:7"))) {
            e.setCancelled(true);
            return;
        }

        // Close button
        if (e.getSlot() == 53) {
            e.setCancelled(true);
            player.closeInventory();
            return;
        }

        // Toggle buttons
        switch (e.getSlot()) {
            case 19 -> handleToggle(e, "settings.categories");
            case 21 -> handleToggle(e, "settings.cost-system");
            case 23 -> handleToggle(e, "settings.locked-view");
            case 25 -> handleToggle(e, "settings.personal-tags.enable");
            case 47 -> handleToggle(e, "settings.active-tag-glow");
            case 51 -> handleToggle(e, "settings.forced-tag");
            case 49 -> handleLayoutToggle(e);
            case 8 -> {
                if (e.getCurrentItem().getType() != Material.AIR)
                    new ConfigTwoMenu(SupremeTags.getMenuUtil(player)).open();
            }
        }
    }

    private void handleToggle(InventoryClickEvent e, String path) {
        e.setCancelled(true);
        boolean enable = e.getCurrentItem().getType().equals(GRAY_DYE);
        SupremeTags plugin = SupremeTags.getInstance();
        plugin.getConfig().set(path, enable);
        plugin.saveConfig();
        plugin.reload();
        super.open();
    }

    private void handleLayoutToggle(InventoryClickEvent e) {
        e.setCancelled(true);
        SupremeTags plugin = SupremeTags.getInstance();
        if (e.getCurrentItem().getType().equals(WHITE_GLASS)) {
            plugin.getConfig().set("settings.layout-type", "BORDER");
        } else if (e.getCurrentItem().getType().equals(GRAY_GLASS)) {
            plugin.getConfig().set("settings.layout-type", "FULL");
        }
        plugin.saveConfig();
        plugin.reload();
        super.open();
    }

    @Override
    public void setMenuItems() {
        SupremeTags plugin = SupremeTags.getInstance();

        // CATEGORIES
        List<String> cat = List.of(
                "&7Categories enhance your tagging system,",
                "&7providing an organized and professional look."
        );
        inventory.setItem(10, makeItem(Material.PAPER, format("&c&lCategories"), color(cat)));
        inventory.setItem(19, makeToggleItem("settings.categories"));

        // COST SYSTEM
        List<String> cost = List.of(
                "&7Cost System allows tags to become buyable,",
                "&7providing players to purchase tags."
        );
        inventory.setItem(12, makeItem(Material.PAPER, format("&6&lCost System"), color(cost)));
        inventory.setItem(21, makeToggleItem("settings.cost-system"));

        // LOCKED VIEW
        List<String> locked = List.of(
                "&7Locked View allows all tags to be visible in the GUI,",
                "&7letting players preview tags before unlocking them."
        );
        inventory.setItem(14, makeItem(Material.PAPER, format("&e&lLocked View"), color(locked)));
        inventory.setItem(23, makeToggleItem("settings.locked-view"));

        // PERSONAL TAGS
        List<String> personal = List.of(
                "&7Personal tags allow players to build their own tags",
                "&7with /mytags. Player limits can be set in config.yml."
        );
        inventory.setItem(16, makeItem(Material.PAPER, format("&a&lPersonal Tags"), color(personal)));
        inventory.setItem(25, makeToggleItem("settings.personal-tags.enable"));

        // ACTIVE TAG GLOW
        List<String> activeGlow = List.of(
                "&7Active Tag Glow adds an enchanted effect",
                "&7to the selected tag in the GUI."
        );
        inventory.setItem(38, makeItem(Material.PAPER, format("&b&lActive Tag Glow"), color(activeGlow)));
        inventory.setItem(47, makeToggleItem("settings.active-tag-glow"));

        // LAYOUT TYPE
        List<String> layoutType = new ArrayList<>();
        layoutType.add("&7Choose between layout styles for better aesthetics:");
        layoutType.add("");
        layoutType.add("&fOptions:");
        boolean isFull = plugin.getConfig().getString("settings.layout-type").equalsIgnoreCase("FULL");
        if (isFull) {
            layoutType.add("&a➟ &fFull");
            layoutType.add("   &7Border");
        } else {
            layoutType.add("   &7Full");
            layoutType.add("&a➟ &fBorder");
        }
        inventory.setItem(40, makeItem(Material.PAPER, format("&3&lLayout Type"), color(layoutType)));
        inventory.setItem(49, isFull
                ? makeItem(WHITE_GLASS, format("&7Type: &f&lFull"), 0, false)
                : makeItem(GRAY_GLASS, format("&7Type: &f&lBorder"), 0, false));

        // FORCED TAG
        List<String> forced = List.of(
                "&7Forced tags make the reset button disappear",
                "&7and force tags onto players automatically."
        );
        inventory.setItem(42, makeItem(Material.PAPER, format("&4&lForced Tag"), color(forced)));
        inventory.setItem(51, makeToggleItem("settings.forced-tag"));

        // Navigation items (Next / Close)
        addNavItems();
    }

    private ItemStack makeToggleItem(String configPath) {
        boolean enabled = SupremeTags.getInstance().getConfig().getBoolean(configPath);
        return enabled
                ? makeItem(LIME_DYE, format("&a&lEnabled"), 0, false)
                : makeItem(GRAY_DYE, format("&7&lDisabled"), 0, false);
    }

    private void addNavItems() {
        String closeName = guis.getString("gui.items.close.displayname");
        String closeMat = guis.getString("gui.items.close.material");
        int closeCmd = guis.getInt("gui.items.close.custom-model-data");
        List<String> closeLore = guis.getStringList("gui.items.close.lore");

        String nextName = guis.getString("gui.items.next.displayname");
        String nextMat = guis.getString("gui.items.next.material");
        int nextCmd = guis.getInt("gui.items.next.custom-model-data");
        List<String> nextLore = guis.getStringList("gui.items.next.lore");

        inventory.setItem(8, makeItem(XMaterialUtil.match(nextMat, nextMat), format(nextName), nextCmd, nextLore));
        inventory.setItem(53, makeItem(XMaterialUtil.match(closeMat, closeMat), format(closeName), closeCmd, closeLore));
    }
}
