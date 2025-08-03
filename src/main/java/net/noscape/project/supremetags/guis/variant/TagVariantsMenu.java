package net.noscape.project.supremetags.guis.variant;

import de.tr7zw.nbtapi.NBTItem;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.api.events.TagAssignEvent;
import net.noscape.project.supremetags.api.events.TagResetEvent;
import net.noscape.project.supremetags.guis.MainMenu;
import net.noscape.project.supremetags.guis.TagMenu;
import net.noscape.project.supremetags.guis.personaltags.PersonalTagsMenu;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.handlers.Variant;
import net.noscape.project.supremetags.handlers.menu.MenuUtil;
import net.noscape.project.supremetags.handlers.menu.Paged;
import net.noscape.project.supremetags.managers.TagManager;
import net.noscape.project.supremetags.storage.UserData;
import net.noscape.project.supremetags.utils.ItemResolver;
import net.noscape.project.supremetags.utils.Utils;
import org.bukkit.Bukkit;
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
import java.util.stream.Collectors;

import static net.noscape.project.supremetags.utils.Utils.*;

public class TagVariantsMenu extends Paged {

    private final List<Variant> variants;
    private final Tag tag;
    private final FileConfiguration messages = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get();
    private final FileConfiguration guis = SupremeTags.getInstance().getConfigManager().getConfig("guis.yml").get();

    public TagVariantsMenu(MenuUtil menuUtil, Tag tag) {
        super(menuUtil);
        this.tag = tag;
        this.variants = tag.getVariants();
    }

    @Override
    public String getMenuName() {
        String title = guis.getString("gui.tag-variants-menu.title");

        if (tag != null) {
            title = title.replace("%tag%", tag.getTag().get(0));
            title = title.replace("%identifier%", tag.getIdentifier());
            title = globalPlaceholders(menuUtil.getOwner(), title);
        }

        return format(title);
    }

    @Override
    public int getSlots() {
        return guis.getInt("gui.tag-variants-menu.size");
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

        if (nbt.hasTag("isVariant") && nbt.hasTag("variant_identifier")) {
            if (nbt.getBoolean("isVariant").booleanValue()) {
                String var_identifier = nbt.getString("variant_identifier");
                Variant var = this.tag.getVariant(var_identifier);
                if (player.hasPermission(var.getPermission()) &&
                        !UserData.getActive(player.getUniqueId()).equalsIgnoreCase(var_identifier) && var_identifier != null) {

                    TagAssignEvent tagevent = new TagAssignEvent(player, var_identifier, false);
                    Bukkit.getPluginManager().callEvent(tagevent);
                    if (tagevent.isCancelled())
                        return;

                    UserData.setActive(player, tagevent.getTag());
                    open();

                    this.menuUtil.setIdentifier(tagevent.getTag());
                    if (SupremeTags.getInstance().getConfig().getBoolean("settings.gui-messages")) {
                        String select = this.messages.getString("messages.tag-select-message").replaceAll("%prefix%", Objects.requireNonNull(this.messages.getString("messages.prefix")));
                        select = replacePlaceholders(this.menuUtil.getOwner(), select);
                        msgPlayer(player, select.replace("%identifier%", var_identifier).replaceAll("%tag%", var.getTag().get(0)));
                    }
                } else if (player.hasPermission(var.getPermission()) &&
                        UserData.getActive(player.getUniqueId()).equalsIgnoreCase(var_identifier) && var_identifier != null) {
                    TagResetEvent tagEvent = new TagResetEvent(player, false);
                    Bukkit.getPluginManager().callEvent(tagEvent);

                    if (tagEvent.isCancelled()) return;

                    String defaultTag = SupremeTags.getInstance().getConfig().getString("settings.default-tag");

                    UserData.setActive(player, defaultTag);
                    super.open();
                    menuUtil.setIdentifier(defaultTag);

                    if (SupremeTags.getInstance().getConfig().getBoolean("settings.gui-messages")) {
                        msgPlayer(player, messages.getString("messages.reset-message").replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix"))));
                    }
                }
            }
        }

        if (nbt.hasTag("name")) {
            String name = nbt.getString("name");

            if (name.equalsIgnoreCase("close")) {
                player.closeInventory();
            }

            if (name.equalsIgnoreCase("personal-tags")) {
                new PersonalTagsMenu(SupremeTags.getMenuUtil(player)).open();
            }

            if (name.equalsIgnoreCase("search")) {
                player.closeInventory();
                openSearchContainer(player);
            }

            if (name.equalsIgnoreCase("reset")) {
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
                    menuUtil.setIdentifier("None");

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

            if (name.equalsIgnoreCase("sort")) {
                String current_sort = menuUtil.getSort();
                Set<String> rarities = SupremeTags.getInstance().getRarityManager().getRarityMap().keySet();

                List<String> sortOptions = new ArrayList<>();
                sortOptions.add("none"); // no sorting
                for (String rarity : rarities) {
                    sortOptions.add("rarity:" + rarity);
                }

                int currentIndex = sortOptions.indexOf(current_sort == null ? "none" : current_sort.toLowerCase());
                if (currentIndex == -1) currentIndex = 0;

                int nextIndex = (currentIndex + 1) % sortOptions.size();
                String nextSort = sortOptions.get(nextIndex);

                menuUtil.setSort(nextSort);
                super.open();
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

            if (name.equalsIgnoreCase("next")) {
                if (variants.size() > maxItems & currentItemsOnPage >= maxItems) {
                    if (!((index + 1) >= variants.size())) {
                        page = page + 1;
                        super.open();
                    } else {
                        e.setCancelled(true);
                    }
                } else {
                    e.setCancelled(true);
                }
            }
        }
    }

    @Override
    public void setMenuItems() {
        applyLayout(false, false, true);

        if (this.variants != null) {

            currentItemsOnPage = 0;

            String sort = menuUtil.getSort();

            for (Variant var : this.variants) {
                if (sort.startsWith("rarity:")) {
                    String rarity = sort.replace("rarity:", "");
                    if (!var.getRarity().equalsIgnoreCase(rarity)) continue;
                }

                String material = this.guis.getString("gui.tag-variants-menu.item.display-item");
                String item_displayname;
                int item_custom_model_data = this.guis.getInt("gui.tag-variants-menu.item.custom-model-data");

                if (menuUtil.getOwner().hasPermission(var.getPermission()) || var.getPermission().equalsIgnoreCase("none")) {
                    item_displayname = this.guis.getString("gui.tag-variants-menu.item.displayname");
                } else {
                    if (SupremeTags.getInstance().getTagManager().getTagConfig().getString("tags." + var.getSisterTag().getIdentifier() + ".locked-tag.displayname") == null) {
                        item_displayname = this.guis.getString("gui.tag-variants-menu.item.displayname").replace("%tag%", var.getTag().get(0));
                    } else {
                        item_displayname = Objects.requireNonNull(SupremeTags.getInstance().getTagManager().getTagConfig().getString("tags." + var.getSisterTag().getIdentifier() + ".locked-tag.displayname")).replace("%tag%", var.getTag().get(0));
                    }
                }

                ItemResolver.ResolvedItem resolved = ItemResolver.resolveCustomItem(material);
                ItemStack variantItem = resolved.item();
                ItemMeta variantMeta = resolved.meta();
                NBTItem nbt = new NBTItem(variantItem);

                if (menuUtil.getOwner().hasPermission(var.getPermission()) || var.getPermission().equalsIgnoreCase("none")) {
                    if (SupremeTags.getInstance().getTagManager().getTagConfig().getString("tags." + var.getSisterTag().getIdentifier() + ".display-item") != null) {
                        material = guis.getString("gui.tag-variants-menu.item.display-item");
                    } else {
                        material = "NAME_TAG";
                    }
                } else {
                    if (SupremeTags.getInstance().getTagManager().getTagConfig().getString("tags." + var.getSisterTag().getIdentifier() + ".locked-tag.display-item") != null) {
                        material = SupremeTags.getInstance().getTagManager().getTagConfig().getString("tags." + var.getSisterTag().getIdentifier() + ".locked-tag.display-item");
                    } else {
                        material = "NAME_TAG";
                    }
                }

                if (menuUtil.getOwner().hasPermission(var.getPermission()) || var.getPermission().equalsIgnoreCase("none")) {
                    if (variantMeta != null) {
                        if (item_custom_model_data > 0) {
                            variantMeta.setCustomModelData(item_custom_model_data);{
                        }
                    }
                } else {
                        if (SupremeTags.getInstance().getTagManager().getTagConfig().getInt("tags." + var.getSisterTag().getIdentifier() + ".locked-tag.custom-model-data") > 0) {
                            int modelData = SupremeTags.getInstance().getTagManager().getTagConfig().getInt("tags." + var.getSisterTag().getIdentifier() + ".locked-tag.custom-model-data");
                            if (variantMeta != null) {
                                variantMeta.setCustomModelData(modelData);
                            }
                        }
                    }
                }

                nbt.setString("variant_identifier", var.getIdentifier());
                nbt.setBoolean("isVariant", Boolean.valueOf(true));

                List<String> lore = getFormattedLore(var, var.getPermission());

                String descriptionPlaceholder = "%description%";
                String identifierPlaceholder = "%identifier%";
                String tagPlaceholder = "%tag%";
                String costPlaceholder = "%cost%";
                String costFormattedPlaceholder = "%cost_formatted%";
                String costFormattedPlaceholderRaw = "%cost_formatted_raw%";
                String variantsPlaceholder = "%variants%";
                String orderPlaceholder = "%order%";
                String trackPlaceholder = "%track_unlocked%";
                String categoryPlaceholder = "%category%";
                String rarityPlaceholder = "%rarity%";
                String joinedDescription = var.getDescription().stream().map(Utils::format).collect(Collectors.joining("\n"));

                for (int l = 0; l < lore.size(); l++) {
                    String line = lore.get(l);

                    if (line.contains(descriptionPlaceholder)) {
                        if (line.trim().equals(descriptionPlaceholder)) {
                            List<String> descriptionLines = Arrays.asList(joinedDescription.split("\n"));
                            lore.remove(l);
                            lore.addAll(l, descriptionLines);
                            l += descriptionLines.size() - 1;
                            continue;
                        } else {
                            line = line.replace(descriptionPlaceholder, joinedDescription.replace("\n", " "));
                        }
                    }

                    line = line.replace(identifierPlaceholder, var.getIdentifier());
                    line = line.replace(tagPlaceholder, var.getTag().get(0));
                    line = line.replace(costFormattedPlaceholder, "$" + formatNumber(tag.getEconomy().getAmount()));
                    line = line.replace(costFormattedPlaceholderRaw, formatNumber(tag.getEconomy().getAmount()));
                    line = line.replace(costPlaceholder, String.valueOf(tag.getEconomy().getAmount()));
                    line = line.replace(variantsPlaceholder, String.valueOf(tag.getVariants().size()));
                    line = line.replace(orderPlaceholder, String.valueOf(tag.getOrder()));
                    line = line.replace(trackPlaceholder, String.valueOf(TagManager.tagUnlockCounts.getOrDefault(var.getIdentifier(), 0)));
                    line = line.replace(categoryPlaceholder, tag.getCategory());
                    line = line.replace(rarityPlaceholder, SupremeTags.getInstance().getRarityManager().getRarity(var.getRarity()).getDisplayname());
                    line = globalPlaceholders(menuUtil.getOwner(), line);

                    lore.set(l, line);
                }

                variantMeta.setLore(color(lore));

                item_displayname = item_displayname.replaceAll("%variant%", var.getTag().get(0));
                item_displayname = item_displayname.replaceAll(identifierPlaceholder, var.getIdentifier());
                item_displayname = item_displayname.replaceAll(tagPlaceholder, var.getTag().get(0));

                item_displayname = globalPlaceholders(menuUtil.getOwner(), item_displayname);

                if (UserData.getActive(menuUtil.getOwner().getUniqueId()).equalsIgnoreCase(var.getIdentifier()) && SupremeTags.getInstance().getConfig().getBoolean("settings.active-tag-glow")) {
                    variantMeta.addEnchant(Enchantment.KNOCKBACK, 1, true);
                }

                variantMeta.setDisplayName(format(item_displayname));
                variantMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

                try {
                    ItemFlag hideDye = ItemFlag.valueOf("HIDE_DYE");
                    variantMeta.addItemFlags(hideDye);
                } catch (IllegalArgumentException ignored) {
                    // HIDE_DYE not available in this version — skip
                }
                variantMeta.addItemFlags(ItemFlag.HIDE_DESTROYS);
                variantMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                variantMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

                nbt.getItem().setItemMeta(variantMeta);

                nbt.setString("variant_identifier", var.getIdentifier());
                nbt.setBoolean("isVariant", Boolean.valueOf(true));

                this.inventory.addItem(nbt.getItem());

                currentItemsOnPage++;
            }
        }
    }

    private List<String> getFormattedLore(Variant var, String permission) {
        List<String> lore;
        boolean hasPermission = menuUtil.getOwner().hasPermission(permission) || permission.equalsIgnoreCase("none");
        boolean isSelected = UserData.getActive(menuUtil.getOwner().getUniqueId()).equalsIgnoreCase(var.getIdentifier());

        String lorePath;

        lorePath = hasPermission ? (isSelected ? "selected-lore" : "unlocked-lore") : "locked-permission";

        lore = guis.getStringList("gui.tag-variants-menu.item.lore." + lorePath);

        return lore;
    }

    public List<Variant> getVariants() {
        return this.variants;
    }
}