package net.noscape.project.supremetags.guis.tageditor;

import com.cryptomorin.xseries.XMaterial;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.enums.EditingType;
import net.noscape.project.supremetags.guis.confirm.ConfirmationMenu;
import net.noscape.project.supremetags.guis.variant.TagVariantsMenu;
import net.noscape.project.supremetags.handlers.Editor;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.handlers.menu.Menu;
import net.noscape.project.supremetags.handlers.menu.MenuUtil;
import net.noscape.project.supremetags.handlers.requirements.TagRequirement;
import net.noscape.project.supremetags.handlers.requirements.TagRequirements;
import net.noscape.project.supremetags.utils.ItemResolver;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
        if (SupremeTags.getInstance().getEditorList().containsKey(player.getUniqueId())) return;

        String tag = messages.getString("messages.editor.tag").replace("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
        String desc = messages.getString("messages.editor.description").replace("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
        String category = messages.getString("messages.editor.category").replace("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
        String permission = messages.getString("messages.editor.permission").replace("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
        String deleted = messages.getString("messages.editor.deleted").replace("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
        String withdrawable = messages.getString("messages.editor.withdrawable").replace("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
        String cost = messages.getString("messages.editor.cost").replace("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
        String rarity = messages.getString("messages.editor.rarity").replace("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));

        Tag t = SupremeTags.getInstance().getTagManager().getTag(menuUtil.getIdentifier());
        if (t == null) return;

        if (e.getSlot() == 13) {
            Editor editor = new Editor(menuUtil.getIdentifier(), EditingType.CHANGING_TAG, false);
            SupremeTags.getInstance().getEditorList().put(player.getUniqueId(), editor);
            msgPlayer(player, tag.replace("%current%", t.getTag().getFirst()));
            player.closeInventory();
        } else if (e.getSlot() == 22) {
            SupremeTags.getInstance().getTagEditorSessionManager().snapshot(player, t);

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
            SupremeTags.getInstance().getEditorList().put(player.getUniqueId(), editor);
            msgPlayer(player, desc.replace("%current%", t.getDescription().getFirst()));
            player.closeInventory();
        } else if (e.getSlot() == 15) {
            if (e.getClick() == ClickType.RIGHT) {
                cycleCategory(player, t);
                return;
            }
            Editor editor = new Editor(menuUtil.getIdentifier(), EditingType.CHANGING_CATEGORY, false);
            SupremeTags.getInstance().getEditorList().put(player.getUniqueId(), editor);
            msgPlayer(player, category.replace("%current%", t.getCategory()));
            player.closeInventory();
        } else if (e.getSlot() == 16) {
            Editor editor = new Editor(menuUtil.getIdentifier(), EditingType.CHANGING_PERMISSION, false);
            SupremeTags.getInstance().getEditorList().put(player.getUniqueId(), editor);
            msgPlayer(player, permission.replace("%current%", t.getPermission()));
            player.closeInventory();
        } else if (e.getSlot() == 23) {
            Editor editor = new Editor(menuUtil.getIdentifier(), EditingType.CHANGING_COST, false);
            SupremeTags.getInstance().getEditorList().put(player.getUniqueId(), editor);
            msgPlayer(player, cost.replace("%current%", String.valueOf(t.getEcoAmount())));
            player.closeInventory();
        } else if (e.getSlot() == 24) {
            if (e.getClick() == ClickType.RIGHT) {
                cycleRarity(player, t);
                return;
            }
            Editor editor = new Editor(menuUtil.getIdentifier(), EditingType.CHANGING_RARITY, false);
            SupremeTags.getInstance().getEditorList().put(player.getUniqueId(), editor);
            msgPlayer(player, rarity.replace("%current%", t.getRarity()), message("messages.editor.rarities-list")
                    .replace("%rarities%", SupremeTags.getInstance().getRarityManager().getRarityMap().keySet().toString().replace("[", "").replace("]", "")));
            player.closeInventory();
        } else if (e.getSlot() == 25) {
            SupremeTags.getInstance().getTagEditorSessionManager().snapshot(player, t);
            t.getEconomy().setEnabled(!t.getEconomy().isEnabled());
            saveAndRefresh(t);
            msgPlayer(player, message("messages.editor.economy-enabled-changed")
                    .replace("%enabled%", String.valueOf(t.getEconomy().isEnabled())));
            super.open();
        } else if (e.getSlot() == 30) {
            Editor editor = new Editor(menuUtil.getIdentifier(), e.getClick() == ClickType.RIGHT ? EditingType.CHANGING_ECONOMY_CONDITION : EditingType.CHANGING_ECONOMY_TAKE_CMD, false);
            SupremeTags.getInstance().getEditorList().put(player.getUniqueId(), editor);
            if (e.getClick() == ClickType.RIGHT) {
                msgPlayer(player, message("messages.editor.economy-condition")
                        .replace("%current%", t.getEconomy().getCondition()), message("messages.editor.economy-condition-example"), message("messages.editor.type-cancel"));
            } else {
                msgPlayer(player, message("messages.editor.economy-take-command")
                        .replace("%current%", t.getEconomy().getTake_cmd()), message("messages.editor.economy-take-command-example"), message("messages.editor.type-cancel"));
            }
            player.closeInventory();
        } else if (e.getSlot() == 31) {
            Editor editor = new Editor(menuUtil.getIdentifier(), EditingType.CHANGING_ECONOMY_TYPE, false);
            SupremeTags.getInstance().getEditorList().put(player.getUniqueId(), editor);
            msgPlayer(player, message("messages.editor.economy-type")
                    .replace("%current%", t.getEconomy().getType()), message("messages.editor.economy-type-examples"), message("messages.editor.type-cancel"));
            player.closeInventory();
        } else if (e.getSlot() == 32) {
            new TagVariantsMenu(SupremeTags.getMenuUtilIdentifier(player, t.getIdentifier()), t).open();
        } else if (e.getSlot() == 33) {
            handleRequirementsClick(player, t, e.getClick());
        } else if (e.getSlot() == 34) {
            new VoucherEditorMenu(SupremeTags.getMenuUtilIdentifier(player, t.getIdentifier())).open();
        } else if (e.getSlot() == 48) {
            String identifier = t.getIdentifier();
            boolean undone = SupremeTags.getInstance().getTagEditorSessionManager().undo(player);
            msgPlayer(player, undone ? message("messages.editor.undo-success").replace("%identifier%", identifier) : message("messages.editor.undo-empty"));
            new SpecificTagMenu(SupremeTags.getMenuUtilIdentifier(player, identifier)).open();
        } else if (e.getSlot() == 49) {
            String identifier = menuUtil.getIdentifier();
            SupremeTags.getInstance().getTagEditorSessionManager().snapshot(player, t);
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

                lore.add("&8[&e➜&8] &fLive preview of the tag item and key settings.");
                lore.add("&8[&e➜&8] &fUse editor items to change values safely.");
                lore.add("");
                lore.add("&7Identifier: &6" + t.getIdentifier());
                lore.add("&7Permission: &6" + t.getPermission());
                lore.add("&7Category: &6" + t.getCategory());
                lore.add("&7Cost: &6" + t.getEconomy().getAmount());
                lore.add("&7Economy Enabled: &6" + t.getEconomy().isEnabled());
                lore.add("&7Economy Type: &6" + t.getEconomy().getType());
                lore.add("&7Withdrawable: &6" + t.isWithdrawable());
                lore.add("&7Order: &6" + t.getOrder());
                lore.add("&7Rarity: &6" + t.getRarity());
                lore.add("&7Description:");
                lore.add("&6" + t.getDescription());
                lore.add("");
                lore.add("&f[&6★&f] &7Use the items on the right to change specific settings/values.");

                String displayname;

                if (SupremeTags.getInstance().getTagManager().getConfigForTag(t.getIdentifier()).getString("tags." + t.getIdentifier() + ".displayname") != null) {
                    if (t.getCurrentTag() != null) {
                        displayname = Objects.requireNonNull(SupremeTags.getInstance().getTagManager().getConfigForTag(t.getIdentifier()).getString("tags." + t.getIdentifier() + ".displayname")).replace("%tag%", t.getCurrentTag());
                    } else {
                        displayname = Objects.requireNonNull(SupremeTags.getInstance().getTagManager().getConfigForTag(t.getIdentifier()).getString("tags." + t.getIdentifier() + ".displayname")).replace("%tag%", t.getTag().get(0));
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

                getInventory().setItem(19, createLivePreview(t, displayname, lore));

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
                c_cat.add("&f[&6★&f] &eLeft-click to type a category!");
                c_cat.add("&f[&6★&f] &eRight-click to cycle categories!");
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
                c_cost.add("&7Enabled: &6" + t.getEconomy().isEnabled());
                c_cost.add("&7Type: &6" + t.getEconomy().getType());
                c_cost.add("");
                c_cost.add("&8[&e➜&8] &fThe amount it will cost when buyable tags are enabled (economy)");
                c_cost.add("");
                c_cost.add("&f[&6★&f] &eClick to change amount!");
                getInventory().setItem(23, makeItem(Material.SUNFLOWER, format("&e&lCost!"), c_cost));

                List<String> c_rarity = new ArrayList<>();
                c_rarity.add("&7Current: &6" + t.getRarity());
                c_rarity.add("");
                c_rarity.add("&8[&e➜&8] &fThe rarity its valued/categorized into.");
                c_rarity.add("");
                c_rarity.add("&f[&6★&f] &eLeft-click to type a rarity!");
                c_rarity.add("&f[&6★&f] &eRight-click to cycle rarities!");
                getInventory().setItem(24, makeItem(Material.EMERALD, format("&b&lRarity"), c_rarity));

                getInventory().setItem(25, makeItem(Material.LEVER, format("&a&lToggle Economy"), List.of("&7Current: &6" + t.getEconomy().isEnabled(), "", "&f[&6★&f] &eClick to toggle!")));
                getInventory().setItem(30, makeItem(Material.COMMAND_BLOCK, format("&6&lCustom Economy"), List.of("&7Take Command: &6" + emptyFallback(t.getEconomy().getTake_cmd()), "&7Condition: &6" + emptyFallback(t.getEconomy().getCondition()), "", "&f[&6★&f] &eLeft-click to edit take command!", "&f[&6★&f] &eRight-click to edit condition!")));
                getInventory().setItem(31, makeItem(Material.HOPPER, format("&e&lEconomy Type"), List.of("&7Current: &6" + t.getEconomy().getType(), "", "&f[&6★&f] &eClick to type a new type!")));
                getInventory().setItem(32, makeItem(Material.CHEST, format("&d&lVariants"), List.of("&7Variants: &6" + t.getVariants().size(), "", "&f[&6★&f] &eClick to edit variants in the same flow!")));
                getInventory().setItem(33, makeItem(Material.WRITABLE_BOOK, format("&b&lRequirements"), getRequirementsLore(t)));
                getInventory().setItem(34, makeItem(Material.PAPER, format("&e&lVoucher Item"), List.of("&7Name: &6" + t.getVoucherDisplayName(), "&7Material: &6" + t.getVoucherMaterial(), "&7Model Data: &6" + t.getVoucherCustomModelData(), "&7Glow: &6" + t.isVoucherGlow(), "&7Lore Lines: &6" + t.getVoucherLore().size(), "", "&f[&6★&f] &eClick to edit voucher item!")));
                  

                List<String> c_delete = new ArrayList<>();
                c_delete.add("&7This cannot be undone!");
                getInventory().setItem(48, makeItem(Material.LIME_DYE, format("&a&lUndo Last Edit"), List.of("&7Undo your last change in this editor session.", "", "&f[&6★&f] &eClick to undo!")));
                getInventory().setItem(49, makeItem(Material.RED_WOOL, format(d_tag_title), c_delete));
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
                    if (getInventory().getItem(i) != null) {
                        continue;
                    }

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

    private void cycleCategory(Player player, Tag tag) {
        List<String> categories = SupremeTags.getInstance().getCategoryManager().getCatorgies();
        if (categories.isEmpty()) {
            msgPlayer(player, message("messages.editor.no-categories-cycle"));
            return;
        }

        SupremeTags.getInstance().getTagEditorSessionManager().snapshot(player, tag);
        int index = categories.indexOf(tag.getCategory());
        tag.setCategory(categories.get((index + 1) % categories.size()));
        saveAndRefresh(tag);
        msgPlayer(player, message("messages.editor.category-changed")
                .replace("%category%", tag.getCategory()));
        super.open();
    }

    private void cycleRarity(Player player, Tag tag) {
        List<String> rarities = new ArrayList<>(SupremeTags.getInstance().getRarityManager().getRarityMap().keySet());
        if (rarities.isEmpty()) {
            msgPlayer(player, message("messages.editor.no-rarities-cycle"));
            return;
        }

        SupremeTags.getInstance().getTagEditorSessionManager().snapshot(player, tag);
        int index = rarities.indexOf(tag.getRarity());
        tag.setRarity(rarities.get((index + 1) % rarities.size()));
        saveAndRefresh(tag);
        msgPlayer(player, message("messages.editor.rarity-changed")
                .replace("%rarity%", tag.getRarity()));
        super.open();
    }

    private void handleRequirementsClick(Player player, Tag tag, ClickType clickType) {
        SupremeTags.getInstance().getTagEditorSessionManager().snapshot(player, tag);
        TagRequirements requirements = tag.getRequirements();
        boolean configuredEnabled = requirements != null && requirements.isConfiguredEnabled();
        boolean persistUnlock = requirements != null && requirements.isPersistUnlock();
        TagRequirements.Mode mode = requirements == null ? TagRequirements.Mode.ALL : requirements.getMode();
        List<TagRequirement> requirementList = new ArrayList<>(requirements == null ? List.of() : requirements.getRequirements());

        if (clickType == ClickType.RIGHT) {
            mode = mode == TagRequirements.Mode.ALL ? TagRequirements.Mode.ANY : TagRequirements.Mode.ALL;
            msgPlayer(player, message("messages.editor.requirement-mode-changed")
                    .replace("%mode%", mode.name()));
        } else if (clickType == ClickType.SHIFT_LEFT) {
            persistUnlock = !persistUnlock;
            msgPlayer(player, message("messages.editor.persist-unlock-changed")
                    .replace("%enabled%", String.valueOf(persistUnlock)));
        } else if (clickType == ClickType.MIDDLE) {
            if (requirementList.isEmpty()) {
                requirementList.add(new TagRequirement("permission", "permission", tag.getPermission(), null, "==", "true", null, "VAULT", 0.0D, "&f- &7Permission: " + tag.getPermission(), "&f- &7Permission: " + tag.getPermission(), "&cYou need permission to unlock this tag."));
                configuredEnabled = true;
                msgPlayer(player, message("messages.editor.default-requirement-added")
                        .replace("%permission%", tag.getPermission()));
            } else {
                requirementList.remove(requirementList.size() - 1);
                msgPlayer(player, message("messages.editor.requirement-removed"));
            }
        } else {
            configuredEnabled = !configuredEnabled;
            msgPlayer(player, message("messages.editor.requirements-enabled-changed")
                    .replace("%enabled%", String.valueOf(configuredEnabled)));
        }

        tag.setRequirements(new TagRequirements(configuredEnabled, persistUnlock, mode, requirementList));
        saveAndRefresh(tag);
        super.open();
    }

    private String message(String path) {
        return messages.getString(path, "")
                .replace("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix", "")));
    }

    private ItemStack createLivePreview(Tag tag, String displayname, List<String> lore) {
        String material = SupremeTags.getInstance().getTagManager().getConfigForTag(tag.getIdentifier()).getString("tags." + tag.getIdentifier() + ".display-item", "NAME_TAG");
        ItemResolver.ResolvedItem resolved = ItemResolver.resolveCustomItem(menuUtil.getOwner(), material);
        ItemStack item = resolved.item();
        ItemMeta meta = resolved.meta();
        if (meta != null) {
            meta.setDisplayName(format(displayname));
            meta.setLore(color(lore));
            int modelData = SupremeTags.getInstance().getTagManager().getConfigForTag(tag.getIdentifier()).getInt("tags." + tag.getIdentifier() + ".custom-model-data", 0);
            if (modelData > 0) {
                meta.setCustomModelData(modelData);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private List<String> getRequirementsLore(Tag tag) {
        List<String> lore = new ArrayList<>();
        TagRequirements requirements = tag.getRequirements();
        lore.add("&7Status: &6" + getRequirementsSummary(tag));
        lore.add("&7Enabled: &6" + (requirements != null && requirements.isConfiguredEnabled()));
        lore.add("&7Mode: &6" + (requirements == null ? TagRequirements.Mode.ALL : requirements.getMode()));
        lore.add("&7Persist Unlock: &6" + (requirements != null && requirements.isPersistUnlock()));
        lore.add("");

        if (requirements != null && !requirements.getRequirements().isEmpty()) {
            lore.add("&7Entries: &6" + requirements.getRequirements().stream().map(TagRequirement::getName).collect(Collectors.joining(", ")));
        } else {
            lore.add("&7Entries: &cNone");
        }

        lore.add("");
        lore.add("&f[&6★&f] &eLeft-click to toggle enabled!");
        lore.add("&f[&6★&f] &eRight-click to cycle ALL/ANY!");
        lore.add("&f[&6★&f] &eShift-left to toggle persist unlock!");
        lore.add("&f[&6★&f] &eMiddle-click to add/remove default entry!");
        return lore;
    }

    private String emptyFallback(String value) {
        return value == null || value.isBlank() ? "Not set" : value;
    }

    private void saveAndRefresh(Tag tag) {
        SupremeTags.getInstance().getTagManager().saveTag(tag);
        SupremeTags.getInstance().getTagManager().unloadTags();
        SupremeTags.getInstance().getTagManager().loadTags(true);
        SupremeTags.getInstance().getCategoryManager().initCategories();
    }

    private String getRequirementsSummary(Tag tag) {
        if (tag.getRequirements() == null || !tag.getRequirements().isEnabled()) {
            return "No active requirements.";
        }
        return tag.getRequirements().getMode() + " / " + tag.getRequirements().getRequirements().size() + " requirement(s)";
    }
}
