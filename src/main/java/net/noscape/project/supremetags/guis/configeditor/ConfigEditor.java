package net.noscape.project.supremetags.guis.configeditor;

import com.cryptomorin.xseries.XMaterial;
import de.tr7zw.nbtapi.NBTItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.handlers.menu.MenuUtil;
import net.noscape.project.supremetags.handlers.menu.Paged;
import net.noscape.project.supremetags.utils.Utils;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConfigEditor extends Paged {

    private final FileConfiguration config = SupremeTags.getInstance().getConfig();
    private final FileConfiguration messages = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get();
    private String sectionPath = "";
    private List<String> allKeys = new ArrayList<>();

    public ConfigEditor(MenuUtil menuUtil) {
        this(menuUtil, "");
    }

    public ConfigEditor(MenuUtil menuUtil, String sectionPath) {
        super(menuUtil);
        this.sectionPath = sectionPath != null ? sectionPath : "";
        enableAutoUpdate(true);
    }

    @Override
    public String getMenuName() {
        if (sectionPath.isEmpty()) {
            return "Config Editor";
        }
        return "Config Editor > " + sectionPath;
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {

        e.setCancelled(true);

        if (e.getCurrentItem() == null)
            return;

        Player player = (Player) e.getWhoClicked();
        NBTItem nbt = new NBTItem(e.getCurrentItem());

        if (nbt.hasTag("configPath")) {

            String path = nbt.getString("configPath");
            Object value = config.get(path);

            if (value instanceof Boolean bool) {
                config.set(path, !bool);
                SupremeTags.getInstance().saveConfig();
                SupremeTags.getInstance().reload();
                super.refresh();
                return;
            }

            if (value instanceof ConfigurationSection) {
                player.closeInventory();
                new ConfigEditor(SupremeTags.getMenuUtil(player), path).open();
                return;
            }

            if (value instanceof String str) {
                openStringEditor(player, path, str);
                return;
            }

            if (value instanceof Number num) {
                openNumberEditor(player, path, num);
                return;
            }

            if (value instanceof List<?> list) {
                openListEditor(player, path, list);
                return;
            }

        } else if (nbt.hasTag("configSectionBack")) {
            player.closeInventory();
            if (sectionPath.isEmpty()) {
                return;
            }
            String parentPath = sectionPath.contains(".")
                    ? sectionPath.substring(0, sectionPath.lastIndexOf('.'))
                    : "";
            new ConfigEditor(SupremeTags.getMenuUtil(player), parentPath).open();
            return;
        } else if (nbt.hasTag("name")) {
            String name = nbt.getString("name");

            if (name.equalsIgnoreCase("close")) {
                e.getWhoClicked().closeInventory();
            } else if (name.equalsIgnoreCase("next")) {
                if (allKeys.size() > maxItems && currentItemsOnPage >= maxItems) {
                    if (!((index + 1) >= allKeys.size())) {
                        page = page + 1;
                        super.refresh();
                    } else {
                        e.setCancelled(true);
                    }
                } else {
                    e.setCancelled(true);
                }
            } else if (name.equalsIgnoreCase("back")) {
                if (page != 0) {
                    page = page - 1;
                    super.refresh();
                }
            }
        }
    }

    @Override
    public void setMenuItems() {
        gatherKeys();
        getConfigKeysCountOnPage();
        applyLayout(false, false, false, true);
        addSectionBackButton();
        populatePage();
    }

    private void gatherKeys() {
        allKeys = new ArrayList<>();

        ConfigurationSection section = sectionPath.isEmpty() ? config : config.getConfigurationSection(sectionPath);
        if (section == null)
            return;

        for (String key : section.getKeys(false)) {
            String fullPath = sectionPath.isEmpty() ? key : sectionPath + "." + key;
            allKeys.add(fullPath);
        }
    }

    private void getConfigKeysCountOnPage() {
        if (allKeys.isEmpty()) {
            currentItemsOnPage = 0;
            return;
        }

        int startIndex = page * maxItems;
        int endIndex = Math.min(startIndex + maxItems, allKeys.size());

        currentItemsOnPage = 0;

        for (int i = startIndex; i < endIndex; i++) {
            currentItemsOnPage++;
        }
    }

    private void populatePage() {
        if (allKeys.isEmpty())
            return;

        int startIndex = page * maxItems;
        int endIndex = Math.min(startIndex + maxItems, allKeys.size());

        for (int i = startIndex; i < endIndex; i++) {
            String fullPath = allKeys.get(i);
            String key = fullPath.substring(fullPath.lastIndexOf('.') + 1);
            Object value = config.get(fullPath);

            ItemStack item = createItem(fullPath, key, value);
            inventory.addItem(item);
        }
    }

    private ItemStack createItem(String path, String key, Object value) {

        Material material;

        if (value instanceof Boolean bool) {
            material = bool
                    ? XMaterial.LIME_CONCRETE.parseMaterial()
                    : XMaterial.RED_CONCRETE.parseMaterial();

        } else if (value instanceof Number) {
            material = XMaterial.CLOCK.parseMaterial();
        } else if (value instanceof String) {
            material = XMaterial.OAK_SIGN.parseMaterial();

        } else if (value instanceof java.util.List<?>) {
            material = XMaterial.CHEST.parseMaterial();

        } else {
            material = XMaterial.BOOK.parseMaterial();
        }

        ItemStack item = new ItemStack(material);

        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(key)
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();

        lore.add(Component.empty());

        if (value instanceof Boolean bool) {

            lore.add(Component.text("Value: ")
                    .color(NamedTextColor.GRAY)
                    .append(Component.text(bool)
                            .color(bool ? NamedTextColor.GREEN : NamedTextColor.RED)));

            lore.add(Component.empty());
            lore.add(Component.text("Click to toggle")
                    .color(NamedTextColor.YELLOW));

        } else if (value instanceof ConfigurationSection) {

            lore.add(Component.text("Open section")
                    .color(NamedTextColor.GOLD));

            lore.add(Component.empty());
            lore.add(Component.text("Click to navigate")
                    .color(NamedTextColor.YELLOW));

        } else if (value instanceof List<?> list) {

            lore.add(Component.text("Entries: " + list.size())
                    .color(NamedTextColor.GRAY));

            lore.add(Component.empty());

            for (Object obj : list.stream().limit(5).toList()) {
                lore.add(Component.text("- " + obj).color(NamedTextColor.DARK_GRAY));
            }

            if (list.size() > 5) {
                lore.add(Component.text("... and " + (list.size() - 5) + " more")
                        .color(NamedTextColor.DARK_GRAY));
            }

            lore.add(Component.empty());
            lore.add(Component.text("Click to add item")
                    .color(NamedTextColor.YELLOW));

        } else {

            lore.add(Component.text("Value: ")
                    .color(NamedTextColor.GRAY)
                    .append(Component.text(String.valueOf(value))
                            .color(NamedTextColor.WHITE)));

            lore.add(Component.empty());
            lore.add(Component.text("Click to edit")
                    .color(NamedTextColor.YELLOW));

        }

        meta.lore(lore);
        item.setItemMeta(meta);

        NBTItem nbt = new NBTItem(item);
        nbt.setString("configPath", path);

        return nbt.getItem();
    }

    private void addSectionBackButton() {
        if (sectionPath.isEmpty())
            return;

        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();

        backMeta.displayName(Component.text("Back")
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> backLore = new ArrayList<>();
        backLore.add(Component.text("Go back to parent section")
                .color(NamedTextColor.GRAY));
        backMeta.lore(backLore);

        backItem.setItemMeta(backMeta);

        NBTItem backNbt = new NBTItem(backItem);
        backNbt.setString("configSectionBack", "true");

        inventory.setItem(45, backNbt.getItem());
    }

    private ItemStack createInputItem(String currentValue) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Current: " + (currentValue != null ? currentValue : "none"))
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Type new value in the anvil")
                .color(NamedTextColor.GRAY));
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private void openStringEditor(Player player, String path, String currentValue) {
        new AnvilGUI.Builder()
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }

                    String text = stateSnapshot.getText();

                    Utils.runMain(() -> {
                        config.set(path, text);
                        SupremeTags.getInstance().saveConfig();
                        SupremeTags.getInstance().reload();
                        super.refresh();
                        String updated = messages.getString("messages.config-value-updated",
                                        "%prefix% &7Configuration value updated.")
                                .replace("%prefix%", messages.getString("messages.prefix"));
                        Utils.msgPlayer(player, updated);
                    });

                    return List.of(AnvilGUI.ResponseAction.close());
                })
                .itemLeft(createInputItem(currentValue))
                .text(currentValue != null ? currentValue : "")
                .title("Edit String Value")
                .plugin(SupremeTags.getInstance())
                .open(player);
    }

    private void openNumberEditor(Player player, String path, Number currentValue) {
        new AnvilGUI.Builder()
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }

                    String text = stateSnapshot.getText();

                    Utils.runMain(() -> {
                        try {
                            if (currentValue instanceof Integer) {
                                config.set(path, Integer.parseInt(text));
                            } else if (currentValue instanceof Double) {
                                config.set(path, Double.parseDouble(text));
                            } else if (currentValue instanceof Long) {
                                config.set(path, Long.parseLong(text));
                            } else if (currentValue instanceof Float) {
                                config.set(path, Float.parseFloat(text));
                            } else {
                                config.set(path, Double.parseDouble(text));
                            }
                            SupremeTags.getInstance().saveConfig();
                            SupremeTags.getInstance().reload();
                            super.refresh();
                            String updated = messages.getString("messages.config-value-updated",
                                            "%prefix% &7Configuration value updated.")
                                    .replace("%prefix%", messages.getString("messages.prefix"));
                            Utils.msgPlayer(player, updated);
                        } catch (NumberFormatException ex) {
                            String invalid = messages.getString("messages.editor.invalid-number",
                                            "%prefix% &cInvalid number, please enter a valid number.")
                                    .replace("%prefix%", messages.getString("messages.prefix"));
                            Utils.msgPlayer(player, invalid);
                        }
                    });

                    return List.of(AnvilGUI.ResponseAction.close());
                })
                .itemLeft(createInputItem(String.valueOf(currentValue)))
                .text(String.valueOf(currentValue))
                .title("Edit Number Value")
                .plugin(SupremeTags.getInstance())
                .open(player);
    }

    private void openListEditor(Player player, String path, List<?> currentList) {
        new AnvilGUI.Builder()
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }

                    String text = stateSnapshot.getText();

                    if (text.isEmpty()) {
                        return List.of(AnvilGUI.ResponseAction.close());
                    }

                    Utils.runMain(() -> {
                        List<String> newList = new ArrayList<>();
                        if (currentList != null) {
                            for (Object obj : currentList) {
                                newList.add(String.valueOf(obj));
                            }
                        }
                        newList.add(text);
                        config.set(path, newList);
                        SupremeTags.getInstance().saveConfig();
                        SupremeTags.getInstance().reload();
                        super.refresh();
                        String updated = messages.getString("messages.config-value-updated",
                                        "%prefix% &7Configuration value updated.")
                                .replace("%prefix%", messages.getString("messages.prefix"));
                        Utils.msgPlayer(player, updated);
                    });

                    return List.of(AnvilGUI.ResponseAction.close());
                })
                .itemLeft(createInputItem("Add new item"))
                .text("")
                .title("Add List Item")
                .plugin(SupremeTags.getInstance())
                .open(player);
    }
}
