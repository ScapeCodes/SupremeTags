package net.noscape.project.supremetags.handlers.menu;

import de.rapha149.signgui.SignGUI;
import de.rapha149.signgui.exception.SignGUIVersionException;
import de.tr7zw.nbtapi.NBTItem;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.api.events.TagAssignEvent;
import net.noscape.project.supremetags.api.events.TagResetEvent;
import net.noscape.project.supremetags.guis.search.SearchResultMenu;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.storage.UserData;
import net.noscape.project.supremetags.utils.ItemResolver;
import net.noscape.project.supremetags.utils.SkullUtil;
import net.noscape.project.supremetags.utils.Utils;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.noscape.project.supremetags.utils.Utils.*;
import static net.noscape.project.supremetags.utils.Utils.globalPlaceholders;

public abstract class Paged extends Menu {

    private final FileConfiguration guis = SupremeTags.getInstance().getConfigManager().getConfig("guis.yml").get();
    private final FileConfiguration messages = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get();
    private final FileConfiguration Cat_Config = SupremeTags.getInstance().getConfigManager().getConfig("categories.yml").get();
    private final FileConfiguration rarityConfig = SupremeTags.getInstance().getConfigManager().getConfig("rarities.yml").get();

    protected int page = 0;
    protected int maxItems = guis.getInt("gui.tag-menu.tags-per-page");
    protected int index = 0;
    private final int tagsCount;
    public static int currentItemsOnPage = 0;
    protected boolean isLast;

    public Paged(MenuUtil menuUtil) {
        super(menuUtil);

        Map<String, Tag> tags = SupremeTags.getInstance().getTagManager().getTags();
        ArrayList<Tag> tag = new ArrayList<>(tags.values());

        tagsCount = tag.size();
    }

    public void applyEditorLayout() {
        String back = guis.getString("gui.items.back.displayname");
        String backMaterial = guis.getString("gui.items.back.material");
        int back_slot = guis.getInt("gui.items.back.slot");
        int backCmd = guis.getInt("gui.items.back.custom-model-data");
        List<String> back_lore = guis.getStringList("gui.items.back.lore");

        String close = guis.getString("gui.items.close.displayname");
        String closeMaterial = guis.getString("gui.items.close.material");
        int close_slot = guis.getInt("gui.items.close.slot");
        int closeCmd = guis.getInt("gui.items.close.custom-model-data");
        List<String> close_lore = guis.getStringList("gui.items.close.lore");

        String next = guis.getString("gui.items.next.displayname");
        String nextMaterial = guis.getString("gui.items.next.material");
        int next_slot = guis.getInt("gui.items.next.slot");
        int nextCmd = guis.getInt("gui.items.next.custom-model-data");
        List<String> next_lore = guis.getStringList("gui.items.next.lore");

        if (backMaterial != null && page > 0) {
            inventory.setItem(back_slot, createCustomItem(backMaterial, back, backCmd, back_lore));
        }

        if (closeMaterial != null) {
            inventory.setItem(close_slot, createCustomItem(closeMaterial, close, closeCmd, close_lore));
        }

        if (getCurrentItemsOnPage() > maxItems) {
            if (nextMaterial != null) {
                inventory.setItem(next_slot, createCustomItem(nextMaterial, next, nextCmd, next_lore));
            }
        }

        for (int i = 36; i <= 44; i++) {
            inventory.setItem(i, makeItem(Material.GRAY_STAINED_GLASS_PANE, "&6", 0, true));
        }
    }

    public void applyLayout(boolean ptags, boolean categories, boolean variantsMenu) {
        if (SupremeTags.getInstance().getLayout().equalsIgnoreCase("FULL")) {
            if (SupremeTags.getInstance().getConfig().getBoolean("gui.items.glass.enable")) {
                for (int i = 36; i <= 44; i++) {
                    String item_material = guis.getString("gui.items.glass.material");
                    String item_displayname = guis.getString("gui.items.glass.displayname");
                    int item_custom_model_data = guis.getInt("gui.items.glass.custom-model-data");

                    boolean hideToolTip = guis.getBoolean("gui.items.glass.hide-tooltip");

                    assert item_material != null;
                    inventory.setItem(i, makeItem(Material.valueOf(item_material.toUpperCase()), item_displayname, item_custom_model_data, hideToolTip));
                }
            }
        } else if (SupremeTags.getInstance().getLayout().equalsIgnoreCase("BORDER")) {
            for (int i = 0; i < 54; i++) {
                if (inventory.getItem(i) == null) {
                    if (i < 9 || i >= 45 || i % 9 == 0 || (i + 1) % 9 == 0) {
                        String item_material = guis.getString("gui.items.glass.material");
                        String item_displayname = guis.getString("gui.items.glass.displayname");
                        int item_custom_model_data = guis.getInt("gui.items.glass.custom-model-data");

                        boolean hideToolTip = guis.getBoolean("gui.items.glass.hide-tooltip");

                        assert item_material != null;
                        inventory.setItem(i, makeItem(Material.valueOf(item_material.toUpperCase()), item_displayname, item_custom_model_data, hideToolTip));
                    }
                }
            }
        }

        for (String str : guis.getConfigurationSection("gui.items").getKeys(false)) {
            boolean enabled = guis.getBoolean("gui.items." + str + ".enable");
            if (enabled && !str.equalsIgnoreCase("glass")) {

                if (!ptags && str.equalsIgnoreCase("create-tag")) continue;
                if (variantsMenu && str.equalsIgnoreCase("personal-tags")) continue;
                if (ptags && str.equalsIgnoreCase("personal-tags")) continue;
                if (variantsMenu && str.equalsIgnoreCase("create-tag")) continue;

                if (!SupremeTags.getInstance().getConfig().getBoolean("settings.personal-tags.enable") && str.equalsIgnoreCase("personal-tags"))
                    continue;

                if (!(tagsCount > maxItems & currentItemsOnPage >= maxItems)) {
                    if (str.equalsIgnoreCase("next"))
                        continue;
                }

                if (!ptags && !categories && !variantsMenu) {
                    if (!(page > 0)) {
                        if (str.equalsIgnoreCase("back")) {
                            continue;
                        }
                    }
                }

                if (variantsMenu || ptags) {
                    if (str.equalsIgnoreCase("filter")) continue;
                }

                if (ptags) {
                    if (str.equalsIgnoreCase("sort")) continue;
                }

                String item_material = guis.getString("gui.items." + str + ".material");
                String item_displayname = guis.getString("gui.items." + str + ".displayname");
                int item_custom_model_data = guis.getInt("gui.items." + str + ".custom-model-data");
                List<String> item_lore = guis.getStringList("gui.items." + str + ".lore");

                int item_slot = guis.getInt("gui.items." + str + ".slot"); // Default slot
                List<Integer> slots = new ArrayList<>();
                boolean isSlots = false;

                boolean hideToolTip = guis.getBoolean("gui.items." + str + ".hide-tooltip");

                if (guis.contains("gui.items." + str + ".slots")) {
                    slots = guis.getIntegerList("gui.items." + str + ".slots");
                    isSlots = true;
                }

                if (!isSlots && guis.contains("gui.items." + str + ".slot")) {
                    item_slot = guis.getInt("gui.items." + str + ".slot");
                }

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
                    if (guis.contains("gui.items." + str + ".hide-tooltip") && hideToolTip) {
                        itemMeta.setHideTooltip(true);
                    }
                }

                nbt.setString("name", str);

                item_displayname = item_displayname.replace("%player%", menuUtil.getOwner().getName());

                if (menuUtil.getFilter() == null) {
                    item_displayname = item_displayname.replace("%filter%", "All Tags");
                } else {
                    if (menuUtil.getFilter().startsWith("category:")) {
                        item_displayname = item_displayname.replace("%filter%", menuUtil.getFilter().replace("category:", ""));
                    } else if (menuUtil.getFilter().equalsIgnoreCase("players")) {
                        item_displayname = item_displayname.replace("%filter%", "Your Tags");
                    } else {
                        item_displayname = item_displayname.replace("%filter%", "All Tags");
                    }
                }

                if (menuUtil.getSort() == null) {
                    item_displayname = item_displayname.replace("%sort%", "&8No Filter!");
                } else {
                    if (menuUtil.getSort().startsWith("rarity:")) {
                        item_displayname = item_displayname.replace("%sort%", menuUtil.getSort().replace("rarity:", "").toUpperCase());
                    } else {
                        item_displayname = item_displayname.replace("%sort%", "&8No Filter!");
                    }
                }

                String identifier = UserData.getActive(menuUtil.getOwner().getUniqueId());

                if (!identifier.equalsIgnoreCase("None")) {
                    item_displayname = item_displayname.replace("%identifier%", identifier);
                    if (SupremeTags.getInstance().getTagManager().doesTagExist(identifier)) {
                        item_displayname = item_displayname.replace("%tag%", SupremeTags.getInstance().getTagManager().getTag(identifier).getTag().get(0));
                    } else if (SupremeTags.getInstance().getPlayerManager().doesTagExist(menuUtil.getOwner().getUniqueId(), identifier)) {
                        item_displayname = item_displayname.replace("%tag%", SupremeTags.getInstance().getPlayerManager().getTag(menuUtil.getOwner().getUniqueId(), identifier).getTag().get(0));
                    } else if (SupremeTags.getInstance().getTagManager().hasVariantTag(menuUtil.getOwner())) {
                        item_displayname = item_displayname.replace("%tag%", SupremeTags.getInstance().getTagManager().getVariantTag(menuUtil.getOwner()).getTag().get(0));
                    }
                } else {
                    item_displayname = item_displayname.replace("%identifier%", SupremeTags.getInstance().getConfig().getString("placeholders.tag.none-output"));
                    item_displayname = item_displayname.replace("%tag%", SupremeTags.getInstance().getConfig().getString("placeholders.tag.none-output"));
                }

                item_displayname = globalPlaceholders(menuUtil.getOwner(), item_displayname);

                if (item_lore != null || !item_lore.isEmpty()) {
                    String identifier_lore = UserData.getActive(menuUtil.getOwner().getUniqueId());

                    if (identifier_lore.equalsIgnoreCase("none")) {
                        identifier_lore = SupremeTags.getInstance().getConfig().getString("placeholders.tag.none-output");
                    }

                    String finalIdentifier_lore = identifier_lore;
                    item_lore.replaceAll(s -> s.replace("%identifier%", finalIdentifier_lore));
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

                if (str.equalsIgnoreCase("filter")) {
                    String filter = menuUtil.getFilter() != null ? menuUtil.getFilter() : "all";
                    Player player = menuUtil.getOwner();
                    List<String> newLore = new ArrayList<>();

                    List<String> cats = SupremeTags.getInstance().getCategoryManager().getCatorgies();
                    String selectedKey = "gui.items.filter.filters.selected.";
                    String unselectedKey = "gui.items.filter.filters.unselected.";

                    int amountAll = getTypeAmount(player, "all");
                    int amountYourTags = getTypeAmount(player, "yourtags");

                    if (filter.equalsIgnoreCase("players")) {
                        newLore.add(ChatColor.translateAlternateColorCodes('&',
                                guis.getString(unselectedKey + "all-tags") + " &7(" + amountAll + ")"));
                        newLore.add(ChatColor.translateAlternateColorCodes('&',
                                guis.getString(selectedKey + "your-tags") + " &7(" + amountYourTags + ")"));
                    } else if (filter.equalsIgnoreCase("all")) {
                        newLore.add(ChatColor.translateAlternateColorCodes('&',
                                guis.getString(selectedKey + "all-tags") + " &7(" + amountAll + ")"));
                        newLore.add(ChatColor.translateAlternateColorCodes('&',
                                guis.getString(unselectedKey + "your-tags") + " &7(" + amountYourTags + ")"));
                    } else {
                        newLore.add(ChatColor.translateAlternateColorCodes('&',
                                guis.getString(unselectedKey + "all-tags") + " &7(" + amountAll + ")"));
                        newLore.add(ChatColor.translateAlternateColorCodes('&',
                                guis.getString(unselectedKey + "your-tags") + " &7(" + amountYourTags + ")"));
                    }

                    ConfigurationSection cat_sec = this.Cat_Config.getConfigurationSection("categories");

                    if (!categories) {
                        for (String category : cats) {
                            boolean isSelected = filter.equalsIgnoreCase("category:" + category);
                            int amountCategory = getTypeAmount(player, "category:" + category);

                            String label = category.toUpperCase(); // fallback
                            if (cat_sec != null && cat_sec.isConfigurationSection(category)) {
                                ConfigurationSection categorySection = cat_sec.getConfigurationSection(category);
                                ConfigurationSection labelsSection = categorySection.getConfigurationSection("filter-labels");

                                if (labelsSection != null) {
                                    if (isSelected && labelsSection.contains("selected")) {
                                        label = labelsSection.getString("selected");
                                    } else if (!isSelected && labelsSection.contains("unselected")) {
                                        label = labelsSection.getString("unselected");
                                    }
                                }
                            }

                            String formatKey = isSelected ? selectedKey + "category" : unselectedKey + "category";
                            String formatTemplate = guis.getString(formatKey, "&7> %category%");
                            String formatted = formatTemplate.replace("%category%", label) + " &7(" + amountCategory + ")";
                            newLore.add(ChatColor.translateAlternateColorCodes('&', formatted));
                        }
                    }

                    List<String> finalLore = new ArrayList<>();
                    for (String line : item_lore) {
                        if (line.contains("%filter_lore%")) {
                            finalLore.addAll(newLore);
                        } else {
                            finalLore.add(line);
                        }
                    }
                    item_lore = finalLore;
                }

                if (str.equalsIgnoreCase("sort")) {
                    String sort = menuUtil.getSort() != null ? menuUtil.getSort() : "none";
                    Player player = menuUtil.getOwner();
                    List<String> newLore = new ArrayList<>();

                    Set<String> rarities = SupremeTags.getInstance().getRarityManager().getRarityMap().keySet();
                    String selectedKey = "gui.items.sort.sorts.selected.rarity";
                    String unselectedKey = "gui.items.sort.sorts.unselected.rarity";

                    // Handle "none" option (no filter)
                    String noneLabel;
                    if (sort.equalsIgnoreCase("none")) {
                        noneLabel = guis.getString("gui.items.sort.sorts.selected.no-filter", "&7None");
                    } else {
                        noneLabel = guis.getString("gui.items.sort.sorts.unselected.no-filter", "&7None");
                    }
                    int amountAll = getTypeAmount(player, "all");
                    newLore.add(ChatColor.translateAlternateColorCodes('&', noneLabel + " &7(" + amountAll + ")"));

                    for (String rarity : rarities) {
                        boolean isSelected = sort.equalsIgnoreCase("rarity:" + rarity);
                        int amountByRarity = getTypeAmount(player, "rarity:" + rarity);

                        String label = deformat(rarity); // fallback label

                        // Try to get label from rarities.yml
                        ConfigurationSection raritySection = rarityConfig.getConfigurationSection("rarities." + rarity);
                        if (raritySection != null && raritySection.isConfigurationSection("filter-labels")) {
                            ConfigurationSection labels = raritySection.getConfigurationSection("filter-labels");
                            if (labels != null) {
                                label = labels.getString(isSelected ? "selected" : "unselected", label);
                            }
                        }

                        // Get format template from GUI config
                        String template = guis.getString(isSelected ? selectedKey : unselectedKey, "&8&l> %rarity%");
                        String formatted = template.replace("%rarity%", label) + " &7(" + amountByRarity + ")";

                        newLore.add(ChatColor.translateAlternateColorCodes('&', formatted));
                    }

                    // Replace %sort_lore% in lore with newLore
                    List<String> finalLore = new ArrayList<>();
                    for (String line : item_lore) {
                        if (line.contains("%sort_lore%")) {
                            finalLore.addAll(newLore);
                        } else {
                            finalLore.add(ChatColor.translateAlternateColorCodes('&', line));
                        }
                    }
                    item_lore = finalLore;
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
                nbt.setString("name", str);

                if (!isSlots) {
                    inventory.setItem(item_slot, nbt.getItem());
                } else {
                    for (int slot : slots) {
                        inventory.setItem(slot, nbt.getItem());
                    }
                }
            }

            for (String cSTR : guis.getConfigurationSection("gui.tag-menu.custom-items").getKeys(false)) {
                boolean cEnable = guis.getBoolean("gui.tag-menu.custom-items." + cSTR + ".enable");

                if (cEnable) {
                    String item_material = guis.getString("gui.tag-menu.custom-items." + cSTR + ".material");
                    String item_displayname = guis.getString("gui.tag-menu.custom-items." + cSTR + ".displayname");
                    int item_custom_model_data = guis.getInt("gui.tag-menu.custom-items." + cSTR + ".custom-model-data");
                    int item_slot = 0;

                    boolean hideToolTip = guis.getBoolean("gui.tag-menu.custom-items." + cSTR + ".hide-tooltip");

                    boolean isSlots = false;

                    List<Integer> slots = new ArrayList<>();

                    if (guis.getConfigurationSection("gui.tag-menu.custom-items." + str + ".slots") != null) {
                        slots = guis.getIntegerList("gui.tag-menu.custom-items." + str + ".slots");
                        isSlots = true;
                    }

                    if (guis.getConfigurationSection("gui.tag-menu.custom-items." + str + ".slot") != null) {
                        item_slot = guis.getInt("gui.tag-menu.custom-items." + str + ".slot");
                    }

                    List<String> item_lore = new ArrayList<>();

                    if (guis.getConfigurationSection("gui.tag-menu.custom-items." + cSTR + ".lore") != null) {
                        item_lore = guis.getStringList("gui.tag-menu.custom-items." + cSTR + ".lore");
                    }

                    ItemResolver.ResolvedItem resolved = ItemResolver.resolveCustomItem(item_material);
                    ItemStack item = resolved.item();
                    ItemMeta itemMeta = resolved.meta();

                    if (item_custom_model_data > 0) {
                        if (itemMeta != null)
                            itemMeta.setCustomModelData(item_custom_model_data);
                    }

                    if (isPaperVersionAtLeast(1, 21, 5)) {
                        if (guis.contains("gui.tag-menu.custom-items." + cSTR + ".hide-tooltip") && hideToolTip) {
                            itemMeta.setHideTooltip(true);
                        }
                    }

                    item_displayname = item_displayname.replace("%player%", menuUtil.getOwner().getName());

                    String identifier = UserData.getActive(menuUtil.getOwner().getUniqueId());

                    if (!identifier.equalsIgnoreCase("None")) {
                        item_displayname = item_displayname.replace("%identifier%", UserData.getActive(menuUtil.getOwner().getUniqueId()));
                    } else {
                        item_displayname = item_displayname.replace("%identifier%", SupremeTags.getInstance().getConfig().getString("placeholders.tag.none-output"));
                    }

                    if (SupremeTags.getInstance().getTagManager().getTag(UserData.getActive(menuUtil.getOwner().getUniqueId())) != null) {
                        if (SupremeTags.getInstance().getTagManager().getTag(UserData.getActive(menuUtil.getOwner().getUniqueId())).getCurrentTag() != null) {
                            item_displayname = item_displayname.replace("%tag%", SupremeTags.getInstance().getTagManager().getTag(UserData.getActive(menuUtil.getOwner().getUniqueId())).getCurrentTag());
                        } else {
                            item_displayname = item_displayname.replace("%tag%", SupremeTags.getInstance().getTagManager().getTag(UserData.getActive(menuUtil.getOwner().getUniqueId())).getTag().get(0));
                        }
                    } else {
                        item_displayname = item_displayname.replace("%tag%", SupremeTags.getInstance().getConfig().getString("placeholders.tag.none-output"));
                    }

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

                    item.setItemMeta(itemMeta);
                    if (!isSlots) {
                        inventory.setItem(item_slot, item);
                    } else {
                        for (int slot : slots) {
                            inventory.setItem(slot, item);
                        }
                    }
                }
            }
        }
    }

    protected int getPage() {
        return page + 1;
    }

    public int getMaxItems() {
        return maxItems;
    }

    public void getTagsCountOnPage() {
        Map<String, Tag> tags = SupremeTags.getInstance().getTagManager().getTags();

        ArrayList<Tag> tag = new ArrayList<>(tags.values());

        if (!tag.isEmpty()) {

            int startIndex = page * maxItems;
            int endIndex = Math.min(startIndex + maxItems, tag.size());

            currentItemsOnPage = 0;

            for (int i = startIndex; i < endIndex; i++) {
                currentItemsOnPage++;
            }
        }
    }

    // ===================================================================================
    // EDITOR TAGS
    // ===================================================================================

    public void getTagItemsEditor() {
        Map<String, Tag> tags = SupremeTags.getInstance().getTagManager().getTags();
        ArrayList<Tag> tag = new ArrayList<>(tags.values());
        if (!tag.isEmpty()) {
            int maxItemsPerPage = 36;
            int startIndex = this.page * maxItemsPerPage;
            int endIndex = Math.min(startIndex + maxItemsPerPage, tag.size());
            tag.sort((tag1, tag2) -> {
                boolean hasPermission1 = this.menuUtil.getOwner().hasPermission(tag1.getPermission());
                boolean hasPermission2 = this.menuUtil.getOwner().hasPermission(tag2.getPermission());
                boolean isActiveUserTag1 = Objects.equals(UserData.getActive(this.menuUtil.getOwner().getUniqueId()), tag1.getIdentifier());
                boolean isActiveUserTag2 = Objects.equals(UserData.getActive(this.menuUtil.getOwner().getUniqueId()), tag2.getIdentifier());
                return (isActiveUserTag1 && !isActiveUserTag2) ? -1 : (

                        (!isActiveUserTag1 && isActiveUserTag2) ? 1 : (

                                (hasPermission1 && !hasPermission2) ? -1 : (

                                        (!hasPermission1 && hasPermission2) ? 1 : tag1.getIdentifier().compareTo(tag2.getIdentifier()))));
            });
            currentItemsOnPage = 0;
            for (int i = startIndex; i < endIndex; i++) {
                Tag t = tag.get(i);
                if (t != null) {
                    String permission = SupremeTags.getInstance().getTagManager().getTagConfig().getString("tags." + t.getIdentifier() + ".permission");
                    if (permission == null || SupremeTags.getInstance().getConfig().getBoolean("settings.locked-view") || SupremeTags.getInstance().getConfig().getBoolean("settings.cost-system") || this.menuUtil.getOwner().hasPermission(permission)) {
                        String displayname, material;
                        if (SupremeTags.getInstance().getTagManager().getTagConfig().getString("tags." + t.getIdentifier() + ".displayname") != null) {
                            if (t.getCurrentTag() != null) {
                                displayname = Objects.requireNonNull(SupremeTags.getInstance().getTagManager().getTagConfig().getString("tags." + t.getIdentifier() + ".displayname")).replace("%tag%", t.getCurrentTag());
                            } else {
                                displayname = Objects.requireNonNull(SupremeTags.getInstance().getTagManager().getTagConfig().getString("tags." + t.getIdentifier() + ".displayname")).replace("%tag%", t.getTag().get(0));
                            }
                        } else {
                            displayname = format("&7Tag: " + t.getTag().get(0));
                        }

                        if (SupremeTags.getInstance().getTagManager().getTagConfig().getString("tags." + t.getIdentifier() + ".display-item") != null) {
                            material = SupremeTags.getInstance().getTagManager().getTagConfig().getString("tags." + t.getIdentifier() + ".display-item");
                        } else {
                            material = "NAME_TAG";
                        }

                        displayname = globalPlaceholders(menuUtil.getOwner(), displayname);

                        assert permission != null;
                        if (material.contains("hdb-")) {
                            HeadDatabaseAPI api = new HeadDatabaseAPI();
                            int id = Integer.parseInt(material.replaceAll("hdb-", ""));
                            ItemStack tagItem = api.getItemHead(String.valueOf(id));
                            ItemMeta tagMeta = tagItem.getItemMeta();
                            assert tagMeta != null;
                            NBTItem nbt = new NBTItem(tagItem);
                            nbt.setString("identifier", t.getIdentifier());
                            tagMeta.setDisplayName(format(displayname));
                            tagMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                            try {
                                ItemFlag hideDye = ItemFlag.valueOf("HIDE_DYE");
                                tagItem.addItemFlags(hideDye);
                            } catch (IllegalArgumentException ignored) {
                                // HIDE_DYE not available in this version — skip
                            }
                            tagMeta.addItemFlags(ItemFlag.HIDE_DESTROYS);
                            tagMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                            tagMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
                            ArrayList<String> lore = (ArrayList<String>) SupremeTags.getInstance().getConfig().getStringList("gui.tag-editor-menu.tag-item.lore");

                            String descriptionPlaceholder = "%description%";
                            String identifierPlaceholder = "%identifier%";
                            String tagPlaceholder = "%tag%";
                            String costPlaceholder = "%cost%";
                            String costFormattedPlaceholder = "%cost_formatted%";
                            String costFormattedPlaceholderRaw = "%cost_formatted_raw%";
                            String variantsPlaceholder = "%variants%";
                            String joinedDescription = t.getDescription().stream().map(Utils::format).collect(Collectors.joining("\n"));

                            for (int l = 0; l < lore.size(); l++) {
                                String line = lore.get(l);

                                // Pattern for %custom-placeholder_?%
                                Pattern customPlaceholderPattern = Pattern.compile("%custom-placeholder_(.*?)%");
                                Matcher matcher = customPlaceholderPattern.matcher(line);

                                while (matcher.find()) {
                                    String dynamicPart = matcher.group(1);
                                    line = line.replace(matcher.group(0), t.getCustomPlaceholder(t.getIdentifier(), dynamicPart));
                                }

                                if (line.contains(descriptionPlaceholder)) {
                                    List<String> descriptionLines = Arrays.asList(joinedDescription.split("\n"));

                                    lore.remove(l);

                                    lore.addAll(l, descriptionLines);

                                    l += descriptionLines.size() - 1;
                                } else {
                                    line = line.replace(identifierPlaceholder, t.getIdentifier());
                                    line = line.replace(tagPlaceholder, t.getTag().get(0));
                                    line = line.replace(costFormattedPlaceholder, "$" + formatNumber(t.getEconomy().getAmount()));
                                    line = line.replace(costFormattedPlaceholderRaw, formatNumber(t.getEconomy().getAmount()));
                                    line = line.replace(costPlaceholder, String.valueOf(t.getEconomy().getAmount()));
                                    line = line.replace(variantsPlaceholder, String.valueOf(t.getVariants().size()));
                                    line = globalPlaceholders(menuUtil.getOwner(), line);

                                    lore.set(l, line);
                                }
                            }

                            tagMeta.setLore(color(lore));
                            nbt.getItem().setItemMeta(tagMeta);
                            nbt.setString("identifier", t.getIdentifier());
                            this.inventory.addItem(nbt.getItem());
                        } else if (material.contains("basehead-")) {
                            String id = material.replaceAll("basehead-", "");
                            ItemStack tagItem = SkullUtil.getSkullByBase64EncodedTextureUrl(SupremeTags.getInstance(), id);
                            ItemMeta tagMeta = tagItem.getItemMeta();
                            assert tagMeta != null;
                            NBTItem nbt = new NBTItem(tagItem);
                            nbt.setString("identifier", t.getIdentifier());
                            tagMeta.setDisplayName(format(displayname));
                            tagMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                            try {
                                ItemFlag hideDye = ItemFlag.valueOf("HIDE_DYE");
                                tagItem.addItemFlags(hideDye);
                            } catch (IllegalArgumentException ignored) {
                                // HIDE_DYE not available in this version — skip
                            }
                            tagMeta.addItemFlags(ItemFlag.HIDE_DESTROYS);
                            tagMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                            tagMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
                            ArrayList<String> lore = (ArrayList<String>) SupremeTags.getInstance().getConfig().getStringList("gui.tag-editor-menu.tag-item.lore");

                            String descriptionPlaceholder = "%description%";
                            String identifierPlaceholder = "%identifier%";
                            String tagPlaceholder = "%tag%";
                            String costPlaceholder = "%cost%";
                            String costFormattedPlaceholder = "%cost_formatted%";
                            String costFormattedPlaceholderRaw = "%cost_formatted_raw%";
                            String variantsPlaceholder = "%variants%";
                            String joinedDescription = t.getDescription().stream().map(Utils::format).collect(Collectors.joining("\n"));

                            for (int l = 0; l < lore.size(); l++) {
                                String line = lore.get(l);

                                // Pattern for %custom-placeholder_?%
                                Pattern customPlaceholderPattern = Pattern.compile("%custom-placeholder_(.*?)%");
                                Matcher matcher = customPlaceholderPattern.matcher(line);

                                while (matcher.find()) {
                                    String dynamicPart = matcher.group(1);
                                    line = line.replace(matcher.group(0), t.getCustomPlaceholder(t.getIdentifier(), dynamicPart));
                                }

                                if (line.contains(descriptionPlaceholder)) {
                                    List<String> descriptionLines = Arrays.asList(joinedDescription.split("\n"));

                                    lore.remove(l);

                                    lore.addAll(l, descriptionLines);

                                    l += descriptionLines.size() - 1;
                                } else {
                                    line = line.replace(identifierPlaceholder, t.getIdentifier());
                                    line = line.replace(tagPlaceholder, t.getTag().get(0));
                                    line = line.replace(costFormattedPlaceholder, "$" + formatNumber(t.getEconomy().getAmount()));
                                    line = line.replace(costFormattedPlaceholderRaw, formatNumber(t.getEconomy().getAmount()));
                                    line = line.replace(costPlaceholder, String.valueOf(t.getEconomy().getAmount()));
                                    line = line.replace(variantsPlaceholder, String.valueOf(t.getVariants().size()));
                                    line = globalPlaceholders(menuUtil.getOwner(), line);

                                    lore.set(l, line);
                                }
                            }

                            tagMeta.setLore(color(lore));
                            nbt.getItem().setItemMeta(tagMeta);
                            nbt.setString("identifier", t.getIdentifier());
                            this.inventory.addItem(nbt.getItem());
                        } else {
                            ItemStack tagItem = new ItemStack(Material.valueOf(material.toUpperCase()), 1);
                            ItemMeta tagMeta = tagItem.getItemMeta();
                            assert tagMeta != null;
                            NBTItem nbt = new NBTItem(tagItem);
                            nbt.setString("identifier", t.getIdentifier());
                            tagMeta.setDisplayName(format(displayname));
                            tagMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                            try {
                                ItemFlag hideDye = ItemFlag.valueOf("HIDE_DYE");
                                tagItem.addItemFlags(hideDye);
                            } catch (IllegalArgumentException ignored) {
                                // HIDE_DYE not available in this version — skip
                            }
                            tagMeta.addItemFlags(ItemFlag.HIDE_DESTROYS);
                            tagMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                            tagMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
                            ArrayList<String> lore = (ArrayList<String>) SupremeTags.getInstance().getConfig().getStringList("gui.tag-editor-menu.tag-item.lore");

                            String descriptionPlaceholder = "%description%";
                            String identifierPlaceholder = "%identifier%";
                            String tagPlaceholder = "%tag%";
                            String costPlaceholder = "%cost%";
                            String costFormattedPlaceholder = "%cost_formatted%";
                            String costFormattedPlaceholderRaw = "%cost_formatted_raw%";
                            String variantsPlaceholder = "%variants%";
                            String joinedDescription = t.getDescription().stream().map(Utils::format).collect(Collectors.joining("\n"));

                            for (int l = 0; l < lore.size(); l++) {
                                String line = lore.get(l);

                                // Pattern for %custom-placeholder_?%
                                Pattern customPlaceholderPattern = Pattern.compile("%custom-placeholder_(.*?)%");
                                Matcher matcher = customPlaceholderPattern.matcher(line);

                                while (matcher.find()) {
                                    String dynamicPart = matcher.group(1);
                                    line = line.replace(matcher.group(0), t.getCustomPlaceholder(t.getIdentifier(), dynamicPart));
                                }

                                if (line.contains(descriptionPlaceholder)) {
                                    List<String> descriptionLines = Arrays.asList(joinedDescription.split("\n"));

                                    lore.remove(l);

                                    lore.addAll(l, descriptionLines);

                                    l += descriptionLines.size() - 1;
                                } else {
                                    line = line.replace(identifierPlaceholder, t.getIdentifier());
                                    line = line.replace(tagPlaceholder, t.getTag().get(0));
                                    line = line.replace(costFormattedPlaceholder, "$" + formatNumber(t.getEconomy().getAmount()));
                                    line = line.replace(costFormattedPlaceholderRaw, formatNumber(t.getEconomy().getAmount()));
                                    line = line.replace(costPlaceholder, String.valueOf(t.getEconomy().getAmount()));
                                    line = line.replace(variantsPlaceholder, String.valueOf(t.getVariants().size()));
                                    line = globalPlaceholders(menuUtil.getOwner(), line);

                                    lore.set(l, line);
                                }
                            }

                            tagMeta.setLore(color(lore));
                            nbt.getItem().setItemMeta(tagMeta);
                            nbt.setString("identifier", t.getIdentifier());
                            this.inventory.addItem(nbt.getItem());
                        }
                        currentItemsOnPage++;
                    }
                }
            }
        }
    }

    public FileConfiguration getTagConfig() {
        return SupremeTags.getInstance().getConfigManager().getConfig("tags.yml").get();
    }

    public int getCurrentItemsOnPage() {
        return currentItemsOnPage;
    }

    public void increaseCurrentItemsOnPage() {
        currentItemsOnPage++;
    }

    public void openSearchContainer(Player player) {
        String search = SupremeTags.getInstance().getConfig().getString("settings.search-type");

        if (search.equalsIgnoreCase("SIGN")) {
            SignGUI gui;
            try {
                gui = SignGUI.builder()
                        .setLines(format(messages.getString("messages.sign-line-top")), null, null)
                        .setColor(DyeColor.YELLOW)

                        .setHandler((p, result) -> {
                            String line1 = result.getLineWithoutColor(1);

                            if (!line1.isEmpty()) {
                                if (SupremeTags.getInstance().getCategoryManager().isCategoryNearName(line1) || SupremeTags.getInstance().getTagManager().tagExistsNearName(line1)) {
                                    Bukkit.getScheduler().runTask(SupremeTags.getInstance(), () -> new SearchResultMenu(SupremeTags.getMenuUtil(player), line1).open());
                                } else {
                                    String search_invalid = messages.getString("messages.search-invalid-1").replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
                                    msgPlayer(player, search_invalid);
                                }
                            } else {
                                String search_invalid = messages.getString("messages.search-invalid-2").replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
                                msgPlayer(player, search_invalid);
                            }

                            return Collections.emptyList();
                        })

                        .build();
            } catch (SignGUIVersionException e) {
                throw new RuntimeException(e);
            }

            gui.open(player);
        } else if (search.equalsIgnoreCase("ANVIL")) {
            new AnvilGUI.Builder()
                    .onClick((slot, stateSnapshot) -> {
                        if (slot != AnvilGUI.Slot.OUTPUT) {
                            return Collections.emptyList();
                        }

                        String text = stateSnapshot.getText();

                        if (!text.isEmpty()) {
                            if (SupremeTags.getInstance().getCategoryManager().isCategoryNearName(text) ||
                                    SupremeTags.getInstance().getTagManager().tagExistsNearName(text)) {

                                Bukkit.getScheduler().runTask(SupremeTags.getInstance(), () ->
                                        new SearchResultMenu(SupremeTags.getMenuUtil(player), text).open()
                                );
                            } else {
                                String search_invalid = messages.getString("messages.search-invalid-1")
                                        .replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
                                msgPlayer(player, search_invalid);
                            }
                        } else {
                            String search_invalid = messages.getString("messages.search-invalid-2")
                                    .replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
                            msgPlayer(player, search_invalid);
                        }

                        return List.of(AnvilGUI.ResponseAction.close());
                    })
                    .itemLeft(new ItemStack(Material.PAPER))
                    .text("") // Optional: Initial text
                    .title(format(messages.getString("messages.anvil-title"))) // Optional: Title
                    .plugin(SupremeTags.getInstance())
                    .open(player);
        } else {
            msgPlayer(player, "&cInvalid Search type, use SIGN or ANVIL.");
        }
    }

    protected Comparator<Tag> getTagComparator(boolean prioritiseSelectedTag, MenuUtil menuUtil) {
        return (tag1, tag2) -> {

            // 3. Then check permission
            boolean hasPermission1 = menuUtil.getOwner().hasPermission(tag1.getPermission());
            boolean hasPermission2 = menuUtil.getOwner().hasPermission(tag2.getPermission());

            if (hasPermission1 != hasPermission2) {
                return Boolean.compare(hasPermission2, hasPermission1); // Permission priority
            }

            // 1. Compare by order first
            int orderComparison = Integer.compare(tag1.getOrder(), tag2.getOrder());
            if (orderComparison != 0) {
                return orderComparison;
            }

            // 2. Optionally prioritize selected tag
            if (prioritiseSelectedTag) {
                boolean isActiveUserTag1 = Objects.equals(
                        UserData.getActive(menuUtil.getOwner().getUniqueId()), tag1.getIdentifier()
                );
                boolean isActiveUserTag2 = Objects.equals(
                        UserData.getActive(menuUtil.getOwner().getUniqueId()), tag2.getIdentifier()
                );

                if (isActiveUserTag1 != isActiveUserTag2) {
                    return Boolean.compare(isActiveUserTag2, isActiveUserTag1); // Active tag first
                }
            }

            // 4. Finally, alphabetical by identifier
            return tag1.getIdentifier().compareTo(tag2.getIdentifier());
        };
    }


    public ItemStack createCustomItem(String materialKey, String displayName, int customModelData, List<String> lore) {
        ItemStack item;
        ItemMeta itemMeta;

        if (materialKey.contains("hdb-")) {
            int id = Integer.parseInt(materialKey.replace("hdb-", ""));
            HeadDatabaseAPI api = new HeadDatabaseAPI();
            item = api.getItemHead(String.valueOf(id));
        } else if (materialKey.contains("basehead-")) {
            String id = materialKey.replace("basehead-", "");
            item = SkullUtil.getSkullByBase64EncodedTextureUrl(SupremeTags.getInstance(), id);
        } else if (materialKey.contains("itemsadder-")) {
            String id = materialKey.replace("itemsadder-", "");
            item = getItemWithIA(id);
        } else if (materialKey.contains("nexo-")) {
            String id = materialKey.replace("nexo-", "");
            item = SupremeTags.getInstance().getItemWithNexo(id);
        } else {
            item = new ItemStack(Material.valueOf(materialKey.toUpperCase()), 1);
        }

        itemMeta = item.getItemMeta();
        if (itemMeta != null) {
            if (customModelData > 0) {
                itemMeta.setCustomModelData(customModelData);
            }

            displayName = globalPlaceholders(menuUtil.getOwner(), displayName);
            displayName = displayName.replace("%identifier%", menuUtil.getIdentifier());

            itemMeta.setDisplayName(format(displayName));
            itemMeta.setLore(color(lore));
            itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
            if (!isVersionLessThan("1.16")) {
                itemMeta.addItemFlags(ItemFlag.HIDE_DYE);
            }

            item.setItemMeta(itemMeta);
        }

        return item;
    }

    protected void sendLockedMessage(Player player) {
        String locked = messages.getString("messages.locked-tag")
                .replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
        locked = replacePlaceholders(menuUtil.getOwner(), locked);
        msgPlayer(player, locked);
    }

    protected void handleTagAssign(Player player, String identifier, Tag t) {
        TagAssignEvent tagevent = new TagAssignEvent(player, identifier, false);
        Bukkit.getPluginManager().callEvent(tagevent);
        if (tagevent.isCancelled()) return;

        String activeTag = UserData.getActive(player.getUniqueId());
        if (!activeTag.equalsIgnoreCase("none")) {
            Tag oldTag = SupremeTags.getInstance().getTagManager().getTag(activeTag);
            if (oldTag != null) oldTag.removeEffects(menuUtil.getOwner());
        }

        UserData.setActive(player, tagevent.getTag());
        super.open();
        menuUtil.setIdentifier(tagevent.getTag());
        t.applyEffects(menuUtil.getOwner());

        if (SupremeTags.getInstance().getConfig().getBoolean("settings.gui-messages")) {
            String select = messages.getString("messages.tag-select-message")
                    .replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
            select = replacePlaceholders(menuUtil.getOwner(), select);
            msgPlayer(player, select
                    .replace("%identifier%", identifier)
                    .replaceAll("%tag%", t.getCurrentTag()));
        }
    }

    protected void handleTagReset(Player player, Tag t) {
        TagResetEvent tagEvent = new TagResetEvent(player, false);
        Bukkit.getPluginManager().callEvent(tagEvent);
        if (tagEvent.isCancelled()) return;

        String defaultTag = SupremeTags.getInstance().getConfig().getString("settings.default-tag");
        UserData.setActive(player, defaultTag);
        super.open();
        menuUtil.setIdentifier(defaultTag);
        t.removeEffects(menuUtil.getOwner());

        if (SupremeTags.getInstance().getConfig().getBoolean("settings.gui-messages")) {
            msgPlayer(player, messages.getString("messages.reset-message")
                    .replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix"))));
        }
    }

    protected List<String> getFormattedLore(Tag t, String permission) {
        List<String> lore;
        boolean isCostEnabled = t.getEconomy().isEnabled();
        boolean hasPermission = menuUtil.getOwner().hasPermission(permission) || permission.equalsIgnoreCase("none");
        boolean isSelected = UserData.getActive(menuUtil.getOwner().getUniqueId()).equalsIgnoreCase(t.getIdentifier());

        String lorePath;

        if (isCostEnabled) {
            lorePath = hasPermission ? (isSelected ? "selected-lore" : "unlocked-lore") : "locked-lore";
        } else {
            lorePath = hasPermission ? (isSelected ? "selected-lore" : "unlocked-lore") : "locked-permission";
        }

        lore = guis.getStringList("gui.tag-menu.global-tag-lores." + lorePath);

        return lore;
    }

}