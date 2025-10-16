package net.noscape.project.supremetags.guis;

import de.tr7zw.nbtapi.NBTItem;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.api.events.TagBuyEvent;
import net.noscape.project.supremetags.api.events.TagResetEvent;
import net.noscape.project.supremetags.guis.personaltags.PersonalTagsMenu;
import net.noscape.project.supremetags.guis.tagactions.TagActionsMenu;
import net.noscape.project.supremetags.guis.variant.TagVariantsMenu;
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

import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.noscape.project.supremetags.utils.Utils.*;

public class TagMenu extends Paged {

    private final Map<String, Tag> tags;
    private FileConfiguration guis = SupremeTags.getInstance().getConfigManager().getConfig("guis.yml").get();

    public TagMenu(MenuUtil menuUtil) {
        super(menuUtil);
        tags = SupremeTags.getInstance().getTagManager().getTags();
    }

    @Override
    public String getMenuName() {
        String title = format(Objects.requireNonNull(guis.getString("gui.tag-menu.title")).replaceAll("%page%", String.valueOf(this.getPage())));
        title = globalPlaceholders(menuUtil.getOwner(), title);
        return title;
    }

    @Override
    public int getSlots() {
        return guis.getInt("gui.tag-menu.size");
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

        String back = guis.getString("gui.items.back.displayname");
        String close = guis.getString("gui.items.close.displayname");
        String next = guis.getString("gui.items.next.displayname");
        String reset = guis.getString("gui.items.reset.displayname");
        String ptags = guis.getString("gui.items.personal-tags.displayname");
        String search = guis.getString("gui.items.search.displayname");

        String insufficient = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.insufficient-funds").replaceAll("%prefix%", Objects.requireNonNull(SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.prefix")));
        String unlocked = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.tag-unlocked").replaceAll("%prefix%", Objects.requireNonNull(SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.prefix")));

        String no_tag_selected = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.no-tag-selected").replaceAll("%prefix%", Objects.requireNonNull(SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.prefix")));

        insufficient = replacePlaceholders(player, insufficient);

        if (e.getCurrentItem().getType().equals(Material.valueOf(Objects.requireNonNull(guis.getString("gui.items.glass.material")).toUpperCase()))) {
            e.setCancelled(true);
        }

        ArrayList<String> tag = new ArrayList<>(tags.keySet());

        NBTItem nbt = new NBTItem(e.getCurrentItem());
        if (nbt.hasTag("identifier")) {
            String identifier = nbt.getString("identifier");
            Tag t = SupremeTags.getInstance().getTagManager().getTag(identifier);
            boolean tagActionsEnabled = guis.getBoolean("gui.tag-actions-menu.enable");

            if (tagActionsEnabled) {
                if (!menuUtil.getOwner().hasPermission(t.getPermission()) && !t.isCostTag()) {
                    sendLockedMessage(player);
                    return;
                }
                player.closeInventory();
                new TagActionsMenu(SupremeTags.getMenuUtilIdentifier(player, identifier)).open();
                return;
            }

            boolean isRightClick = e.getClick().isRightClick();
            boolean hasVariants = !t.getVariants().isEmpty();

            if (isRightClick && hasVariants) {
                player.closeInventory();
                new TagVariantsMenu(SupremeTags.getMenuUtil(player), t).open();
                return;
            }

            boolean isCostTag = t.isEcoEnabled();
            boolean hasPerm = player.hasPermission(t.getPermission()) || t.getPermission().equalsIgnoreCase("none");
            boolean isActive = UserData.getActive(player.getUniqueId()).equalsIgnoreCase(identifier);
            boolean canDeactivate = SupremeTags.getInstance().isDeactivateClick();

            if (hasPerm) {
                if (!isActive && identifier != null) {
                    handleTagAssign(player, identifier, t);
                } else if (isActive && canDeactivate) {
                    handleTagReset(player, t);
                }
            } else if (isCostTag) { // Only check cost if it's actually a cost tag
                if (hasAmount(player, t.getEcoType(), t.getEcoAmount())) {
                    TagBuyEvent tagevent = new TagBuyEvent(player, identifier, t.getEcoAmount(), false);
                    Bukkit.getPluginManager().callEvent(tagevent);
                    if (tagevent.isCancelled()) return;

                    take(player, t.getEcoType(), t.getEcoAmount());
                    addPerm(player, t.getPermission());

                    if (SupremeTags.getInstance().getConfig().getBoolean("settings.gui-messages")) {
                        msgPlayer(player, unlocked
                                .replace("%identifier%", t.getIdentifier())
                                .replace("%tag%", t.getCurrentTag()));
                    }
                    super.open();
                } else {
                    if (SupremeTags.getInstance().getConfig().getBoolean("settings.gui-messages")) {
                        insufficient = replacePlaceholders(menuUtil.getOwner(), insufficient);
                        msgPlayer(player, insufficient.replaceAll("%cost%", String.valueOf(t.getEcoAmount())));
                    }
                }
            } else {
                // Non-cost tag but no permission → treat as locked
                sendLockedMessage(player);
            }
        } else {

            if (nbt.hasTag("custom-item")) {
                String name = nbt.getString("custom-item");

                List<String> click_commands = guis.getStringList("gui.tag-menu.custom-items." + name + ".click-commands");

                for (String option : click_commands) {
                    if (option.startsWith("[message]") || option.startsWith("[MESSAGE]")) {
                        String message = option.replace("[message] ", "").replace("[MESSAGE] ", "");
                        message = replacePlaceholders(menuUtil.getOwner(), message);
                        msgPlayer(menuUtil.getOwner(), message);
                    }

                    if (option.startsWith("[player]") || option.startsWith("[PLAYER]")) {
                        String command = option.replace("[player] ", "").replace("[PLAYER] ", "");
                        command = command.replaceAll("%player%", menuUtil.getOwner().getName());
                        menuUtil.getOwner().performCommand(command);
                    }

                    if (option.startsWith("[console]") || option.startsWith("[CONSOLE]")) {
                        String command = option.replace("[console] ", "").replace("[CONSOLE] ", "");
                        command = command.replaceAll("%player%", menuUtil.getOwner().getName());
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replaceAll("%player%", menuUtil.getOwner().getName()));
                    }

                    if (option.startsWith("[broadcast]") || option.startsWith("[BROADCAST]")) {
                        String message = option.replace("[message] ", "").replace("[BROADCAST] ", "");
                        message = replacePlaceholders(menuUtil.getOwner(), message);
                        Bukkit.broadcastMessage(message);
                    }

                    if (option.startsWith("[next-page]") || option.startsWith("[NEXT-PAGE]")) {
                        if (tag.size() > maxItems & currentItemsOnPage >= maxItems) {
                            if (!((index + 1) >= tag.size())) {
                                page = page + 1;
                                super.open();
                            } else {
                                e.setCancelled(true);
                            }
                        } else {
                            e.setCancelled(true);
                        }
                    }

                    if (option.startsWith("[previous-page]") || option.startsWith("[PREVIOUS-PAGE]")) {
                        if (page != 0) {
                            page = page - 1;
                            super.open();
                        }
                    }

                    if (option.startsWith("[close]") || option.startsWith("[CLOSE]")) {
                        menuUtil.getOwner().closeInventory();
                    }
                }
                return;
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

                if (name.equalsIgnoreCase("filter")) {
                    String current_filter = menuUtil.getFilter();

                    List<String> categories = SupremeTags.getInstance().getCategoryManager().getCatorgies();
                    List<String> filterOptions = new ArrayList<>();
                    filterOptions.add("all");
                    filterOptions.add("players");
                    for (String category : categories) {
                        filterOptions.add("category:" + category.toLowerCase());
                    }

                    String currentFilterLower = current_filter.toLowerCase();
                    int currentIndex = filterOptions.indexOf(currentFilterLower);
                    if (currentIndex == -1) currentIndex = 0;

                    int nextIndex = (currentIndex + 1) % filterOptions.size();
                    String nextFilter = filterOptions.get(nextIndex);

                    menuUtil.setFilter(nextFilter);
                    super.open();
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
                            msgPlayer(player, SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.reset-message").replaceAll("%prefix%", Objects.requireNonNull(SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.prefix"))));
                        }

                        playConfigSound(player, "reset-tag");
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
                            msgPlayer(player, SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.reset-message").replaceAll("%prefix%", Objects.requireNonNull(SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.prefix"))));
                        }
                        playConfigSound(player, "reset-tag");
                    }
                }

                if (name.equalsIgnoreCase("back")) {
                    if (page != 0) {
                        page = page - 1;
                        super.open();
                    }
                }

                if (name.equalsIgnoreCase("next")) {
                    if (tag.size() > maxItems & currentItemsOnPage >= maxItems) {
                        if (!((index + 1) >= tag.size())) {
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
    }

    @Override
    public void setMenuItems() {
        getTagsCountOnPage();
        applyLayout(false, false, false);
        getTagItems();
    }

    public void getTagItems() {
        Map<String, Tag> tags = SupremeTags.getInstance().getTagManager().getTags();

        List<Tag> tag = new ArrayList<>();

        String filter = menuUtil.getFilter();
        if (filter == null) filter = "all";

        String sort = menuUtil.getSort();

        if (SupremeTags.getInstance().getConfig().getBoolean("settings.only-show-player-access-tags")) {
            for (Tag t : tags.values()) {
                if (menuUtil.getOwner().hasPermission(t.getPermission())) {
                    tag.add(t);
                }
            }
        } else {
            tag.addAll(tags.values());
        }

        if (filter.equalsIgnoreCase("players")) {
            tag.removeIf(t -> !menuUtil.getOwner().hasPermission(t.getPermission()));
        }

        if (filter.startsWith("category:")) {
            String category = filter.replace("category:", "");
            tag.removeIf(t -> !t.getCategory().equalsIgnoreCase(category));
        }

        if (sort.startsWith("rarity:")) {
            String rarity = sort.replace("rarity:", "");
            tag.removeIf(t -> !t.getRarity().equalsIgnoreCase(rarity));
        }

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

                if (SupremeTags.getInstance().getConfig().getBoolean("settings.only-show-player-access-tags")) {
                    if (!menuUtil.getOwner().hasPermission(permission) || !menuUtil.getOwner().hasPermission("supremetags.tag.*") &&
                            (!SupremeTags.getInstance().getConfig().getBoolean("settings.locked-view") &&
                                    !SupremeTags.getInstance().getConfig().getBoolean("settings.cost-system"))) {
                        continue;
                    }
                }

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
                    line = line.replace(tagPlaceholder, t.getTag().get(0));
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

                if (guis.getBoolean("gui.tag-menu.slots-tag.enable")) {
                    if (currentItemsOnPage < slots.size()) {
                        try {
                            int slot = Integer.parseInt(slots.get(currentItemsOnPage));
                            inventory.setItem(slot, nbt.getItem());
                        } catch (NumberFormatException e) {
                            inventory.addItem(nbt.getItem());
                        }
                    } else {
                        inventory.addItem(nbt.getItem());
                    }
                } else {
                    inventory.addItem(nbt.getItem());
                }

                currentItemsOnPage++;
            }
        }
    }

}