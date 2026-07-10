package net.noscape.project.supremetags.guis;

import de.tr7zw.nbtapi.NBTItem;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.handlers.menu.MenuUtil;
import net.noscape.project.supremetags.handlers.menu.Paged;
import net.noscape.project.supremetags.managers.TagManager;
import net.noscape.project.supremetags.storage.UserData;
import net.noscape.project.supremetags.utils.CompatUtils;
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
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.noscape.project.supremetags.utils.Utils.*;

public class TagShowcaseMenu extends Paged {

    private final Player targetPlayer;
    private final UUID targetUuid;
    private FileConfiguration guis = SupremeTags.getInstance().getConfigManager().getConfig("guis.yml").get();

    public TagShowcaseMenu(MenuUtil menuUtil, Player targetPlayer) {
        super(menuUtil);
        this.targetPlayer = targetPlayer;
        this.targetUuid = targetPlayer.getUniqueId();
        enableAutoUpdate(true);
    }

    @Override
    public String getMenuName() {
        String title = format(Objects.requireNonNull(guis.getString("gui.tag-showcase-menu.title"))
                .replace("%player%", targetPlayer.getName())
                .replaceAll("%page%", String.valueOf(this.getPage())));
        title = globalPlaceholders(menuUtil.getOwner(), title);
        return title;
    }

    @Override
    public int getSlots() {
        return guis.getInt("gui.tag-showcase-menu.size");
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

        // Cancel all clicks - this is a read-only showcase
        e.setCancelled(true);

        if (clickedItem.getType().equals(Material.valueOf(Objects.requireNonNull(guis.getString("gui.items.glass.material")).toUpperCase()))) {
            e.setCancelled(true);
            return;
        }

        NBTItem nbt = new NBTItem(e.getCurrentItem());

        if (nbt.hasTag("name")) {
            String name = nbt.getString("name");

            if (name.equalsIgnoreCase("close")) {
                player.closeInventory();
            }

            if (name.equalsIgnoreCase("back")) {
                if (page != 0) {
                    page = page - 1;
                    super.refresh();
                }
            }

            if (name.equalsIgnoreCase("next")) {
                List<Tag> accessibleTags = getAccessibleTags();
                if (accessibleTags.size() > maxItems && currentItemsOnPage >= maxItems) {
                    if (!((index + 1) >= accessibleTags.size())) {
                        page = page + 1;
                        super.refresh();
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
        getShowcaseCountOnPage();
        applyLayout(false, false, false, true);
        getShowcaseItems();
    }

    private List<Tag> getAccessibleTags() {
        Map<String, Tag> tags = SupremeTags.getInstance().getTagManager().getTags();
        List<Tag> accessibleTags = new ArrayList<>();

        for (Tag t : tags.values()) {
            if (Utils.hasTagAccess(targetPlayer, t)) {
                accessibleTags.add(t);
            }
        }

        return accessibleTags;
    }

    public void getShowcaseItems() {
        List<Tag> accessibleTags = getAccessibleTags();

        if (!accessibleTags.isEmpty()) {
            int maxItemsPerPage = guis.getInt("gui.tag-showcase-menu.tags-per-page");

            int startIndex = page * maxItemsPerPage;
            int endIndex = Math.min(startIndex + maxItemsPerPage, accessibleTags.size());

            accessibleTags.sort(getTagComparator(false, menuUtil));

            currentItemsOnPage = 0;

            List<String> slots = guis.getStringList("gui.tag-showcase-menu.slots-tag.slots");

            for (int i = startIndex; i < endIndex; i++) {
                Tag t = accessibleTags.get(i);
                if (t == null) break;

                String permission = t.getPermission();

                String displayname;
                if (SupremeTags.getInstance().getTagManager().getTagConfig().getString("tags." + t.getIdentifier() + ".displayname") != null) {
                    if (t.getCurrentTag() != null) {
                        displayname = Objects.requireNonNull(SupremeTags.getInstance().getTagManager().getTagConfig().getString("tags." + t.getIdentifier() + ".displayname")).replace("%tag%", t.getCurrentTag());
                    } else {
                        displayname = Objects.requireNonNull(SupremeTags.getInstance().getTagManager().getTagConfig().getString("tags." + t.getIdentifier() + ".displayname")).replace("%tag%", t.getTag().get(0));
                    }
                } else {
                    displayname = format("&7Tag: " + (t.getCurrentTag() != null ? t.getCurrentTag() : t.getTag().get(0)));
                }

                displayname = globalPlaceholders(menuUtil.getOwner(), displayname);

                String material;
                if (SupremeTags.getInstance().getTagManager().getTagConfig().getString("tags." + t.getIdentifier() + ".display-item") != null) {
                    material = SupremeTags.getInstance().getTagManager().getTagConfig().getString("tags." + t.getIdentifier() + ".display-item");
                } else {
                    material = "NAME_TAG";
                }

                ItemResolver.ResolvedItem resolved = ItemResolver.resolveCustomItem(menuUtil.getOwner(), material);
                ItemStack tagItem = resolved.item();
                ItemMeta tagMeta = resolved.meta();
                NBTItem nbt = new NBTItem(tagItem);

                nbt.setString("identifier", t.getIdentifier());

                if (SupremeTags.getInstance().getTagManager().getTagConfig().getInt("tags." + t.getIdentifier() + ".custom-model-data") > 0) {
                    int modelData = SupremeTags.getInstance().getTagManager().getTagConfig().getInt("tags." + t.getIdentifier() + ".custom-model-data");
                    if (tagMeta != null)
                        tagMeta.setCustomModelData(modelData);
                }

                assert tagMeta != null;

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

                List<String> lore = getShowcaseLore(t);

                String descriptionPlaceholder = "%description%";
                String identifierPlaceholder = "%identifier%";
                String tagPlaceholder = "%tag%";
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

                boolean useDefinedSlots = guis.getBoolean("gui.tag-showcase-menu.slots-tag.enable");

                if (useDefinedSlots && currentItemsOnPage < slots.size()) {
                    String rawSlot = slots.get(currentItemsOnPage);
                    try {
                        placementSlot = Integer.parseInt(rawSlot);
                    } catch (NumberFormatException ex) {
                        placementSlot = inventory.firstEmpty();
                    }
                } else {
                    placementSlot = inventory.firstEmpty();
                }

                if (placementSlot != -1) {
                    inventory.setItem(placementSlot, nbt.getItem());
                }

                currentItemsOnPage++;
            }
        } else {
            // No tags accessible - show a message item
            String noTagsDisplayname = format(Objects.requireNonNull(guis.getString("gui.tag-showcase-menu.no-tags-item.displayname"))
                    .replace("%player%", targetPlayer.getName()));

            ItemResolver.ResolvedItem resolved = ItemResolver.resolveCustomItem(menuUtil.getOwner(),
                    guis.getString("gui.tag-showcase-menu.no-tags-item.material", "BARRIER"));
            ItemStack item = resolved.item();
            ItemMeta meta = resolved.meta();

            if (meta != null) {
                meta.setDisplayName(noTagsDisplayname);
                List<String> lore = guis.getStringList("gui.tag-showcase-menu.no-tags-item.lore");
                lore.replaceAll(s -> s.replace("%player%", targetPlayer.getName()));
                meta.setLore(color(lore));
                item.setItemMeta(meta);
            }

            int slot = guis.getInt("gui.tag-showcase-menu.no-tags-item.slot", 22);
            inventory.setItem(slot, item);
        }
    }

    public void getShowcaseCountOnPage() {
        List<Tag> accessibleTags = getAccessibleTags();

        if (!accessibleTags.isEmpty()) {
            int startIndex = page * maxItems;
            int endIndex = Math.min(startIndex + maxItems, accessibleTags.size());

            currentItemsOnPage = 0;

            for (int i = startIndex; i < endIndex; i++) {
                currentItemsOnPage++;
            }
        }
    }

    protected List<String> getShowcaseLore(Tag t) {
        List<String> lore = guis.getStringList("gui.tag-showcase-menu.global-tag-lores.lore");
        return color(lore);
    }
}
