package net.noscape.project.supremetags.guis.tageditor;

import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.enums.EditingType;
import net.noscape.project.supremetags.guis.confirm.ConfirmationMenu;
import net.noscape.project.supremetags.handlers.Editor;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.handlers.menu.Menu;
import net.noscape.project.supremetags.handlers.menu.MenuUtil;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.noscape.project.supremetags.utils.Utils.*;

public class SpecificTagMenu extends Menu {

    private FileConfiguration messages = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get();
    private FileConfiguration guis = SupremeTags.getInstance().getConfigManager().getConfig("guis.yml").get();


    public SpecificTagMenu(MenuUtil menuUtil) {
        super(menuUtil);
    }

    @Override
    public String getMenuName() {
        String title = format(guis.getString("gui.tag-editor-menu.specific-tag-editor-title").replaceAll("%identifier%", menuUtil.getIdentifier()));
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

        String tag = messages.getString("messages.editor.tag").replace("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
        String desc = messages.getString("messages.editor.description").replace("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
        String category = messages.getString("messages.editor.category").replace("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
        String permission = messages.getString("messages.editor.permission").replace("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
        String deleted = messages.getString("messages.editor.deleted").replace("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
        String withdrawable = messages.getString("messages.editor.withdrawable").replace("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
        String cost = messages.getString("messages.editor.cost").replace("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
        String rarity = messages.getString("messages.editor.rarity").replace("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));

        Tag t = SupremeTags.getInstance().getTagManager().getTag(menuUtil.getIdentifier());

        if (e.getSlot() == 13) {
            Editor editor = new Editor(menuUtil.getIdentifier(), EditingType.CHANGING_TAG, false);
            SupremeTags.getInstance().getEditorList().put(player, editor);
            msgPlayer(player, tag.replace("%current%", t.getTag().getFirst()));
            player.closeInventory();
        } else if (e.getSlot() == 22) {

            if (t.isWithdrawable()) {
                t.setWithdrawable(false);
            } else {
                t.setWithdrawable(true);
            }

            SupremeTags.getInstance().getTagManager().saveTag(t);
            SupremeTags.getInstance().getTagManager().unloadTags();
            SupremeTags.getInstance().getTagManager().loadTags(true);

            SupremeTags.getInstance().getCategoryManager().initCategories();

            super.open();
            msgPlayer(player, withdrawable);
        } else if (e.getSlot() == 14) {
            Editor editor = new Editor(menuUtil.getIdentifier(), EditingType.CHANGING_DESCRIPTION, false);
            SupremeTags.getInstance().getEditorList().put(player, editor);
            msgPlayer(player, desc.replace("%current%", t.getDescription().getFirst()));
            player.closeInventory();
        } else if (e.getSlot() == 15) {
            Editor editor = new Editor(menuUtil.getIdentifier(), EditingType.CHANGING_CATEGORY, false);
            SupremeTags.getInstance().getEditorList().put(player, editor);
            msgPlayer(player, category.replace("%current%", t.getCategory()));
            player.closeInventory();
        } else if (e.getSlot() == 16) {
            Editor editor = new Editor(menuUtil.getIdentifier(), EditingType.CHANGING_PERMISSION, false);
            SupremeTags.getInstance().getEditorList().put(player, editor);
            msgPlayer(player, permission.replace("%current%", t.getPermission()));
            player.closeInventory();
        } else if (e.getSlot() == 23) {
            Editor editor = new Editor(menuUtil.getIdentifier(), EditingType.CHANGING_COST, false);
            SupremeTags.getInstance().getEditorList().put(player, editor);
            msgPlayer(player, cost.replace("%current%", String.valueOf(t.getEcoAmount())));
            player.closeInventory();
        } else if (e.getSlot() == 24) {
            Editor editor = new Editor(menuUtil.getIdentifier(), EditingType.CHANGING_RARITY, false);
            SupremeTags.getInstance().getEditorList().put(player, editor);
            msgPlayer(player, rarity.replace("%current%", t.getRarity()), "&7Rarities: " + SupremeTags.getInstance().getRarityManager().getRarityMap().keySet().toString().replace("[", "").replace("]", ""));
            player.closeInventory();
        } else if (e.getSlot() == 49) {
            String identifier = menuUtil.getIdentifier();
            player.closeInventory();
            new ConfirmationMenu(SupremeTags.getMenuUtil(player), "delete-tag:" + identifier).open();
        } else {
            e.setCancelled(true);
        }
    }

    @Override
    public void setMenuItems() {
        if (menuUtil.getIdentifier() != null) {
            if (SupremeTags.getInstance().getTagManager().getTag(menuUtil.getIdentifier()) != null) {
                Tag t = SupremeTags.getInstance().getTagManager().getTag(menuUtil.getIdentifier());

                List<String> lore = new ArrayList<>();

                lore.add("&8[&e➜&8] &fAll tag settings can be edited by this editor.");
                lore.add("&8[&e➜&8] &fYou can also edit the tags in the tags.yml and then reload.");
                lore.add("");
                lore.add("&7Identifier: &6" + t.getIdentifier());
                lore.add("&7Permission: &6" + t.getPermission());
                lore.add("&7Category: &6" + t.getCategory());
                lore.add("&7Cost: &6" + t.getEconomy().getAmount());
                lore.add("&7Withdrawable: &6" + t.isWithdrawable());
                lore.add("&7Order: &6" + t.getOrder());
                lore.add("&7Rarity: &6" + t.getRarity());
                lore.add("&7Description:");
                lore.add("&6" + t.getDescription());
                lore.add("");
                lore.add("&f[&6★&f] &7Use the items on the right to change specific settings/values.");

                String displayname;

                if (SupremeTags.getInstance().getTagManager().getTagConfig().getString("tags." + t.getIdentifier() + ".displayname") != null) {
                    if (t.getCurrentTag() != null) {
                        displayname = Objects.requireNonNull(SupremeTags.getInstance().getTagManager().getTagConfig().getString("tags." + t.getIdentifier() + ".displayname")).replace("%tag%", t.getCurrentTag());
                    } else {
                        displayname = Objects.requireNonNull(SupremeTags.getInstance().getTagManager().getTagConfig().getString("tags." + t.getIdentifier() + ".displayname")).replace("%tag%", t.getTag().get(0));
                    }
                } else {
                    if (t.getCurrentTag() != null) {
                        displayname = format("&7Tag: " + t.getCurrentTag());
                    } else {
                        displayname = format("&7Tag: " + t.getTag().get(0));
                    }
                }

                String c_tag_title = guis.getString("gui.tag-editor-menu.editor-items.change-tag");
                String c_description_title = guis.getString("gui.tag-editor-menu.editor-items.change-description");
                String c_permission_title = guis.getString("gui.tag-editor-menu.editor-items.change-permission");
                String c_category_title = guis.getString("gui.tag-editor-menu.editor-items.change-category");
                String d_tag_title = guis.getString("gui.tag-editor-menu.editor-items.delete-tag");

                getInventory().setItem(19, makeItem(Material.BOOK, format(displayname), lore));

                List<String> c_tag = new ArrayList<>();
                if (t.getCurrentTag() != null) {
                    c_tag.add("&7Current: &6" + t.getCurrentTag());
                } else {
                    c_tag.add("&7Current: &6" + t.getTag().get(0));
                }
                getInventory().setItem(13, makeItem(Material.NAME_TAG, format(c_tag_title), c_tag));

                List<String> c_desc = new ArrayList<>();
                c_desc.add("&7Current: &6" + t.getDescription());
                c_desc.add("");
                c_desc.add("&8[&e➜&8] &fDescriptions are one liner strings that can be assign to tag lores.");
                c_desc.add("");
                c_desc.add("&f[&6★&f] &eClick to change!");
                getInventory().setItem(14, makeItem(Material.OAK_SIGN, format(c_description_title), c_desc));

                List<String> c_cat = new ArrayList<>();
                c_cat.add("&7Current: &6" + t.getCategory());
                c_cat.add("");
                c_cat.add("&8[&e➜&8] &fCategory value makes sure that the tag is assigned to the category when catorgies");
                c_cat.add("&fare enabled.");
                c_cat.add("");
                c_cat.add("&f[&6★&f] &eClick to change!");
                getInventory().setItem(15, makeItem(Material.BOOK, format(c_category_title), c_cat));

                List<String> c_perm = new ArrayList<>();
                c_perm.add("&7Current: &6" + t.getPermission());
                c_perm.add("");
                c_perm.add("&8[&e➜&8] &fThe permission that allows the player to unlock the tag.");
                c_perm.add("");
                c_perm.add("&f[&6★&f] &eClick to change!");
                getInventory().setItem(16, makeItem(Material.REDSTONE_TORCH, format(c_permission_title), c_perm));

                List<String> c_withdraw = new ArrayList<>();
                c_withdraw.add("&7Current: &6" + t.isWithdrawable());
                c_withdraw.add("");
                c_withdraw.add("&8[&e➜&8] &fIf the tag can be withdrawn into a voucher item with the withdraw command.");
                c_withdraw.add("&8[&e➜&8] &f/tag withdraw <identifier>");
                c_withdraw.add("");
                c_withdraw.add("&f[&6★&f] &eClick to change!");
                getInventory().setItem(22, makeItem(Material.REPEATER, format("&6&lWithdrawable!"), c_withdraw));

                List<String> c_cost = new ArrayList<>();
                c_cost.add("&7Current: &6" + t.getEconomy().getAmount());
                c_cost.add("");
                c_cost.add("&8[&e➜&8] &fThe amount it will cost when buyable tags are enabled (economy)");
                c_cost.add("");
                c_cost.add("&f[&6★&f] &eClick to change!");
                getInventory().setItem(23, makeItem(Material.SUNFLOWER, format("&e&lCost!"), c_cost));

                List<String> c_rarity = new ArrayList<>();
                c_rarity.add("&7Current: &6" + t.getRarity());
                c_rarity.add("");
                c_rarity.add("&8[&e➜&8] &fThe rarity its valued/categorized into.");
                c_rarity.add("");
                c_rarity.add("&f[&6★&f] &eClick to change!");
                getInventory().setItem(24, makeItem(Material.EMERALD, format("&b&lRarity"), c_rarity));

                getInventory().setItem(25, makeItem(Material.BARRIER, format("&c&lComing Soon!"), 0, false));
                getInventory().setItem(31, makeItem(Material.BARRIER, format("&c&lComing Soon!"), 0, false));
                getInventory().setItem(32, makeItem(Material.BARRIER, format("&c&lComing Soon!"), 0, false));
                getInventory().setItem(33, makeItem(Material.BARRIER, format("&c&lComing Soon!"), 0, false));
                getInventory().setItem(34, makeItem(Material.BARRIER, format("&c&lComing Soon!"), 0, false));
                

                List<String> c_delete = new ArrayList<>();
                c_delete.add("&7This cannot be undone!");
                getInventory().setItem(49, makeItem(Material.RED_WOOL, format(d_tag_title), c_delete));
            }
        }
        fillEmpty();
    }
}
