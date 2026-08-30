package net.noscape.project.supremetags.guis.tageditor;

import com.cryptomorin.xseries.XMaterial;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.enums.EditingType;
import net.noscape.project.supremetags.handlers.Editor;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.handlers.menu.Menu;
import net.noscape.project.supremetags.handlers.menu.MenuUtil;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.noscape.project.supremetags.utils.Utils.format;
import static net.noscape.project.supremetags.utils.Utils.msgPlayer;

public class VoucherEditorMenu extends Menu {

    private final FileConfiguration messages = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get();
    private final FileConfiguration guis = SupremeTags.getInstance().getConfigManager().getConfig("guis.yml").get();

    public VoucherEditorMenu(MenuUtil menuUtil) {
        super(menuUtil);
    }

    @Override
    public String getMenuName() {
        return format("&8Voucher Editor: &e" + menuUtil.getIdentifier());
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        if (e.getCurrentItem() == null || e.getCurrentItem().getItemMeta() == null) return;
        if (SupremeTags.getInstance().getEditorList().containsKey(player.getUniqueId())) return;

        Tag tag = SupremeTags.getInstance().getTagManager().getTag(menuUtil.getIdentifier());
        if (tag == null) return;

        if (e.getSlot() == 20) {
            startChatEdit(player, tag, EditingType.CHANGING_VOUCHER_DISPLAYNAME, message("messages.editor.voucher-displayname").replace("%current%", tag.getVoucherDisplayName()));
        } else if (e.getSlot() == 21) {
            startChatEdit(player, tag, EditingType.CHANGING_VOUCHER_MATERIAL, message("messages.editor.voucher-material").replace("%current%", tag.getVoucherMaterial()));
        } else if (e.getSlot() == 22) {
            new VoucherLoreEditorMenu(SupremeTags.getMenuUtilIdentifier(player, tag.getIdentifier())).open();
        } else if (e.getSlot() == 23) {
            startChatEdit(player, tag, EditingType.CHANGING_VOUCHER_MODEL_DATA, message("messages.editor.voucher-model-data").replace("%current%", String.valueOf(tag.getVoucherCustomModelData())));
        } else if (e.getSlot() == 24) {
            SupremeTags.getInstance().getTagEditorSessionManager().snapshot(player, tag);
            tag.setVoucherGlow(!tag.isVoucherGlow());
            saveAndRefresh(tag);
            msgPlayer(player, message("messages.editor.voucher-glow-changed").replace("%enabled%", String.valueOf(tag.isVoucherGlow())));
            new VoucherEditorMenu(SupremeTags.getMenuUtilIdentifier(player, tag.getIdentifier())).open();
        } else if (e.getSlot() == 49) {
            new SpecificTagMenu(SupremeTags.getMenuUtilIdentifier(player, tag.getIdentifier())).open();
        }

        e.setCancelled(true);
    }

    @Override
    public void setMenuItems() {
        Tag tag = SupremeTags.getInstance().getTagManager().getTag(menuUtil.getIdentifier());
        if (tag == null) return;

        getInventory().setItem(13, makeItem(Material.NAME_TAG, format("&e&lVoucher Overview"), overviewLore(tag)));
        getInventory().setItem(20, makeItem(Material.PAPER, format("&e&lDisplay Name"), List.of("&7Current: &6" + tag.getVoucherDisplayName(), "", "&f[&6★&f] &eClick to change!")));
        getInventory().setItem(21, makeItem(Material.NAME_TAG, format("&e&lMaterial"), List.of("&7Current: &6" + tag.getVoucherMaterial(), "", "&f[&6★&f] &eClick to change!")));
        getInventory().setItem(22, makeItem(Material.BOOK, format("&e&lLore"), voucherLore(tag)));
        getInventory().setItem(23, makeItem(Material.ITEM_FRAME, format("&e&lCustom Model Data"), List.of("&7Current: &6" + tag.getVoucherCustomModelData(), "", "&f[&6★&f] &eClick to change!")));
        getInventory().setItem(24, makeItem(Material.GLOWSTONE_DUST, format("&e&lGlow"), List.of("&7Current: &6" + tag.isVoucherGlow(), "", "&f[&6★&f] &eClick to toggle!")));
        getInventory().setItem(49, makeItem(Material.ARROW, format("&f&lBack"), List.of("&7Return to the tag editor.")));

        applyBorder();
    }

    private void startChatEdit(Player player, Tag tag, EditingType type, String... prompts) {
        SupremeTags.getInstance().getEditorList().put(player.getUniqueId(), new Editor(tag.getIdentifier(), type, false));
        if (prompts.length == 0) {
            msgPlayer(player, message("messages.editor.type-cancel"));
        } else {
            msgPlayer(player, prompts);
        }
        player.closeInventory();
    }

    private List<String> overviewLore(Tag tag) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Tag: &6" + tag.getIdentifier());
        lore.add("&7Material: &6" + tag.getVoucherMaterial());
        lore.add("&7Display Name: &6" + tag.getVoucherDisplayName());
        lore.add("&7Custom Model Data: &6" + tag.getVoucherCustomModelData());
        lore.add("&7Glow: &6" + tag.isVoucherGlow());
        lore.add("&7Lore Lines: &6" + tag.getVoucherLore().size());
        lore.add("");
        lore.add("&8[&e➜&8] &fThese settings are saved with database tags too.");
        return lore;
    }

    private List<String> voucherLore(Tag tag) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Current:");
        if (tag.getVoucherLore().isEmpty()) {
            lore.add("&cNone");
        } else {
            tag.getVoucherLore().forEach(line -> lore.add("&6" + line));
        }
        lore.add("");
        lore.add("&8[&e➜&8] &fOpen the lore editor to manage each line.");
        lore.add("");
        lore.add("&f[&6★&f] &eClick to open lore editor!");
        return lore;
    }

    private void saveAndRefresh(Tag tag) {
        SupremeTags.getInstance().getTagManager().saveTag(tag);
        SupremeTags.getInstance().getTagManager().unloadTags();
        SupremeTags.getInstance().getTagManager().loadTags(true);
        SupremeTags.getInstance().getCategoryManager().initCategories();
    }

    private void applyBorder() {
        String itemMaterial = guis.getString("gui.items.glass.material");
        if (itemMaterial == null) return;
        String itemDisplayname = guis.getString("gui.items.glass.displayname");
        int customModelData = guis.getInt("gui.items.glass.custom-model-data");
        boolean hideToolTip = guis.getBoolean("gui.items.glass.hide-tooltip");
        Material material = XMaterial.matchXMaterial(itemMaterial.toUpperCase()).map(XMaterial::get).orElse(Material.GRAY_STAINED_GLASS_PANE);

        for (int i = 0; i < getSlots(); i++) {
            if (getInventory().getItem(i) != null) continue;
            if (i < 9 || i >= 45 || i % 9 == 0 || (i + 1) % 9 == 0) {
                getInventory().setItem(i, makeItem(material, itemDisplayname, customModelData, hideToolTip));
            }
        }
    }

    private String message(String path) {
        return messages.getString(path, "")
                .replace("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix", "")));
    }
}
