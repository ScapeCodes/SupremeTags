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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.noscape.project.supremetags.utils.Utils.format;
import static net.noscape.project.supremetags.utils.Utils.msgPlayer;

public class VoucherLoreEditorMenu extends Menu {

    private static final int[] LORE_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};

    private final FileConfiguration messages = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get();
    private final FileConfiguration guis = SupremeTags.getInstance().getConfigManager().getConfig("guis.yml").get();

    public VoucherLoreEditorMenu(MenuUtil menuUtil) {
        super(menuUtil);
    }

    @Override
    public String getMenuName() {
        return format("&8Voucher Lore: &e" + menuUtil.getIdentifier());
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

        int lineIndex = getLineIndex(e.getSlot());
        if (lineIndex >= 0 && lineIndex < tag.getVoucherLore().size()) {
            if (e.getClick() == ClickType.RIGHT) {
                SupremeTags.getInstance().getTagEditorSessionManager().snapshot(player, tag);
                List<String> lore = new ArrayList<>(tag.getVoucherLore());
                lore.remove(lineIndex);
                tag.setVoucherLore(lore);
                saveAndRefresh(tag);
                msgPlayer(player, message("messages.editor.voucher-lore-line-removed").replace("%line%", String.valueOf(lineIndex + 1)));
                new VoucherLoreEditorMenu(SupremeTags.getMenuUtilIdentifier(player, tag.getIdentifier())).open();
            } else if (e.getClick() == ClickType.SHIFT_LEFT && lineIndex > 0) {
                moveLine(player, tag, lineIndex, lineIndex - 1);
            } else if (e.getClick() == ClickType.SHIFT_RIGHT && lineIndex < tag.getVoucherLore().size() - 1) {
                moveLine(player, tag, lineIndex, lineIndex + 1);
            } else {
                startChatEdit(player, tag.getIdentifier() + "::" + lineIndex, EditingType.CHANGING_VOUCHER_LORE_LINE,
                        message("messages.editor.voucher-lore-line").replace("%line%", String.valueOf(lineIndex + 1)).replace("%current%", tag.getVoucherLore().get(lineIndex)),
                        message("messages.editor.type-cancel"));
            }
        } else if (e.getSlot() == 40) {
            startChatEdit(player, tag.getIdentifier(), EditingType.ADDING_VOUCHER_LORE_LINE,
                    message("messages.editor.voucher-lore-add"), message("messages.editor.type-cancel"));
        } else if (e.getSlot() == 41) {
            SupremeTags.getInstance().getTagEditorSessionManager().snapshot(player, tag);
            tag.setVoucherLore(new ArrayList<>());
            saveAndRefresh(tag);
            msgPlayer(player, message("messages.editor.voucher-lore-cleared"));
            new VoucherLoreEditorMenu(SupremeTags.getMenuUtilIdentifier(player, tag.getIdentifier())).open();
        } else if (e.getSlot() == 49) {
            new VoucherEditorMenu(SupremeTags.getMenuUtilIdentifier(player, tag.getIdentifier())).open();
        }

        e.setCancelled(true);
    }

    @Override
    public void setMenuItems() {
        Tag tag = SupremeTags.getInstance().getTagManager().getTag(menuUtil.getIdentifier());
        if (tag == null) return;

        List<String> lore = tag.getVoucherLore();
        for (int i = 0; i < Math.min(lore.size(), LORE_SLOTS.length); i++) {
            getInventory().setItem(LORE_SLOTS[i], makeItem(Material.PAPER, format("&e&lLine " + (i + 1)), List.of(
                    "&7Current: &f" + lore.get(i),
                    "",
                    "&f[&6★&f] &eLeft-click to edit!",
                    "&f[&6★&f] &eRight-click to remove!",
                    "&f[&6★&f] &eShift-left to move up!",
                    "&f[&6★&f] &eShift-right to move down!"
            )));
        }

        getInventory().setItem(40, makeItem(Material.LIME_DYE, format("&a&lAdd Lore Line"), List.of("&7Add a new line to the voucher lore.", "", "&f[&6★&f] &eClick to add!")));
        getInventory().setItem(41, makeItem(Material.RED_DYE, format("&c&lClear Lore"), List.of("&7Remove all voucher lore lines.", "", "&f[&6★&f] &eClick to clear!")));
        getInventory().setItem(49, makeItem(Material.ARROW, format("&f&lBack"), List.of("&7Return to the voucher editor.")));

        applyBorder();
    }

    private void moveLine(Player player, Tag tag, int from, int to) {
        SupremeTags.getInstance().getTagEditorSessionManager().snapshot(player, tag);
        List<String> lore = new ArrayList<>(tag.getVoucherLore());
        String value = lore.remove(from);
        lore.add(to, value);
        tag.setVoucherLore(lore);
        saveAndRefresh(tag);
        msgPlayer(player, message("messages.editor.voucher-lore-line-moved").replace("%line%", String.valueOf(from + 1)));
        new VoucherLoreEditorMenu(SupremeTags.getMenuUtilIdentifier(player, tag.getIdentifier())).open();
    }

    private void startChatEdit(Player player, String identifier, EditingType type, String... prompts) {
        SupremeTags.getInstance().getEditorList().put(player.getUniqueId(), new Editor(identifier, type, false));
        msgPlayer(player, prompts);
        player.closeInventory();
    }

    private int getLineIndex(int slot) {
        for (int i = 0; i < LORE_SLOTS.length; i++) {
            if (LORE_SLOTS[i] == slot) {
                return i;
            }
        }
        return -1;
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
