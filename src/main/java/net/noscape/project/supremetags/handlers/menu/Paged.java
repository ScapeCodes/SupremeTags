package net.noscape.project.supremetags.handlers.menu;

import net.noscape.project.supremetags.utils.ItemData;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XItemFlag;
import com.cryptomorin.xseries.XMaterial;
import de.rapha149.signgui.SignGUI;
import de.rapha149.signgui.exception.SignGUIVersionException;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.api.events.TagAssignEvent;
import net.noscape.project.supremetags.api.events.TagResetEvent;
import net.noscape.project.supremetags.guis.search.SearchResultMenu;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.handlers.TagFormatter;
import net.noscape.project.supremetags.handlers.Variant;
import net.noscape.project.supremetags.handlers.requirements.RequirementEvaluator;
import net.noscape.project.supremetags.managers.TagManager;
import net.noscape.project.supremetags.storage.UserData;
import net.noscape.project.supremetags.utils.ItemResolver;
import net.noscape.project.supremetags.utils.SkullUtil;
import net.noscape.project.supremetags.utils.Utils;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.noscape.project.supremetags.utils.Utils.*;
import static net.noscape.project.supremetags.utils.Utils.globalPlaceholders;

public abstract class Paged extends Menu {

    private static final Pattern CUSTOM_PLACEHOLDER_PATTERN = Pattern.compile("%custom-placeholder_(.*?)%");

    private final FileConfiguration guis = SupremeTags.getInstance().getConfigManager().getConfig("guis.yml").get();
    private final FileConfiguration messages = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get();
    private final FileConfiguration Cat_Config = SupremeTags.getInstance().getConfigManager().getConfig("categories.yml").get();
    private final FileConfiguration rarityConfig = SupremeTags.getInstance().getConfigManager().getConfig("rarities.yml").get();

    protected int page = 0;
    protected int maxItems = guis.getInt("gui.tag-menu.tags-per-page");
    protected int index = 0;
    private final int tagsCount;
    protected int currentItemsOnPage = 0;
    protected boolean isLast;
    protected int totalItems;

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

        if (nextMaterial != null) {
            inventory.setItem(next_slot, createCustomItem(nextMaterial, next, nextCmd, next_lore));
        }

        String layout = SupremeTags.getInstance().getLayout();
        if (layout == null) {
            return;
        }

        if (layout.equalsIgnoreCase("FULL")) {
            if (SupremeTags.getInstance().getConfig().getBoolean("gui.items.glass.enable")) {
                for (int i = 36; i <= 44; i++) {
                    String item_material = guis.getString("gui.items.glass.material");
                    String item_displayname = guis.getString("gui.items.glass.displayname");
                    int item_custom_model_data = guis.getInt("gui.items.glass.custom-model-data");

                    boolean hideToolTip = guis.getBoolean("gui.items.glass.hide-tooltip");

                    assert item_material != null;
                    inventory.setItem(i, makeItem(XMaterial.matchXMaterial(item_material.toUpperCase()).get().get(), item_displayname, item_custom_model_data, hideToolTip));
                }
            }
        } else if (layout.equalsIgnoreCase("BORDER")) {
            for (int i = 0; i < 54; i++) {
                if (inventory.getItem(i) == null) {
                    if (i < 9 || i >= 45 || i % 9 == 0 || (i + 1) % 9 == 0) {
                        String item_material = guis.getString("gui.items.glass.material");
                        String item_displayname = guis.getString("gui.items.glass.displayname");
                        int item_custom_model_data = guis.getInt("gui.items.glass.custom-model-data");

                        boolean hideToolTip = guis.getBoolean("gui.items.glass.hide-tooltip");

                        assert item_material != null;
                        inventory.setItem(i, makeItem(XMaterial.matchXMaterial(item_material.toUpperCase()).get().get(), item_displayname, item_custom_model_data, hideToolTip));
                    }
                }
            }
        }
    }

    public void applyLayout(boolean ptags, boolean categories, boolean variantsMenu, boolean showcaseMenu) {
        String layout = SupremeTags.getInstance().getLayout();
        if (layout == null) {
            return;
        }

        if (layout.equalsIgnoreCase("FULL")) {
            if (SupremeTags.getInstance().getConfig().getBoolean("gui.items.glass.enable")) {
                for (int i = 36; i <= 44; i++) {
                    String item_material = guis.getString("gui.items.glass.material");
                    String item_displayname = guis.getString("gui.items.glass.displayname");
                    int item_custom_model_data = guis.getInt("gui.items.glass.custom-model-data");

                    boolean hideToolTip = guis.getBoolean("gui.items.glass.hide-tooltip");

                    assert item_material != null;
                    inventory.setItem(i, makeItem(XMaterial.matchXMaterial(item_material.toUpperCase()).get().get(), item_displayname, item_custom_model_data, hideToolTip));
                }
            }
        } else if (layout.equalsIgnoreCase("BORDER")) {
            for (int i = 0; i < 54; i++) {
                if (inventory.getItem(i) == null) {
                    if (i < 9 || i >= 45 || i % 9 == 0 || (i + 1) % 9 == 0) {
                        String item_material = guis.getString("gui.items.glass.material");
                        String item_displayname = guis.getString("gui.items.glass.displayname");
                        int item_custom_model_data = guis.getInt("gui.items.glass.custom-model-data");

                        boolean hideToolTip = guis.getBoolean("gui.items.glass.hide-tooltip");

                        assert item_material != null;
                        inventory.setItem(i, makeItem(XMaterial.matchXMaterial(item_material.toUpperCase()).get().get(), item_displayname, item_custom_model_data, hideToolTip));
                    }
                }
            }
        }

        ConfigurationSection guiItemsSection = guis.getConfigurationSection("gui.items");
        if (guiItemsSection == null) {
            return;
        }

        for (String str : guiItemsSection.getKeys(false)) {
            boolean enabled = guis.getBoolean("gui.items." + str + ".enable");
            if (enabled && !str.equalsIgnoreCase("glass")) {

                if (showcaseMenu && str.equalsIgnoreCase("create-tag")) continue;
                if (showcaseMenu && str.equalsIgnoreCase("personal-tags")) continue;
                if (showcaseMenu && str.equalsIgnoreCase("active")) continue;
                if (showcaseMenu && str.equalsIgnoreCase("reset")) continue;
                if (showcaseMenu && str.equalsIgnoreCase("favourites")) continue;
                if (showcaseMenu && str.equalsIgnoreCase("search")) continue;
                if (showcaseMenu && str.equalsIgnoreCase("sort")) continue;
                if (showcaseMenu && str.equalsIgnoreCase("filter")) continue;

                if (!ptags && str.equalsIgnoreCase("create-tag")) continue;
                if (!ptags && str.equalsIgnoreCase("tag-credits")) continue;
                if (!ptags && str.equalsIgnoreCase("tag-credits-help")) continue;
                if (variantsMenu && str.equalsIgnoreCase("personal-tags")) continue;
                if (ptags && str.equalsIgnoreCase("personal-tags")) continue;
                if (variantsMenu && str.equalsIgnoreCase("create-tag")) continue;
                if (variantsMenu && str.equalsIgnoreCase("tag-credits")) continue;
                if (variantsMenu && str.equalsIgnoreCase("tag-credits-help")) continue;

                if (!SupremeTags.getInstance().getConfig().getBoolean("settings.personal-tags.enable") && str.equalsIgnoreCase("personal-tags"))
                    continue;

                if (!(tagsCount > maxItems & currentItemsOnPage >= maxItems)) {
                    if (str.equalsIgnoreCase("next"))
                        continue;
                }

                if (!ptags && !categories && !variantsMenu) {
                    if (!(page > 0) && !shouldShowBackOnFirstPage()) {
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

                int item_slot = guis.getInt("gui.items." + str + ".slot");
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

                ItemResolver.ResolvedItem resolved = ItemResolver.resolveCustomItem(menuUtil.getOwner(), item_material);
                ItemStack item = resolved.item();
                ItemMeta itemMeta = resolved.meta();
                ItemStack nbt = item;

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

                ItemData.setString(nbt, "name", str);

                item_displayname = item_displayname.replace("%player%", menuUtil.getOwner().getName());
                if (item_displayname.contains("%tag_credits%")) {
                    item_displayname = item_displayname.replace("%tag_credits%", String.valueOf(UserData.getDisplayTagCredits(menuUtil.getOwner().getUniqueId())));
                }
                if (item_displayname.contains("%tag_credits_creation_cost%")) {
                    item_displayname = item_displayname.replace("%tag_credits_creation_cost%", String.valueOf(SupremeTags.getInstance().getConfig().getLong("settings.personal-tags.credits.creation-cost", 0L)));
                }

                String filterDisplay = getCurrentFilterDisplay();
                String sortDisplay = getCurrentSortDisplay();

                item_displayname = item_displayname.replace("%filter%", filterDisplay);
                item_displayname = item_displayname.replace("%sort%", sortDisplay);

                String identifier = UserData.getActive(menuUtil.getOwner().getUniqueId());

                if (identifier.equalsIgnoreCase("None")) {
                    String customTag = UserData.getCustomTag(menuUtil.getOwner().getUniqueId());

                    if (customTag != null && !customTag.isBlank()) {
                        item_displayname = item_displayname.replace("%identifier%", "Custom");
                        item_displayname = item_displayname.replace("%tag%", customTag);
                    } else {
                        String none = SupremeTags.getInstance().getConfig().getString("placeholders.tag.none-output", "");
                        item_displayname = item_displayname.replace("%identifier%", none);
                        item_displayname = item_displayname.replace("%tag%", none);
                    }
                } else {
                    item_displayname = item_displayname.replace("%identifier%", identifier);
                    item_displayname = item_displayname.replace(
                            "%tag%",
                            TagFormatter.getFormattedTag(menuUtil.getOwner(), TagFormatter.Context.TAG)
                    );
                }

                item_displayname = globalPlaceholders(menuUtil.getOwner(), item_displayname);

                if (item_lore != null || !item_lore.isEmpty()) {
                    if (ptags && str.equalsIgnoreCase("tag-credits-help")) {
                        item_lore = new ArrayList<>(SupremeTags.getInstance().getConfig().getStringList("settings.personal-tags.credits.how-to-earn"));
                    }

                    String identifier_lore = UserData.getActive(menuUtil.getOwner().getUniqueId());

                    if (identifier_lore.equalsIgnoreCase("none")) {
                        identifier_lore = SupremeTags.getInstance().getConfig().getString("placeholders.tag.none-output");
                    }

                    String finalIdentifier_lore = identifier_lore;
                    item_lore.replaceAll(s -> s.replace("%identifier%", finalIdentifier_lore));
                    item_lore.replaceAll(s -> s.replace("%filter%", filterDisplay));
                    item_lore.replaceAll(s -> s.replace("%sort%", sortDisplay));

                    String displayTag = TagFormatter.getFormattedTag(menuUtil.getOwner(), TagFormatter.Context.TAG);
                    item_lore.replaceAll(s -> s.replace("%tag%", displayTag));
                    if (item_lore.stream().anyMatch(s -> s.contains("%tag_credits%"))) {
                        String tagCredits = String.valueOf(UserData.getDisplayTagCredits(menuUtil.getOwner().getUniqueId()));
                        item_lore.replaceAll(s -> s.replace("%tag_credits%", tagCredits));
                    }
                    if (item_lore.stream().anyMatch(s -> s.contains("%tag_credits_creation_cost%"))) {
                        String creationCost = String.valueOf(SupremeTags.getInstance().getConfig().getLong("settings.personal-tags.credits.creation-cost", 0L));
                        item_lore.replaceAll(s -> s.replace("%tag_credits_creation_cost%", creationCost));
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
                        addConfiguredLoreLine(newLore, unselectedKey + "all-tags", amountAll);
                        addConfiguredLoreLine(newLore, selectedKey + "your-tags", amountYourTags);
                    } else if (filter.equalsIgnoreCase("all")) {
                        addConfiguredLoreLine(newLore, selectedKey + "all-tags", amountAll);
                        addConfiguredLoreLine(newLore, unselectedKey + "your-tags", amountYourTags);
                    } else {
                        addConfiguredLoreLine(newLore, unselectedKey + "all-tags", amountAll);
                        addConfiguredLoreLine(newLore, unselectedKey + "your-tags", amountYourTags);
                    }

                    ConfigurationSection cat_sec = this.Cat_Config.getConfigurationSection("categories");

                    if (!categories) {
                        for (String category : cats) {
                            boolean isSelected = filter.equalsIgnoreCase("category:" + category);
                            int amountCategory = getTypeAmount(player, "category:" + category);

                            String label = category.toUpperCase();
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
                            String formatTemplate = guis.getString(formatKey);
                            if (formatTemplate == null || formatTemplate.isBlank()) {
                                continue;
                            }
                            String formatted = formatTemplate.replace("%category%", label) + " (" + amountCategory + ")";
                            newLore.add(format(formatted));
                        }
                    }

                    List<String> finalLore = new ArrayList<>();
                    for (String line : item_lore) {
                        if (line.contains("%filter_lore%")) {
                            finalLore.addAll(color(newLore));
                        } else {
                            finalLore.add(format(line
                                    .replace("%filter%", filterDisplay)
                                    .replace("%sort%", sortDisplay)));
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

                    String noneLabel;
                    if (sort.equalsIgnoreCase("none")) {
                        noneLabel = guis.getString("gui.items.sort.sorts.selected.no-filter");
                    } else {
                        noneLabel = guis.getString("gui.items.sort.sorts.unselected.no-filter");
                    }
                    int amountAll = 0;

                    if (!variantsMenu) {
                        amountAll = getTypeAmount(player, "all");
                    } else {
                        amountAll = getVariantTypeAmount(player, "all");
                    }

                    if (noneLabel != null && !noneLabel.isBlank()) {
                        newLore.add(format(noneLabel + " (" + amountAll + ")"));
                    }

                    addConfiguredLoreLine(newLore, "gui.items.sort.sorts." + (sort.equalsIgnoreCase("popularity") ? "selected" : "unselected") + ".popularity", amountAll);
                    addConfiguredLoreLine(newLore, "gui.items.sort.sorts." + (sort.equalsIgnoreCase("recently-used") ? "selected" : "unselected") + ".recently-used", amountAll);

                    for (String rarity : rarities) {
                        boolean isSelected = sort.equalsIgnoreCase("rarity:" + rarity);

                        int amountByRarity = 0;

                        if (!variantsMenu) {
                            amountByRarity = getTypeAmount(player, "rarity:" + rarity);
                        } else {
                            amountByRarity = getVariantTypeAmount(player, "rarity:" + rarity);
                        }

                        String label = deformat(rarity);

                        ConfigurationSection raritySection = rarityConfig.getConfigurationSection("rarities." + rarity);
                        if (raritySection != null && raritySection.isConfigurationSection("filter-labels")) {
                            ConfigurationSection labels = raritySection.getConfigurationSection("filter-labels");
                            if (labels != null) {
                                label = labels.getString(isSelected ? "selected" : "unselected", label);
                            }
                        }

                        String template = guis.getString(isSelected ? selectedKey : unselectedKey);
                        if (template == null || template.isBlank()) {
                            continue;
                        }
                        String formatted = template.replace("%rarity%", label) + " (" + amountByRarity + ")";

                        newLore.add(format(formatted));
                    }

                    List<String> finalLore = new ArrayList<>();
                    for (String line : item_lore) {
                        if (line.contains("%sort_lore%")) {
                            finalLore.addAll(color(newLore));
                        } else {
                            finalLore.add(format(line
                                    .replace("%filter%", filterDisplay)
                                    .replace("%sort%", sortDisplay)));
                        }
                    }
                    item_lore = finalLore;
                }

                itemMeta.setLore(color(item_lore));

                itemMeta.setDisplayName(format(item_displayname));
                addItemFlags(itemMeta, XItemFlag.HIDE_ATTRIBUTES, XItemFlag.HIDE_DYE, XItemFlag.HIDE_DESTROYS, XItemFlag.HIDE_ENCHANTS, XItemFlag.HIDE_UNBREAKABLE);

                nbt.setItemMeta(itemMeta);
                ItemData.setString(nbt, "name", str);

                if (!isSlots) {
                    inventory.setItem(item_slot, nbt);
                } else {
                    for (int slot : slots) {
                        inventory.setItem(slot, nbt);
                    }
                }
            }

            ConfigurationSection customItemsSection = guis.getConfigurationSection("gui.tag-menu.custom-items");
            if (customItemsSection == null) {
                return;
            }

            for (String cSTR : customItemsSection.getKeys(false)) {
                boolean cEnable = guis.getBoolean("gui.tag-menu.custom-items." + cSTR + ".enable");

                if (showcaseMenu) continue;

                if (cEnable) {
                    String item_material = guis.getString("gui.tag-menu.custom-items." + cSTR + ".material");
                    String item_displayname = guis.getString("gui.tag-menu.custom-items." + cSTR + ".displayname");
                    int item_custom_model_data = guis.getInt("gui.tag-menu.custom-items." + cSTR + ".custom-model-data");

                    boolean hideToolTip = guis.getBoolean("gui.tag-menu.custom-items." + cSTR + ".hide-tooltip");

                    int item_slot = 0;
                    boolean isSlots = false;
                    List<Integer> slots = new ArrayList<>();

                    if (guis.contains("gui.tag-menu.custom-items." + cSTR + ".slots")) {
                        slots = guis.getIntegerList("gui.tag-menu.custom-items." + cSTR + ".slots");
                        isSlots = true;
                    }

                    if (!isSlots && guis.contains("gui.tag-menu.custom-items." + cSTR + ".slot")) {
                        item_slot = guis.getInt("gui.tag-menu.custom-items." + cSTR + ".slot");
                    }

                    List<String> item_lore = guis.getStringList("gui.tag-menu.custom-items." + cSTR + ".lore");

                    ItemResolver.ResolvedItem resolved = ItemResolver.resolveCustomItem(menuUtil.getOwner(), item_material);
                    ItemStack item = resolved.item();
                    ItemMeta itemMeta = resolved.meta();

                    if (item_custom_model_data > 0 && itemMeta != null) {
                        itemMeta.setCustomModelData(item_custom_model_data);
                    }

                    if (isPaperVersionAtLeast(1, 21, 5) && hideToolTip) {
                        itemMeta.setHideTooltip(true);
                    }

                    item_displayname = item_displayname.replace("%player%", menuUtil.getOwner().getName());
                    String identifier = UserData.getActive(menuUtil.getOwner().getUniqueId());

                    if (!identifier.equalsIgnoreCase("None")) {
                        item_displayname = item_displayname.replace("%identifier%", identifier);
                    } else {
                        item_displayname = item_displayname.replace("%identifier%",
                                SupremeTags.getInstance().getConfig().getString("placeholders.tag.none-output"));
                    }

                    if (SupremeTags.getInstance().getTagManager().getTag(identifier) != null) {
                        if (SupremeTags.getInstance().getTagManager().getTag(identifier).getCurrentTag() != null) {
                            item_displayname = item_displayname.replace("%tag%",
                                    SupremeTags.getInstance().getTagManager().getTag(identifier).getCurrentTag());
                        } else {
                            item_displayname = item_displayname.replace("%tag%",
                                    SupremeTags.getInstance().getTagManager().getTag(identifier).getTag().get(0));
                        }
                    } else {
                        item_displayname = item_displayname.replace("%tag%",
                                SupremeTags.getInstance().getConfig().getString("placeholders.tag.none-output"));
                    }

                    item_displayname = globalPlaceholders(menuUtil.getOwner(), item_displayname);

                    if (item_lore != null && !item_lore.isEmpty()) {
                        item_lore.replaceAll(s -> s.replace("%identifier%", identifier));
                        if (SupremeTags.getInstance().getTagManager().getTag(identifier) != null) {
                            if (SupremeTags.getInstance().getTagManager().getTag(identifier).getCurrentTag() != null) {
                                item_lore.replaceAll(s -> s.replace("%tag%",
                                        SupremeTags.getInstance().getTagManager().getTag(identifier).getCurrentTag()));
                            } else {
                                item_lore.replaceAll(s -> s.replace("%tag%",
                                        SupremeTags.getInstance().getTagManager().getTag(identifier).getTag().get(0)));
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

                    addItemFlags(itemMeta, XItemFlag.HIDE_ATTRIBUTES, XItemFlag.HIDE_DYE, XItemFlag.HIDE_DESTROYS, XItemFlag.HIDE_ENCHANTS, XItemFlag.HIDE_UNBREAKABLE);

                    item.setItemMeta(itemMeta);

            ItemStack nbt = item;
                    ItemData.setString(nbt, "custom-item", cSTR);
                    item = nbt;

                    if (!isSlots) {
                        inventory.setItem(item_slot, item);
                    } else {
                        for (int slot : slots) {
                            inventory.setItem(slot, item.clone());
                        }
                    }
                }
            }
        }
    }

    protected int getPage() {
        return page + 1;
    }

    protected String applyPagePlaceholders(String title, int totalItems) {
        return title
                .replace("%page%", String.valueOf(this.getPage()))
                .replace("%max_pages%", String.valueOf(this.getMaxPages(totalItems)));
    }

    protected int getMaxPages(int totalItems) {
        if (maxItems <= 0) {
            return 1;
        }

        return Math.max(1, (int) Math.ceil((double) totalItems / maxItems));
    }

    public int getMaxItems() {
        return maxItems;
    }

    public void getVariantsCountOnPage(String identifier) {
        ArrayList<Variant> tag = new ArrayList<>(SupremeTags.getInstance().getTagManager().getTag(identifier).getVariants());

        if (!tag.isEmpty()) {

            int startIndex = page * maxItems;
            int endIndex = Math.min(startIndex + maxItems, tag.size());

            currentItemsOnPage = 0;

            for (int i = startIndex; i < endIndex; i++) {
                currentItemsOnPage++;
            }
        }
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

    public int getCurrentItemsOnPage() {
        return currentItemsOnPage;
    }

    public void openSearchContainer(Player player) {
        String search = SupremeTags.getInstance().getConfig().getString("settings.search-type");

        if (search.equalsIgnoreCase("SIGN")) {
            openSignSearch(player);
        } else if (search.equalsIgnoreCase("ANVIL")) {
            openAnvilSearch(player);
        } else if (search.equalsIgnoreCase("DIALOG")) {
            openDialogSearch(player);
        } else {
            msgPlayer(player, messages.getString("messages.invalid-search-type", "%prefix% &cInvalid Search type, use SIGN, ANVIL or DIALOG.")
                    .replace("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix", ""))));
        }
    }

    private void openSignSearch(Player player) {
        SignGUI gui;
        try {
            gui = SignGUI.builder()
                    .setLines(format(messages.getString("messages.sign-line-top")), null, null)
                    .callHandlerSynchronously(SupremeTags.getInstance())
                    .setColor(DyeColor.YELLOW)

                    .setHandler((p, result) -> {
                        String text = result.getLineWithoutColor(1);

                        if (text != null && !text.isEmpty()) {
                            if (SupremeTags.getInstance().getCategoryManager().isCategoryNearName(text)
                                    || SupremeTags.getInstance().getTagManager().tagExistsNearName(text)) {

                                Utils.runMain(() -> new SearchResultMenu(SupremeTags.getMenuUtil(player), text).open());

                            } else {
                                String searchInvalid = messages.getString("messages.search-invalid-1")
                                        .replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
                                msgPlayer(player, searchInvalid);
                            }
                        } else {
                            String searchInvalid = messages.getString("messages.search-invalid-2")
                                    .replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
                            msgPlayer(player, searchInvalid);
                        }

                        return Collections.emptyList();
                    })

                    .build();
        } catch (SignGUIVersionException e) {
            throw new RuntimeException(e);
        }

        gui.open(player);
    }

    private void openAnvilSearch(Player player) {
        new AnvilGUI.Builder()
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }

                    String text = stateSnapshot.getText();

                    if (!text.isEmpty()) {
                        if (SupremeTags.getInstance().getCategoryManager().isCategoryNearName(text)
                                || SupremeTags.getInstance().getTagManager().tagExistsNearName(text)) {

                            Utils.runMain(() -> new SearchResultMenu(SupremeTags.getMenuUtil(player), text).open());

                        } else {
                            String searchInvalid = messages.getString("messages.search-invalid-1")
                                    .replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
                            msgPlayer(player, searchInvalid);
                        }
                    } else {
                        String searchInvalid = messages.getString("messages.search-invalid-2")
                                .replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
                        msgPlayer(player, searchInvalid);
                    }

                    return List.of(AnvilGUI.ResponseAction.close());
                })
                .itemLeft(createSearchItem())
                .text("")
                .title(format(messages.getString("messages.sign-line-top")))
                .plugin(SupremeTags.getInstance())
                .open(player);
    }

    private void openDialogSearch(Player player) {
        if (Utils.isVersionLessThan("1.21.8")) {
            openSignSearch(player);
            return;
        }

        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text(format(messages.getString("messages.sign-line-top"))))
                        .inputs(List.of(
                                DialogInput.text(
                                        "search",
                                        Component.text(format(messages.getString("messages.sign-line-top")))
                                ).width(300).build()
                        ))
                        .build())
                .type(DialogType.multiAction(
                        List.of(
                                ActionButton.create(
                                        Component.text("Search"),
                                        Component.empty(),
                                        150,
                                        DialogAction.customClick(Key.key("supremetags:search"), null)
                                ),
                                ActionButton.create(
                                        Component.text("Cancel"),
                                        Component.empty(),
                                        150,
                                        null
                                )
                        ),
                        null,
                        2
                ))
        );

        player.showDialog(dialog);
    }

    private ItemStack createSearchItem() {
        ItemStack item = XMaterial.NAME_TAG.parseItem();
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(format("&eSearch Tags"));

            meta.setLore(Arrays.asList(
                    format("&7Type the tag name"),
                    format("&7into the anvil text box."),
                    "",
                    format("&aClick the output to search!")
            ));

            meta.addEnchant(XEnchantment.MENDING.get(), 1, true);
            addItemFlags(meta, XItemFlag.HIDE_ENCHANTS);

            item.setItemMeta(meta);
        }

        return item;
    }

    protected boolean shouldShowBackOnFirstPage() {
        return false;
    }

    private void addConfiguredLoreLine(List<String> lore, String path, int amount) {
        String template = guis.getString(path);
        if (template == null || template.isBlank()) {
            return;
        }

        lore.add(format(template + " (" + amount + ")"));
    }

    private String getCurrentFilterDisplay() {
        String filter = menuUtil.getFilter();
        if (filter == null || filter.equalsIgnoreCase("all")) {
            return guis.getString("gui.items.filter.filters.replacements.all-tags", "All Tags");
        }

        if (filter.startsWith("category:")) {
            return filter.replace("category:", "");
        }

        if (filter.equalsIgnoreCase("players")) {
            return guis.getString("gui.items.filter.filters.replacements.your-tags", "Your Tags");
        }

        return guis.getString("gui.items.filter.filters.replacements.all-tags", "All Tags");
    }

    private String getCurrentSortDisplay() {
        String sort = menuUtil.getSort();
        if (sort == null || sort.equalsIgnoreCase("none")) {
            return guis.getString("gui.items.sort.sorts.replacements.no-filter", "&7No Filter");
        }

        if (sort.startsWith("rarity:")) {
            return sort.replace("rarity:", "").toUpperCase(Locale.ROOT);
        }

        if (sort.equalsIgnoreCase("popularity")) {
            return guis.getString("gui.items.sort.sorts.replacements.popularity", "&7Most Popular");
        }

        if (sort.equalsIgnoreCase("recently-used")) {
            return guis.getString("gui.items.sort.sorts.replacements.recently-used", "&7Recently Used");
        }

        return guis.getString("gui.items.sort.sorts.replacements.no-filter", "&7No Filter");
    }

    protected Comparator<Tag> getTagComparator(boolean prioritiseSelectedTag, MenuUtil menuUtil) {
        return (tag1, tag2) -> {

            String sort = menuUtil.getSort() == null ? "none" : menuUtil.getSort().toLowerCase(Locale.ROOT);
            if (sort.equalsIgnoreCase("popularity")) {
                int popularity = Integer.compare(
                        SupremeTags.getInstance().getTagStatisticsManager().getTagSelections(tag2.getIdentifier()),
                        SupremeTags.getInstance().getTagStatisticsManager().getTagSelections(tag1.getIdentifier())
                );
                if (popularity != 0) return popularity;
            } else if (sort.equalsIgnoreCase("recently-used")) {
                int recent = Long.compare(
                        SupremeTags.getInstance().getTagStatisticsManager().getPlayerTagLastSelected(menuUtil.getOwner().getUniqueId(), tag2.getIdentifier()),
                        SupremeTags.getInstance().getTagStatisticsManager().getPlayerTagLastSelected(menuUtil.getOwner().getUniqueId(), tag1.getIdentifier())
                );
                if (recent != 0) return recent;
            }

            boolean hasPermission1 = menuUtil.getOwner().hasPermission(tag1.getPermission());
            boolean hasPermission2 = menuUtil.getOwner().hasPermission(tag2.getPermission());

            if (hasPermission1 != hasPermission2) {
                return Boolean.compare(hasPermission2, hasPermission1);
            }

            int orderComparison = Integer.compare(tag1.getOrder(), tag2.getOrder());
            if (orderComparison != 0) {
                return orderComparison;
            }

            if (prioritiseSelectedTag) {
                boolean isActiveUserTag1 = Objects.equals(
                        UserData.getActive(menuUtil.getOwner().getUniqueId()), tag1.getIdentifier()
                );
                boolean isActiveUserTag2 = Objects.equals(
                        UserData.getActive(menuUtil.getOwner().getUniqueId()), tag2.getIdentifier()
                );

                if (isActiveUserTag1 != isActiveUserTag2) {
                    return Boolean.compare(isActiveUserTag2, isActiveUserTag1);
                }
            }

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
            item = new ItemStack(XMaterial.matchXMaterial(materialKey.toUpperCase()).get().get(), 1);
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
            addItemFlags(itemMeta, XItemFlag.HIDE_ATTRIBUTES, XItemFlag.HIDE_ENCHANTS, XItemFlag.HIDE_UNBREAKABLE, XItemFlag.HIDE_DYE);

            item.setItemMeta(itemMeta);
        }

        return item;
    }

    protected ItemStack buildMenuTagItem(Tag t, String permission, String loreMenuType) {
        Player owner = menuUtil.getOwner();
        String identifier = t.getIdentifier();
        boolean hasAccess = Utils.hasTagAccess(owner, t);
        boolean isActive = UserData.getActive(owner.getUniqueId()).equalsIgnoreCase(identifier);
        FileConfiguration tagConfig = SupremeTags.getInstance().getTagManager().getConfigForTag(identifier);
        String tagPath = "tags." + identifier;
        String currentTag = t.getCurrentTag() != null ? t.getCurrentTag() : t.getTag().getFirst();

        String displayname;
        if (hasAccess) {
            String configuredDisplayName = tagConfig.getString(tagPath + ".displayname");
            displayname = configuredDisplayName != null ? configuredDisplayName.replace("%tag%", currentTag) : format("&7Tag: " + currentTag);
        } else {
            String lockedDisplayName = guis.getString("gui.tag-menu.global-locked-tag.displayname");
            String configuredDisplayName = tagConfig.getString(tagPath + ".displayname");
            displayname = (lockedDisplayName != null ? lockedDisplayName : Objects.requireNonNull(configuredDisplayName)).replace("%tag%", currentTag);
        }

        displayname = globalPlaceholders(owner, displayname);

        String material = hasAccess
                ? tagConfig.getString(tagPath + ".display-item", "NAME_TAG")
                : guis.getString("gui.tag-menu.global-locked-tag.display-item", "NAME_TAG");

        ItemResolver.ResolvedItem resolved = ItemResolver.resolveCustomItem(owner, material);
        ItemStack tagItem = resolved.item();
        ItemMeta tagMeta = resolved.meta();
        ItemStack nbt = tagItem;

        ItemData.setString(nbt, "identifier", identifier);

        int modelData = hasAccess ? tagConfig.getInt(tagPath + ".custom-model-data") : guis.getInt("gui.tag-menu.global-locked-tag.custom-model-data");
        if (modelData > 0 && tagMeta != null) {
            tagMeta.setCustomModelData(modelData);
        }

        assert tagMeta != null;

        if (isActive && SupremeTags.getInstance().getConfig().getBoolean("settings.active-tag-glow")) {
            tagMeta.addEnchant(Enchantment.KNOCKBACK, 1, true);
        }

        tagMeta.setDisplayName(format(displayname));
        addItemFlags(tagMeta, XItemFlag.HIDE_ATTRIBUTES, XItemFlag.HIDE_DYE, XItemFlag.HIDE_DESTROYS, XItemFlag.HIDE_ENCHANTS, XItemFlag.HIDE_UNBREAKABLE);

        List<String> lore = getFormattedLore(t, permission, loreMenuType);
        String joinedDescription = t.getDescription().stream().map(Utils::format).collect(Collectors.joining("\n"));
        String joinedEffects;
        String effectsList;

        if (!t.getEffects().isEmpty()) {
            String formatEffectTemplate = messages.getString("messages.effects-replace-style");

            joinedEffects = t.getEffects().keySet().stream()
                    .map(PotionEffectType::getName)
                    .map(Utils::format)
                    .map(effect -> formatEffectTemplate.replace("%effect%", effect))
                    .collect(Collectors.joining("\n"));

            effectsList = t.getEffects().keySet().stream()
                    .map(effect -> effect.getKey().getKey().toUpperCase(Locale.ROOT))
                    .collect(Collectors.joining(", "));
        } else {
            joinedEffects = format(messages.getString("messages.no-effects"));
            effectsList = joinedEffects;
        }

        for (int l = 0; l < lore.size(); l++) {
            String line = lore.get(l);

            Matcher matcher = CUSTOM_PLACEHOLDER_PATTERN.matcher(line);
            while (matcher.find()) {
                String dynamicPart = matcher.group(1);
                line = line.replace(matcher.group(0), t.getCustomPlaceholder(identifier, dynamicPart));
            }

            if (line.contains("%description%")) {
                if (line.trim().equals("%description%")) {
                    List<String> descriptionLines = Arrays.asList(joinedDescription.split("\n"));
                    lore.remove(l);
                    lore.addAll(l, descriptionLines);
                    l += descriptionLines.size() - 1;
                    continue;
                } else {
                    line = line.replace("%description%", joinedDescription.replace("\n", " "));
                }
            }

            if (line.contains("%effects%")) {
                if (line.trim().equals("%effects%")) {
                    List<String> effectLines = Arrays.asList(joinedEffects.split("\n"));
                    lore.remove(l);
                    lore.addAll(l, effectLines);
                    l += effectLines.size() - 1;
                    continue;
                } else {
                    line = line.replace("%effects%", joinedEffects.replace("\n", " "));
                }
            }

            String requirements = RequirementEvaluator.formatStatus(owner, t);
            if (line.contains("%requirements%")) {
                if (line.trim().equals("%requirements%")) {
                    List<String> requirementLines = requirements.isEmpty() ? new ArrayList<>() : Arrays.asList(requirements.split("\n"));
                    lore.remove(l);
                    lore.addAll(l, requirementLines);
                    l += requirementLines.size() - 1;
                    continue;
                } else {
                    line = line.replace("%requirements%", requirements.replace("\n", " "));
                }
            }

            line = line.replace("%identifier%", identifier);
            line = line.replace("%tag%", currentTag);
            line = line.replace("%cost_formatted%", "$" + formatNumber(t.getEconomy().getAmount()));
            line = line.replace("%cost_formatted_raw%", formatNumber(t.getEconomy().getAmount()));
            line = line.replace("%cost%", String.valueOf(t.getEconomy().getAmount()));
            line = line.replace("%variants%", String.valueOf(t.getVariants().size()));
            line = line.replace("%order%", String.valueOf(t.getOrder()));
            line = line.replace("%track_unlocked%", String.valueOf(TagManager.tagUnlockCounts.getOrDefault(identifier, 0)));
            line = line.replace("%category%", t.getCategory());
            line = line.replace("%rarity%", SupremeTags.getInstance().getRarityManager().getRarity(t.getRarity()).getDisplayname());
            line = line.replace("%effects_list%", effectsList);
            line = SupremeTags.getInstance().getTagStatisticsManager().replaceTagPlaceholders(owner, line, identifier);
            line = globalPlaceholders(owner, line);

            lore.set(l, line);
        }

        tagMeta.setLore(color(lore));
        nbt.setItemMeta(tagMeta);
        ItemData.setString(nbt, "identifier", identifier);

        return nbt;
    }

    protected void sendLockedMessage(Player player) {
        if (SupremeTags.getInstance().getConfig().getBoolean("settings.gui-messages")) {
            String locked = messages.getString("messages.locked-tag")
                    .replace("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
            locked = replacePlaceholders(menuUtil.getOwner(), locked);
            msgPlayer(player, locked);
        }
    }

    protected void sendLockedMessage(Player player, Tag tag) {
        if (!SupremeTags.getInstance().getConfig().getBoolean("settings.gui-messages")) {
            return;
        }

        String requirementMessage = Utils.getTagRequirementMessage(player, tag);
        if (requirementMessage != null && !requirementMessage.isBlank()) {
            msgPlayer(player, replacePlaceholders(player, requirementMessage));
            return;
        }

        sendLockedMessage(player);
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
            String prefix = messages.getString("messages.prefix");
            if (prefix == null) {
                prefix = "&e[SupremeTags]";
            }

            String select = messages.getString("messages.tag-select-message");
            if (select == null) {
                select = "&aYou have selected the tag %tag%";
            }

            select = select.replace("%prefix%", prefix);
            select = replacePlaceholders(menuUtil.getOwner(), select);
            msgPlayer(player, select
                    .replace("%identifier%", identifier)
                    .replace("%tag%", t.getTag().getFirst()));
            playConfigSound(player, "selected-tag");
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
                    .replace("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix"))));
        }

        playConfigSound(player, "reset-tag");
    }

    protected List<String> getFormattedLore(Tag t, String permission) {
        List<String> lore;
        boolean isCostEnabled = t.isEcoEnabled();
        boolean hasPermission = Utils.hasTagAccess(menuUtil.getOwner(), t);
        boolean isSelected = UserData.getActive(menuUtil.getOwner().getUniqueId()).equalsIgnoreCase(t.getIdentifier());

        String lorePath;

        if (isCostEnabled) {
            lorePath = hasPermission ? (isSelected ? "selected-lore" : "unlocked-lore") : "locked-lore";
        } else {
            lorePath = hasPermission ? (isSelected ? "selected-lore" : "unlocked-lore") : "locked-permission";
        }

        lore = guis.getStringList("gui.tag-menu.global-tag-lores." + lorePath);

        return color(lore);
    }

    protected List<String> getFormattedLore(Tag t, String permission, String menuType) {
        List<String> lore;
        boolean isCostEnabled = t.isEcoEnabled();
        boolean hasPermission = Utils.hasTagAccess(menuUtil.getOwner(), t);
        boolean isSelected = UserData.getActive(menuUtil.getOwner().getUniqueId()).equalsIgnoreCase(t.getIdentifier());

        String lorePath;

        if (isCostEnabled) {
            lorePath = hasPermission ? (isSelected ? "selected-lore" : "unlocked-lore") : "locked-lore";
        } else {
            lorePath = hasPermission ? (isSelected ? "selected-lore" : "unlocked-lore") : "locked-permission";
        }

        lore = guis.getStringList("gui." + menuType + ".global-tag-lores." + lorePath);

        return color(lore);
    }
}
