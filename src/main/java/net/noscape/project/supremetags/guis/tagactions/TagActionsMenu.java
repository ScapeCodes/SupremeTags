package net.noscape.project.supremetags.guis.tagactions;

import de.tr7zw.nbtapi.NBTItem;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.api.events.TagAssignEvent;
import net.noscape.project.supremetags.api.events.TagResetEvent;
import net.noscape.project.supremetags.guis.MainMenu;
import net.noscape.project.supremetags.guis.TagMenu;
import net.noscape.project.supremetags.guis.tageditor.SpecificTagMenu;
import net.noscape.project.supremetags.guis.variant.TagVariantsMenu;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.handlers.menu.Menu;
import net.noscape.project.supremetags.handlers.menu.MenuUtil;
import net.noscape.project.supremetags.storage.UserData;
import net.noscape.project.supremetags.utils.ItemResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.noscape.project.supremetags.utils.Utils.*;

public class TagActionsMenu extends Menu {

    private FileConfiguration messages = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get();
    private FileConfiguration guis = SupremeTags.getInstance().getConfigManager().getConfig("guis.yml").get();

    public TagActionsMenu(MenuUtil menuUtil) {
        super(menuUtil);
    }

    @Override
    public String getMenuName() {
        String title = format(Objects.requireNonNull(guis.getString("gui.tag-actions-menu.title")));
        title = title.replaceAll("%identifier%", menuUtil.getIdentifier());
        title = globalPlaceholders(menuUtil.getOwner(), title);
        return title;
    }

    @Override
    public int getSlots() {
        return guis.getInt("gui.tag-actions-menu.size");
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player player = (Player)e.getWhoClicked();

        String no_tag_selected = messages.getString("messages.no-tag-selected").replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));

        if (e.getClickedInventory().getType() == InventoryType.PLAYER) {
            e.setCancelled(true);
            return;
        }

        if (e.getCurrentItem().getType().equals(Material.valueOf(Objects.requireNonNull(this.guis.getString("gui.items.glass.material")).toUpperCase())))
            e.setCancelled(true);

        NBTItem nbt = new NBTItem(e.getCurrentItem());

        if (nbt.hasTag("name")) {
            String name = nbt.getString("name");

            if (name.equalsIgnoreCase("variants")) {
                String id = menuUtil.getIdentifier();
                player.closeInventory();
                new TagVariantsMenu(SupremeTags.getMenuUtil(player), SupremeTags.getInstance().getTagManager().getTag(id)).open();
            }

            if (name.equalsIgnoreCase("editor-tag")) {
                String id = menuUtil.getIdentifier();
                player.closeInventory();
                new SpecificTagMenu(SupremeTags.getMenuUtilIdentifier(player, id)).open();
            }

            if (name.equalsIgnoreCase("withdraw-tag")) {
                SupremeTags.getInstance().getVoucherManager().withdrawTag(player, menuUtil.getIdentifier());
                player.closeInventory();
            }

            if (name.equalsIgnoreCase("back")) {
                player.closeInventory();
                boolean isCategories = SupremeTags.getInstance().getConfig().getBoolean("settings.categories");
                if (isCategories) {
                    new MainMenu(SupremeTags.getMenuUtil(player)).open();
                } else {
                    new TagMenu(SupremeTags.getMenuUtil(player)).open();
                }
            }

            if (name.equalsIgnoreCase("assign-tag")) {
                String id = menuUtil.getIdentifier();

                TagAssignEvent tagevent = new TagAssignEvent(player, id, false);
                Bukkit.getPluginManager().callEvent(tagevent);

                if (tagevent.isCancelled()) return;

                if (!UserData.getActive(player.getUniqueId()).equalsIgnoreCase("none")) {
                    Tag t1 = SupremeTags.getInstance().getTagManager().getTag(UserData.getActive(player.getUniqueId()));

                    if (t1 != null) {
                        t1.removeEffects(menuUtil.getOwner());
                    }
                }

                UserData.setActive(player, tagevent.getTag());

                super.open();

                SupremeTags.getInstance().getTagManager().getTag(id).applyEffects(menuUtil.getOwner());

                if (SupremeTags.getInstance().getConfig().getBoolean("settings.gui-messages")) {
                    String select = messages.getString("messages.tag-select-message").replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
                    select = replacePlaceholders(menuUtil.getOwner(), select);
                    msgPlayer(player, select.replace("%identifier%", id).replaceAll("%tag%", SupremeTags.getInstance().getTagManager().getTag(menuUtil.getIdentifier()).getTag().get(0)));
                }
            }

            if (name.equalsIgnoreCase("unassign-tag")) {
                if (menuUtil.getIdentifier() == null || (menuUtil.getIdentifier().equalsIgnoreCase("none"))) {
                    msgPlayer(player, no_tag_selected);
                    return;
                }

                if (!SupremeTags.getInstance().getConfig().getBoolean("settings.forced-tag")) {
                    TagResetEvent tagEvent = new TagResetEvent(player, false);
                    Bukkit.getPluginManager().callEvent(tagEvent);

                    if (tagEvent.isCancelled()) return;

                    if (menuUtil.getIdentifier() != null || !(menuUtil.getIdentifier().equalsIgnoreCase("none"))) {
                        Tag t = SupremeTags.getInstance().getTagManager().getTag(menuUtil.getIdentifier());
                        if (t != null) {
                            t.removeEffects(menuUtil.getOwner());
                        }
                    }

                    UserData.setActive(player, "None");
                    super.open();

                    if (SupremeTags.getInstance().getConfig().getBoolean("settings.gui-messages")) {
                        msgPlayer(player, messages.getString("messages.reset-message").replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix"))));
                    }
                } else {
                    TagResetEvent tagEvent = new TagResetEvent(player, false);
                    Bukkit.getPluginManager().callEvent(tagEvent);

                    if (tagEvent.isCancelled()) return;

                    String defaultTag = SupremeTags.getInstance().getConfig().getString("settings.default-tag");

                    if (menuUtil.getIdentifier() != null || !(menuUtil.getIdentifier().equalsIgnoreCase("none"))) {
                        Tag t = SupremeTags.getInstance().getTagManager().getTag(menuUtil.getIdentifier());
                        if (t != null) {
                            t.removeEffects(menuUtil.getOwner());
                        }
                    }

                    UserData.setActive(player, defaultTag);
                    super.open();
                    menuUtil.setIdentifier(defaultTag);

                    if (SupremeTags.getInstance().getConfig().getBoolean("settings.gui-messages")) {
                        msgPlayer(player, messages.getString("messages.reset-message").replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix"))));
                    }
                }
            }
        }
    }

    @Override
    public void setMenuItems() {
        fillEmpty();

        for (String name : guis.getConfigurationSection("gui.tag-actions-menu.functions").getKeys(false)) {
            boolean enabled = guis.getBoolean("gui.tag-actions-menu.functions." + name + ".enable");

            //if (!enabled) continue;

            Tag tag = SupremeTags.getInstance().getTagManager().getTag(menuUtil.getIdentifier());

            if (enabled && name.equalsIgnoreCase("variants")) {
                boolean hasVariants = tag.hasVariants();
                boolean hide_when_zero = guis.getBoolean("gui.tag-actions-menu.functions." + name + ".hide-when-zero");

                if (!hasVariants) {
                    if (hide_when_zero) continue;
                }
            }

            // Handle assign-tag: permission should allow it always
            if (enabled && name.equalsIgnoreCase("assign-tag")) {
                if (!menuUtil.getOwner().hasPermission(tag.getPermission()))
                    continue;
            }

            // Handle unassign-tag: only allow if they actually have an active tag
            if (enabled && name.equalsIgnoreCase("unassign-tag")) {
                if (!UserData.getActive(menuUtil.getOwner().getUniqueId()).equalsIgnoreCase(menuUtil.getIdentifier()))
                    continue;
            }

            // Handle purchase-tag: only if it's a cost tag, and they don't already have permission
            if (enabled && name.equalsIgnoreCase("purchase-tag")) {
                if (!tag.isCostTag())
                    continue;
                if (menuUtil.getOwner().hasPermission(tag.getPermission()))
                    continue; // They already have it; don't offer purchase
            }

            if (enabled && name.equalsIgnoreCase("editor-tag")) {
                if (!menuUtil.getOwner().hasPermission("supremetags.admin"))
                    continue;
            }

            if (enabled && name.equalsIgnoreCase("withdraw-tag")) {
                if (!tag.isWithdrawable() && !menuUtil.getOwner().hasPermission(tag.getPermission()))
                    continue;
            }

            if (!enabled) continue;

            String item_material = guis.getString("gui.tag-actions-menu.functions." + name + ".display-item");
            int item_custom_model_data = guis.getInt("gui.tag-actions-menu.functions." + name + ".custom-model-data");
            List<String> item_lore = guis.getStringList("gui.tag-actions-menu.functions." + name + ".lore");

            int item_slot = guis.getInt("gui.tag-actions-menu.functions." + name + ".slot"); // Default slot
            List<Integer> slots = new ArrayList<>();
            boolean isSlots = false;

            boolean hideToolTip = guis.getBoolean("gui.tag-actions-menu.functions." + name + ".hide-tooltip");

            String item_displayname = guis.getString("gui.tag-actions-menu.functions." + name + ".displayname");

            ItemResolver.ResolvedItem resolved = ItemResolver.resolveCustomItem(item_material);
            ItemStack item = resolved.item();
            ItemMeta itemMeta = resolved.meta();
            NBTItem nbt = new NBTItem(item);

            if (item_custom_model_data > 0) {
                if (itemMeta != null) {
                    itemMeta.setCustomModelData(item_custom_model_data);
                }
            }

            if (isPaperVersionAtLeast(1, 21, 5)) {
                if (guis.contains("gui.tag-actions-menu.functions." + name + ".hide-tooltip") && hideToolTip) {
                    itemMeta.setHideTooltip(true);
                }
            }

            nbt.setString("name", name);

            item_displayname = item_displayname.replace("%player%", menuUtil.getOwner().getName());

            String identifier = menuUtil.getIdentifier();

            item_displayname = item_displayname.replace("%tag%", SupremeTags.getInstance().getTagManager().getTag(identifier).getTag().get(0));
            item_displayname = item_displayname.replace("%identifier%", identifier);

            item_displayname = globalPlaceholders(menuUtil.getOwner(), item_displayname);

            if (item_lore != null || !item_lore.isEmpty()) {
                item_lore.replaceAll(s -> s.replace("%identifier%", UserData.getActive(menuUtil.getOwner().getUniqueId())));
                if (SupremeTags.getInstance().getTagManager().getTag(UserData.getActive(menuUtil.getOwner().getUniqueId())) != null) {
                    if (SupremeTags.getInstance().getTagManager().getTag(UserData.getActive(menuUtil.getOwner().getUniqueId())).getCurrentTag() != null) {
                        item_lore.replaceAll(s -> s.replace("%tag%", SupremeTags.getInstance().getTagManager().getTag(UserData.getActive(menuUtil.getOwner().getUniqueId())).getCurrentTag()));
                    } else {
                        item_lore.replaceAll(s -> s.replace("%tag%", SupremeTags.getInstance().getTagManager().getTag(UserData.getActive(menuUtil.getOwner().getUniqueId())).getTag().get(0)));
                    }
                } else {
                    item_lore.replaceAll(s -> s.replace("%tag%", ""));
                }

                item_lore.replaceAll(s -> globalPlaceholders(menuUtil.getOwner(), s));
            } else {
                item_lore = new ArrayList<>();
            }

            itemMeta.setLore(color(item_lore));

            itemMeta.setDisplayName(format(item_displayname));
            itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            try {
                ItemFlag hideDye = ItemFlag.valueOf("HIDE_DYE");
                itemMeta.addItemFlags(hideDye);
            } catch (IllegalArgumentException ignored) {
                // HIDE_DYE not available in this version — skip
            }
            itemMeta.addItemFlags(ItemFlag.HIDE_DESTROYS);
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            itemMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

            nbt.getItem().setItemMeta(itemMeta);
            nbt.setString("name", name);

            if (!isSlots) {
                inventory.setItem(item_slot, nbt.getItem());
            } else {
                for (int slot : slots) {
                    inventory.setItem(slot, nbt.getItem());
                }
            }
        }
    }
}
