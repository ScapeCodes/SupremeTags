package net.noscape.project.supremetags.guis.tageditor;

import de.tr7zw.nbtapi.NBTItem;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.guis.MainMenu;
import net.noscape.project.supremetags.guis.TagMenu;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.handlers.menu.MenuUtil;
import net.noscape.project.supremetags.handlers.menu.Paged;
import net.noscape.project.supremetags.managers.TagManager;
import net.noscape.project.supremetags.storage.UserData;
import net.noscape.project.supremetags.utils.CompatUtils;
import net.noscape.project.supremetags.utils.ItemResolver;
import net.noscape.project.supremetags.utils.Utils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.noscape.project.supremetags.utils.Utils.*;

public class TagEditorMenu extends Paged {

    private final Map<String, Tag> tags;

    private FileConfiguration guis = SupremeTags.getInstance().getConfigManager().getConfig("guis.yml").get();

    public TagEditorMenu(MenuUtil menuUtil) {
        super(menuUtil);
        tags = SupremeTags.getInstance().getTagManager().getTags();
    }

    @Override
    public String getMenuName() {
        String title = format(Objects.requireNonNull(guis.getString("gui.tag-editor-menu.title")).replaceAll("%page%", String.valueOf(this.getPage())));
        title = globalPlaceholders(menuUtil.getOwner(), title);
        return title;
    }

    @Override
    public int getSlots() {
        return 54;
    }

    public void handleMenu(InventoryClickEvent e) {
        Player player = (Player)e.getWhoClicked();
        ArrayList<String> tag = new ArrayList<>(this.tags.keySet());
        String back = this.guis.getString("gui.items.back.displayname");
        String close = this.guis.getString("gui.items.close.displayname");
        String next = this.guis.getString("gui.items.next.displayname");
        String reset = this.guis.getString("gui.items.reset.displayname");
        String active = this.guis.getString("gui.items.active.displayname");
        NBTItem nbt = new NBTItem(e.getCurrentItem());

        if (nbt.hasTag("identifier")) {
            String identifier = nbt.getString("identifier");
            this.menuUtil.setIdentifier(identifier);
            (new SpecificTagMenu(SupremeTags.getMenuUtilIdentifier(player, identifier))).open();
        }
        if (e.getCurrentItem().getItemMeta().getDisplayName().equalsIgnoreCase(format(close)))
            player.closeInventory();
        if (e.getCurrentItem().getItemMeta().getDisplayName().equalsIgnoreCase(format(back))) {
            if (this.page != 0) {
                this.page--;
                open();
            } else {
                player.closeInventory();
                boolean useCategories = SupremeTags.getInstance().getConfig().getBoolean("settings.categories");
                if (useCategories) {
                    (new MainMenu(SupremeTags.getMenuUtil(player))).open();
                } else {
                    (new TagMenu(SupremeTags.getMenuUtil(player))).open();
                }
            }
        } else if (e.getCurrentItem().getItemMeta().getDisplayName().equalsIgnoreCase(format(next))) {
            if ((((tag.size() > this.maxItems) ? 1 : 0) & ((currentItemsOnPage >= this.maxItems) ? 1 : 0)) != 0) {
                if (this.index + 1 < tag.size()) {
                    this.page++;
                    open();
                } else {
                    e.setCancelled(true);
                }
            } else {
                e.setCancelled(true);
            }
        }
    }

    @Override
    public void setMenuItems() {
        getTagsCountOnPage();
        applyEditorLayout();
        getTagItems();
    }

    public void getTagItems() {
        Map<String, Tag> tags = SupremeTags.getInstance().getTagManager().getTags();

        List<Tag> tag = new ArrayList<>(tags.values());

        if (!tag.isEmpty()) {
            int maxItemsPerPage = guis.getInt("gui.tag-menu.tags-per-page");

            int startIndex = page * maxItemsPerPage;
            int endIndex = Math.min(startIndex + maxItemsPerPage, tag.size());

            tag.sort(getTagComparator(SupremeTags.getInstance().getConfig().getBoolean("settings.prioritise-selected-tag"), menuUtil));

            currentItemsOnPage = 0;

            List<String> slots = guis.getStringList("gui.tag-menu.slots-tag.slots");

            for (int i = startIndex; i <= endIndex; i++) {
                if (i > tag.size() - 1) {
                    break;
                }

                Tag t = tag.get(i);
                if (t == null) break;

                if (i == endIndex) {
                    continue;
                }

                String permission = t.getPermission();

                String displayname;

                if (menuUtil.getOwner().hasPermission(t.getPermission()) || permission.equalsIgnoreCase("none")) {
                    if (SupremeTags.getInstance().getTagManager().getTagConfig().getString("tags." + t.getIdentifier() + ".displayname") != null) {
                        if (t.getCurrentTag() != null) {
                            displayname = Objects.requireNonNull(SupremeTags.getInstance().getTagManager().getTagConfig().getString("tags." + t.getIdentifier() + ".displayname")).replace("%tag%", t.getCurrentTag());
                        } else {
                            displayname = Objects.requireNonNull(SupremeTags.getInstance().getTagManager().getTagConfig().getString("tags." + t.getIdentifier() + ".displayname")).replace("%tag%", t.getTag().get(0));
                        }
                    } else {
                        displayname = format("&7Tag: " + t.getCurrentTag());
                    }
                } else {
                    if (guis.getString("gui.tag-menu.global-locked-tag.displayname") == null) {
                        if (t.getCurrentTag() != null) {
                            displayname = Objects.requireNonNull(SupremeTags.getInstance().getTagManager().getTagConfig().getString("tags." + t.getIdentifier() + ".displayname")).replace("%tag%", t.getCurrentTag());
                        } else {
                            displayname = Objects.requireNonNull(SupremeTags.getInstance().getTagManager().getTagConfig().getString("tags." + t.getIdentifier() + ".displayname")).replace("%tag%", t.getTag().get(0));
                        }
                    } else {
                        if (t.getCurrentTag() != null) {
                            displayname = Objects.requireNonNull(guis.getString("gui.tag-menu.global-locked-tag.displayname")).replace("%tag%", t.getCurrentTag());
                        } else {
                            displayname = Objects.requireNonNull(guis.getString("gui.tag-menu.global-locked-tag.displayname")).replace("%tag%", t.getTag().get(0));
                        }
                    }
                }

                displayname = globalPlaceholders(menuUtil.getOwner(), displayname);

                String material;

                if (menuUtil.getOwner().hasPermission(t.getPermission()) || permission.equalsIgnoreCase("none")) {
                    if (SupremeTags.getInstance().getTagManager().getTagConfig().getString("tags." + t.getIdentifier() + ".display-item") != null) {
                        material = SupremeTags.getInstance().getTagManager().getTagConfig().getString("tags." + t.getIdentifier() + ".display-item");
                    } else {
                        material = "NAME_TAG";
                    }
                } else {
                    if (guis.getString("gui.tag-menu.global-locked-tag.display-item") != null) {
                        material = guis.getString("gui.tag-menu.global-locked-tag.display-item");
                    } else {
                        material = "NAME_TAG";
                    }
                }

                assert permission != null;

                ItemResolver.ResolvedItem resolved = ItemResolver.resolveCustomItem(menuUtil.getOwner(), material);
                ItemStack tagItem = resolved.item();
                ItemMeta tagMeta = resolved.meta();
                NBTItem nbt = new NBTItem(tagItem);

                nbt.setString("identifier", t.getIdentifier());

                if (menuUtil.getOwner().hasPermission(t.getPermission()) || permission.equalsIgnoreCase("none")) {
                    if (SupremeTags.getInstance().getTagManager().getTagConfig().getInt("tags." + t.getIdentifier() + ".custom-model-data") > 0) {
                        int modelData = SupremeTags.getInstance().getTagManager().getTagConfig().getInt("tags." + t.getIdentifier() + ".custom-model-data");
                        if (tagMeta != null)
                            tagMeta.setCustomModelData(modelData);
                    }
                } else {
                    if (guis.getInt("gui.tag-menu.global-locked-tag.custom-model-data") > 0) {
                        int modelData = guis.getInt("gui.tag-menu.global-locked-tag.custom-model-data");
                        if (tagMeta != null)
                            tagMeta.setCustomModelData(modelData);
                    }
                }

                assert tagMeta != null;

                if (UserData.getActive(menuUtil.getOwner().getUniqueId()).equalsIgnoreCase(t.getIdentifier()) && SupremeTags.getInstance().getConfig().getBoolean("settings.active-tag-glow")) {
                    tagMeta.addEnchant(Enchantment.KNOCKBACK, 1, true);
                }

                tagMeta.setDisplayName(format(displayname));
                tagMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                try {
                    ItemFlag hideDye = ItemFlag.valueOf("HIDE_DYE");
                    tagMeta.addItemFlags(hideDye);
                } catch (IllegalArgumentException ignored) {
                    // HIDE_DYE not available in this version — skip
                }
                tagMeta.addItemFlags(ItemFlag.HIDE_DESTROYS);
                tagMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                tagMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

                List<String> lore = getFormattedLore(t, permission);

                String descriptionPlaceholder = "%description%";
                String identifierPlaceholder = "%identifier%";
                String tagPlaceholder = "%tag%";
                String costPlaceholder = "%cost%";
                String costFormattedPlaceholder = "%cost_formatted%";
                String costFormattedPlaceholderRaw = "%cost_formatted_raw%";
                String variantsPlaceholder = "%variants%";
                String orderPlaceholder = "%order%";
                String trackPlaceholder = "%track_unlocked%";
                String effectsPlaceholder = "%effects%";
                String categoryPlaceholder = "%category%";
                String rarityPlaceholder = "%rarity%";
                String effectsListPlaceholder = "%effects_list%";
                String joinedDescription = t.getDescription().stream().map(Utils::format).collect(Collectors.joining("\n"));

                String joinedEffects;

                String effects_list;

                if (!t.getEffects().isEmpty()) {
                    String formatEffectTemplate = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.effects-replace-style");

                    joinedEffects = t.getEffects().keySet().stream()
                            .map(PotionEffectType::getName)
                            .map(Utils::format)
                            .map(effect -> formatEffectTemplate.replace("%effect%", effect))
                            .collect(Collectors.joining("\n"));

                    effects_list = t.getEffects().keySet().stream()
                            .map(CompatUtils::getEffectKey)
                            .collect(Collectors.joining(", "));
                } else {
                    joinedEffects = format(SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.no-effects"));
                    effects_list = format(SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.no-effects"));
                }

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

                    if (line.contains(effectsPlaceholder)) {
                        if (line.trim().equals(effectsPlaceholder)) {
                            List<String> effectLines = Arrays.asList(joinedEffects.split("\n"));
                            lore.remove(l);
                            lore.addAll(l, effectLines);
                            l += effectLines.size() - 1;
                            continue;
                        } else {
                            line = line.replace(effectsPlaceholder, joinedEffects.replace("\n", " "));
                        }
                    }

                    line = line.replace(identifierPlaceholder, t.getIdentifier());
                    if (t.getCurrentTag() != null) {
                        line = line.replace(tagPlaceholder, t.getCurrentTag());
                    } else {
                        line = line.replace(tagPlaceholder, t.getTag().getFirst());
                    }
                    line = line.replace(costFormattedPlaceholder, "$" + formatNumber(t.getEconomy().getAmount()));
                    line = line.replace(costFormattedPlaceholderRaw, formatNumber(t.getEconomy().getAmount()));
                    line = line.replace(costPlaceholder, String.valueOf(t.getEconomy().getAmount()));
                    line = line.replace(variantsPlaceholder, String.valueOf(t.getVariants().size()));
                    line = line.replace(orderPlaceholder, String.valueOf(t.getOrder()));
                    line = line.replace(trackPlaceholder, String.valueOf(TagManager.tagUnlockCounts.getOrDefault(t.getIdentifier(), 0)));
                    line = line.replace(categoryPlaceholder, t.getCategory());
                    line = line.replace(rarityPlaceholder, SupremeTags.getInstance().getRarityManager().getRarity(t.getRarity()).getDisplayname());
                    line = line.replace(effectsListPlaceholder, effects_list);
                    line = globalPlaceholders(menuUtil.getOwner(), line);

                    lore.set(l, line);
                }

                tagMeta.setLore(color(lore));
                nbt.getItem().setItemMeta(tagMeta);
                nbt.setString("identifier", t.getIdentifier());

                int placementSlot;

                boolean useDefinedSlots = guis.getBoolean("gui.tag-menu.slots-tag.enable");

                if (useDefinedSlots && currentItemsOnPage < slots.size()) {
                    String rawSlot = slots.get(currentItemsOnPage);
                    try {
                        placementSlot = Integer.parseInt(rawSlot);
                    } catch (NumberFormatException e) {
                        placementSlot = inventory.firstEmpty();
                    }
                } else if (useDefinedSlots) {
                    placementSlot = inventory.firstEmpty();
                } else {
                    placementSlot = inventory.firstEmpty();
                }

                if (placementSlot != -1) {
                    inventory.setItem(placementSlot, nbt.getItem());
                    if (t.isAnimated()) {
                        animatedSlots.add(placementSlot);
                    }
                }

                currentItemsOnPage++;
            }
        }
    }
}