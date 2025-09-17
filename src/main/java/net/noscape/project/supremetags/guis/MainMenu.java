package net.noscape.project.supremetags.guis;

import de.tr7zw.nbtapi.NBTItem;
import dev.lone.itemsadder.api.FontImages.FontImageWrapper;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.handlers.menu.Menu;
import net.noscape.project.supremetags.handlers.menu.MenuUtil;
import net.noscape.project.supremetags.storage.UserData;
import net.noscape.project.supremetags.utils.ItemResolver;
import net.noscape.project.supremetags.utils.SkullUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

import static net.noscape.project.supremetags.utils.Utils.*;

public class MainMenu extends Menu {

    private final List<String> catorgies;
    private final Map<Integer, String> dataItem = new HashMap<>();
    private final Map<String, Integer> categoriesTags;

    private FileConfiguration guis = SupremeTags.getInstance().getConfigManager().getConfig("guis.yml").get();

    public MainMenu(MenuUtil menuUtil) {
        super(menuUtil);
        this.catorgies = SupremeTags.getInstance().getCategoryManager().getCatorgies();
        this.categoriesTags = SupremeTags.getInstance().getCategoryManager().getCatorgiesTags();
    }

    @Override
    public String getMenuName() {
        String title = format(guis.getString("gui.main-menu.title"));
        title = globalPlaceholders(menuUtil.getOwner(), title);
        return title;
    }

    @Override
    public int getSlots() {
        return guis.getInt("gui.main-menu.size");
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();

        if (e.getClickedInventory().getType() == InventoryType.PLAYER) {
            e.setCancelled(true);
            return;
        }

        ItemStack clickedItem = e.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        // Handle NBT click commands
        NBTItem nbt = new NBTItem(clickedItem);
        if (nbt.hasTag("supremetags_click_commands")) {
            e.setCancelled(true); // cancel event if interacting with a custom item

            String rawCommands = nbt.getString("supremetags_click_commands");
            List<String> clickCommands = Arrays.asList(rawCommands.split(";;"));

            for (String option : clickCommands) {
                option = replacePlaceholders(player, option);

                if (option.toLowerCase().startsWith("[message]")) {
                    String message = option.substring(9).trim();
                    msgPlayer(player, message);
                } else if (option.toLowerCase().startsWith("[player]")) {
                    String command = option.substring(8).trim().replaceAll("%player%", player.getName());
                    player.performCommand(command);
                } else if (option.toLowerCase().startsWith("[console]")) {
                    String command = option.substring(9).trim().replaceAll("%player%", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                } else if (option.toLowerCase().startsWith("[broadcast]")) {
                    String message = option.substring(11).trim();
                    Bukkit.broadcastMessage(message);
                } else if (option.toLowerCase().startsWith("[close]")) {
                    player.closeInventory();
                }
            }
            return;
        }

        // Category slot logic
        String category = dataItem.get(e.getSlot());

        if (category != null) {
            int slot = SupremeTags.getInstance().getCategoryManager().getCatConfig().getInt("categories." + category + ".slot");
            String permission = SupremeTags.getInstance().getCategoryManager().getCatConfig().getString("categories." + category + ".permission");

            String prefix = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.prefix");
            String no_access_category = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.no-access-category").replace("%prefix%", prefix);
            String no_tags_category = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.no-tags-category").replace("%prefix%", prefix);

            boolean hasMinTags = false;
            for (String cats : getCatorgies()) {
                if (cats != null && categoriesTags.get(cats) != null) {
                    hasMinTags = true;
                    break;
                }
            }

            if (e.getSlot() == slot) {
                if (hasMinTags) {
                    if (permission != null && player.hasPermission(permission)) {
                        menuUtil.setCategory(category);
                        new CategoryMenu(SupremeTags.getMenuUtil(player)).open();
                    } else {
                        msgPlayer(player, no_access_category);
                    }
                } else {
                    e.setCancelled(true);
                    msgPlayer(player, no_tags_category);
                }
            }
        }
    }

    @Override
    public void setMenuItems() {
        for (String cats : getCatorgies()) {
            if (cats != null) {
                boolean canSee = SupremeTags.getInstance().getCategoryManager().getCatConfig().getBoolean("categories." + cats + ".permission-see-category");
                boolean glow = SupremeTags.getInstance().getCategoryManager().getCatConfig().getBoolean("categories." + cats + ".glow");
                String permission = SupremeTags.getInstance().getCategoryManager().getCatConfig().getString("categories." + cats + ".permission");
                String material = SupremeTags.getInstance().getCategoryManager().getCatConfig().getString("categories." + cats + ".material");
                int slot = SupremeTags.getInstance().getCategoryManager().getCatConfig().getInt("categories." + cats + ".slot");
                int custom_model_data = SupremeTags.getInstance().getCategoryManager().getCatConfig().getInt("categories." + cats + ".custom-model-data");
                String displayname = SupremeTags.getInstance().getCategoryManager().getCatConfig().getString("categories." + cats + ".id_display");

                displayname = replacePlaceholders(menuUtil.getOwner(), displayname);

                if (SupremeTags.getInstance().isItemsAdder()) {
                    displayname = FontImageWrapper.replaceFontImages(displayname);
                }

                if (permission != null && menuUtil.getOwner().hasPermission(permission) && canSee) {
                    assert material != null;
                    ItemStack cat_item;
                    ItemMeta cat_itemMeta;

                    if (material.contains("hdb-")) {
                        int id = Integer.parseInt(material.replaceAll("hdb-", ""));
                        HeadDatabaseAPI api = new HeadDatabaseAPI();
                        cat_item = api.getItemHead(String.valueOf(id));
                        cat_itemMeta = cat_item.getItemMeta();
                    } else if (material.contains("basehead-")) {
                        String id = material.replaceAll("basehead-", "");
                        cat_item = SkullUtil.getSkullByBase64EncodedTextureUrl(SupremeTags.getInstance(), id);
                        cat_itemMeta = cat_item.getItemMeta();
                    } else if (material.contains("itemsadder-")) {
                        String id = material.replaceAll("itemsadder-", "");
                        cat_item = getItemWithIA(id);
                        cat_itemMeta = cat_item.getItemMeta();
                    } else if (material.contains("nexo-")) {
                        String id = material.replace("nexo-", "");
                        cat_item = SupremeTags.getInstance().getItemWithNexo(id);
                        cat_itemMeta = cat_item.getItemMeta();
                    } else {
                        cat_item = new ItemStack(Material.valueOf(material.toUpperCase()), 1);
                        cat_itemMeta = cat_item.getItemMeta();
                    }

                    cat_itemMeta.setDisplayName(format(displayname));

                    if (glow) {
                        cat_itemMeta.addEnchant(Enchantment.KNOCKBACK, 1, true);
                    }

                    cat_itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

                    try {
                        ItemFlag hideDye = ItemFlag.valueOf("HIDE_DYE");
                        cat_itemMeta.addItemFlags(hideDye);
                    } catch (IllegalArgumentException ignored) {
                        // HIDE_DYE not available
                    }

                    cat_itemMeta.addItemFlags(ItemFlag.HIDE_DESTROYS);

                    if (custom_model_data > 0) {
                        cat_itemMeta.setCustomModelData(custom_model_data);
                    }

                    // Set lore
                    ArrayList<String> lore = (ArrayList<String>) SupremeTags.getInstance().getCategoryManager().getCatConfig().getStringList("categories." + cats + ".lore");
                    if (categoriesTags.get(cats) != null) {
                        lore.replaceAll(s -> ChatColor.translateAlternateColorCodes('&', s).replaceAll("%tags_amount%", String.valueOf(categoriesTags.get(cats))));
                    } else {
                        lore.replaceAll(s -> ChatColor.translateAlternateColorCodes('&', s).replaceAll("%tags_amount%", String.valueOf(0)));
                    }

                    cat_itemMeta.setLore(color(lore));

                    cat_item.setItemMeta(cat_itemMeta);

                    dataItem.put(slot, cats);

                    inventory.setItem(slot, cat_item);
                } else if (permission != null && !menuUtil.getOwner().hasPermission(permission) && !canSee) {
                    assert material != null;
                    ItemStack cat_item;
                    ItemMeta cat_itemMeta;

                    if (material.contains("hdb-")) {
                        int id = Integer.parseInt(material.replaceAll("hdb-", ""));
                        HeadDatabaseAPI api = new HeadDatabaseAPI();
                        cat_item = api.getItemHead(String.valueOf(id));
                        cat_itemMeta = cat_item.getItemMeta();
                    } else if (material.contains("basehead-")) {
                        String id = material.replaceAll("basehead-", "");
                        cat_item = SkullUtil.getSkullByBase64EncodedTextureUrl(SupremeTags.getInstance(), id);
                        cat_itemMeta = cat_item.getItemMeta();
                    } else if (material.contains("itemsadder-")) {
                        String id = material.replaceAll("itemsadder-", "");
                        cat_item = getItemWithIA(id);
                        cat_itemMeta = cat_item.getItemMeta();
                    } else if (material.contains("nexo-")) {
                        String id = material.replace("nexo-", "");
                        cat_item = SupremeTags.getInstance().getItemWithNexo(id);
                        cat_itemMeta = cat_item.getItemMeta();
                    } else {
                        cat_item = new ItemStack(Material.valueOf(material.toUpperCase()), 1);
                        cat_itemMeta = cat_item.getItemMeta();
                    }

                    assert cat_itemMeta != null;
                    cat_itemMeta.setDisplayName(format(displayname));

                    if (glow) {
                        cat_itemMeta.addEnchant(Enchantment.KNOCKBACK, 1, true);
                    }

                    cat_itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                    try {
                        ItemFlag hideDye = ItemFlag.valueOf("HIDE_DYE");
                        cat_itemMeta.addItemFlags(hideDye);
                    } catch (IllegalArgumentException ignored) {
                        // HIDE_DYE not available
                    }
                    cat_itemMeta.addItemFlags(ItemFlag.HIDE_DESTROYS);

                    if (custom_model_data > 0) {
                        cat_itemMeta.setCustomModelData(custom_model_data);
                    }

                    // Set lore
                    ArrayList<String> lore = (ArrayList<String>) SupremeTags.getInstance().getCategoryManager().getCatConfig().getStringList("categories." + cats + ".lore");
                    if (categoriesTags.get(cats) != null) {
                        lore.replaceAll(s -> ChatColor.translateAlternateColorCodes('&', s).replaceAll("%tags_amount%", String.valueOf(categoriesTags.get(cats))));
                    } else {
                        lore.replaceAll(s -> ChatColor.translateAlternateColorCodes('&', s).replaceAll("%tags_amount%", String.valueOf(0)));
                    }
                    cat_itemMeta.setLore(color(lore));

                    cat_item.setItemMeta(cat_itemMeta);

                    dataItem.put(slot, cats);

                    inventory.setItem(slot, cat_item);
                } else if (permission != null && menuUtil.getOwner().hasPermission(permission) && !canSee) {
                    assert material != null;
                    ItemStack cat_item;
                    ItemMeta cat_itemMeta;

                    if (material.contains("hdb-")) {
                        int id = Integer.parseInt(material.replaceAll("hdb-", ""));
                        HeadDatabaseAPI api = new HeadDatabaseAPI();
                        cat_item = api.getItemHead(String.valueOf(id));
                        cat_itemMeta = cat_item.getItemMeta();
                    } else if (material.contains("basehead-")) {
                        String id = material.replaceAll("basehead-", "");
                        cat_item = SkullUtil.getSkullByBase64EncodedTextureUrl(SupremeTags.getInstance(), id);
                        cat_itemMeta = cat_item.getItemMeta();
                    } else if (material.contains("itemsadder-")) {
                        String id = material.replaceAll("itemsadder-", "");
                        cat_item = getItemWithIA(id);
                        cat_itemMeta = cat_item.getItemMeta();
                    } else if (material.contains("nexo-")) {
                        String id = material.replace("nexo-", "");
                        cat_item = SupremeTags.getInstance().getItemWithNexo(id);
                        cat_itemMeta = cat_item.getItemMeta();
                    } else if (material.contains("oraxen-")) {
                        String id = material.replace("oraxen-", "");
                        cat_item = getItemWithOraxen(id);
                        cat_itemMeta = cat_item.getItemMeta();
                    } else {
                        cat_item = new ItemStack(Material.valueOf(material.toUpperCase()), 1);
                        cat_itemMeta = cat_item.getItemMeta();
                    }

                    assert cat_itemMeta != null;
                    cat_itemMeta.setDisplayName(format(displayname));
                    cat_itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                    try {
                        ItemFlag hideDye = ItemFlag.valueOf("HIDE_DYE");
                        cat_itemMeta.addItemFlags(hideDye);
                    } catch (IllegalArgumentException ignored) {
                        // HIDE_DYE not available in this version — skip
                    }
                    cat_itemMeta.addItemFlags(ItemFlag.HIDE_DESTROYS);

                    if (custom_model_data > 0) {
                        cat_itemMeta.setCustomModelData(custom_model_data);
                    }

                    // Set lore
                    ArrayList<String> lore = (ArrayList<String>) SupremeTags.getInstance().getCategoryManager().getCatConfig().getStringList("categories." + cats + ".lore");
                    lore.replaceAll(s -> ChatColor.translateAlternateColorCodes('&', s).replaceAll("%tags_amount%", String.valueOf(categoriesTags.get(cats))));
                    cat_itemMeta.setLore(color(lore));

                    cat_item.setItemMeta(cat_itemMeta);

                    dataItem.put(slot, cats);

                    inventory.setItem(slot, cat_item);
                }
            }
        }

        for (String name : guis.getConfigurationSection("gui.main-menu.custom-items").getKeys(false)) {
            boolean enable = guis.getBoolean("gui.main-menu.custom-items." + name + ".enable");

            if (enable) {
                String item_material = guis.getString("gui.main-menu.custom-items." + name + ".material");
                String item_displayname = guis.getString("gui.main-menu.custom-items." + name + ".displayname");
                int item_custom_model_data = guis.getInt("gui.main-menu.custom-items." + name + ".custom-model-data");
                int item_slot = guis.getInt("gui.main-menu.custom-items." + name + ".slot");
                boolean glow = guis.getBoolean("gui.main-menu.custom-items." + name + ".glow");
                List<Integer> slots = new ArrayList<>();
                boolean isSlots = false;

                if (guis.contains("gui.main-menu.custom-items." + name + ".slots")) {
                    slots = guis.getIntegerList("gui.main-menu.custom-items." + name + ".slots");
                    isSlots = true;
                }

                if (!isSlots && guis.contains("gui.main-menu.custom-items." + name + ".slot")) {
                    item_slot = guis.getInt("gui.main-menu.custom-items." + name + ".slot");
                }

                List<String> item_lore = new ArrayList<>();
                if (guis.contains("gui.main-menu.custom-items." + name + ".lore")) {
                    item_lore = guis.getStringList("gui.main-menu.custom-items." + name + ".lore");
                }

                ItemResolver.ResolvedItem resolved = ItemResolver.resolveCustomItem(menuUtil.getOwner(), item_material);
                ItemStack item = resolved.item();
                ItemMeta itemMeta = resolved.meta();

                // Placeholder replacements
                item_displayname = item_displayname.replace("%player%", menuUtil.getOwner().getName());
                String identifier = UserData.getActive(menuUtil.getOwner().getUniqueId());

                if (!identifier.equalsIgnoreCase("None")) {
                    item_displayname = item_displayname.replace("%identifier%", identifier);
                } else {
                    item_displayname = item_displayname.replace("%identifier%", Objects.requireNonNull(SupremeTags.getInstance().getConfig().getString("placeholders.tag.none-output")));
                }

                String tag;
                if (SupremeTags.getInstance().getTagManager().getTag(identifier) != null) {
                    if (SupremeTags.getInstance().getTagManager().getTag(identifier).getCurrentTag() != null) {
                        tag = SupremeTags.getInstance().getTagManager().getTag(identifier).getCurrentTag();
                    } else {
                        tag = SupremeTags.getInstance().getTagManager().getTag(identifier).getTag().get(0);
                    }
                } else {
                    tag = "";
                }

                item_displayname = item_displayname.replace("%tag%", tag);
                item_displayname = globalPlaceholders(menuUtil.getOwner(), item_displayname);

                if (!item_lore.isEmpty()) {
                    item_lore.replaceAll(s -> s.replace("%identifier%", identifier));
                    item_lore.replaceAll(s -> s.replace("%tag%", tag));
                    item_lore.replaceAll(s -> globalPlaceholders(menuUtil.getOwner(), s));
                    itemMeta.setLore(color(item_lore));
                }

                itemMeta.setDisplayName(format(item_displayname));

                if (item_custom_model_data > 0) {
                    itemMeta.setCustomModelData(item_custom_model_data);
                }

                if (glow) {
                    itemMeta.addEnchant(Enchantment.KNOCKBACK, 1, true);
                }

                itemMeta.addItemFlags(
                        ItemFlag.HIDE_ATTRIBUTES,
                        ItemFlag.HIDE_DESTROYS,
                        ItemFlag.HIDE_ENCHANTS,
                        ItemFlag.HIDE_UNBREAKABLE
                );

                try {
                    ItemFlag hideDye = ItemFlag.valueOf("HIDE_DYE");
                    itemMeta.addItemFlags(hideDye);
                } catch (IllegalArgumentException ignored) {
                    // HIDE_DYE not available
                }

                // ✅ Apply meta before wrapping in NBTItem
                item.setItemMeta(itemMeta);

                // ✅ Wrap the updated item in NBT
                NBTItem nbt = new NBTItem(item);

                List<String> clickCommandsList = guis.getStringList("gui.main-menu.custom-items." + name + ".click-commands");
                if (!clickCommandsList.isEmpty()) {
                    nbt.setString("supremetags_click_commands", String.join(";;", clickCommandsList));
                }

                if (!isSlots) {
                    inventory.setItem(item_slot, nbt.getItem());
                } else {
                    for (int slot : slots) {
                        inventory.setItem(slot, nbt.getItem());
                    }
                }
            }
        }

        if (SupremeTags.getInstance().getCategoryManager().getCatConfig().getBoolean("categories-menu-fill-empty")) {
            fillEmpty();
        }
    }

    public List<String> getCatorgies() {
        return catorgies;
    }

    public Map<Integer, String> getDataItem() {
        return dataItem;
    }
}
