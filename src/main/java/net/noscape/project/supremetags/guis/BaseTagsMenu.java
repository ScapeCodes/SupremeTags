package net.noscape.project.supremetags.guis;

import net.noscape.project.supremetags.guis.personaltags.PersonalTagsHubMenu;
import net.noscape.project.supremetags.utils.ItemData;

import com.cryptomorin.xseries.XItemFlag;
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
import net.noscape.project.supremetags.utils.ItemResolver;
import net.noscape.project.supremetags.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.noscape.project.supremetags.utils.Utils.*;

public abstract class BaseTagsMenu extends Paged {

    private static final Pattern CUSTOM_PLACEHOLDER_PATTERN = Pattern.compile("%custom-placeholder_(.*?)%");

    protected final Map<String, Tag> tags;
    protected final FileConfiguration guis = SupremeTags.getInstance().getConfigManager().getConfig("guis.yml").get();

    protected BaseTagsMenu(MenuUtil menuUtil) {
        super(menuUtil);
        this.tags = SupremeTags.getInstance().getTagManager().getTags();
        enableAutoUpdate(true);
    }

    protected abstract boolean isCategoryMenu();

    protected boolean isFavouritesMenu() {
        return false;
    }

    @Override
    protected boolean shouldShowBackOnFirstPage() {
        return shouldBackToMainMenu();
    }

    protected String getMenuConfigPath() {
        return isFavouritesMenu() ? "gui.favourites-menu" : "gui.tag-menu";
    }

    protected abstract String getRawTitle();

    @Override
    public String getMenuName() {
        this.maxItems = guis.getInt(getMenuConfigPath() + ".tags-per-page");
        String title = format(applyPagePlaceholders(Objects.requireNonNull(getRawTitle()), getVisibleTags().size()));
        title = globalPlaceholders(menuUtil.getOwner(), title);
        return title;
    }

    @Override
    public int getSlots() {
        return isCategoryMenu() ? 54 : guis.getInt(getMenuConfigPath() + ".size");
    }

    @Override
    public void setMenuItems() {
        List<Tag> visibleTags = getVisibleTags();
        this.maxItems = guis.getInt(getMenuConfigPath() + ".tags-per-page");
        currentItemsOnPage = Math.min(maxItems, Math.max(0, visibleTags.size() - (page * maxItems)));
        applyLayout(false, isCategoryMenu(), false, false);
        renderTagItems(visibleTags);
    }

    @Override
    protected ItemStack buildAnimatedItem(int slot) {
        String identifier = animatedTagSlots.get(slot);
        if (identifier == null) return null;

        Tag tag = SupremeTags.getInstance().getTagManager().getTag(identifier);
        if (tag == null) return null;

        return buildMenuTagItem(tag, tag.getPermission(), getMenuConfigPath().replace("gui.", ""));
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();

        if (e.getClickedInventory() == null) {
            return;
        }

        if (e.getClickedInventory().getType() == InventoryType.PLAYER) {
            e.setCancelled(true);
            return;
        }

        ItemStack clickedItem = e.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        String insufficient = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.insufficient-funds").replace("%prefix%", Objects.requireNonNull(SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.prefix")));
        String unlocked = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.tag-unlocked").replace("%prefix%", Objects.requireNonNull(SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.prefix")));
        String noTagSelected = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.no-tag-selected").replace("%prefix%", Objects.requireNonNull(SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.prefix")));

        insufficient = replacePlaceholders(player, insufficient);

        Material glassMaterial = Material.matchMaterial(guis.getString("gui.items.glass.material", "BLACK_STAINED_GLASS_PANE").toUpperCase(Locale.ROOT));
        if (glassMaterial != null && clickedItem.getType().equals(glassMaterial)) {
            e.setCancelled(true);
        }

        ItemStack nbt = clickedItem;
        if (ItemData.has(nbt, "identifier")) {
            handleTagClick(e, player, ItemData.getString(nbt, "identifier"), insufficient, unlocked);
            return;
        }

        if (ItemData.has(nbt, "custom-item")) {
            handleCustomItemClick(e, ItemData.getString(nbt, "custom-item"));
            return;
        }

        if (ItemData.has(nbt, "name")) {
            handleNamedItemClick(e, player, ItemData.getString(nbt, "name"), noTagSelected);
        }
    }

    protected List<Tag> getVisibleTags() {
        List<Tag> tag = new ArrayList<>();

        String filter = menuUtil.getFilter();
        if (filter == null) filter = "all";

        String sort = menuUtil.getSort();
        if (sort == null) sort = "none";

        for (Tag t : tags.values()) {
            if (isFavouritesMenu() && !UserData.getFavourites(menuUtil.getOwner().getUniqueId()).contains(t.getIdentifier())) {
                continue;
            }

            if (isCategoryMenu() && !t.getCategory().equalsIgnoreCase(menuUtil.getCategory())) {
                continue;
            }

            if (SupremeTags.getInstance().getConfig().getBoolean("settings.only-show-player-access-tags") && !Utils.hasTagAccess(menuUtil.getOwner(), t)) {
                continue;
            }

            tag.add(t);
        }

        if (filter.equalsIgnoreCase("players")) {
            tag.removeIf(t -> !Utils.hasTagAccess(menuUtil.getOwner(), t));
        }

        if (!isCategoryMenu() && filter.startsWith("category:")) {
            String category = filter.replace("category:", "");
            tag.removeIf(t -> !t.getCategory().equalsIgnoreCase(category));
        }

        if (sort.startsWith("rarity:")) {
            String rarity = sort.replace("rarity:", "");
            tag.removeIf(t -> !t.getRarity().equalsIgnoreCase(rarity));
        }

        tag.sort(getTagComparator(SupremeTags.getInstance().getConfig().getBoolean("settings.prioritise-selected-tag"), menuUtil));
        return tag;
    }

    protected void renderTagItems(List<Tag> tag) {
        if (!tag.isEmpty()) {
            int maxItemsPerPage = guis.getInt(getMenuConfigPath() + ".tags-per-page");
            this.maxItems = maxItemsPerPage;

            int startIndex = page * maxItemsPerPage;
            int endIndex = Math.min(startIndex + maxItemsPerPage, tag.size());
            this.index = startIndex;
            this.currentItemsOnPage = 0;

            List<String> slots = guis.getStringList(getMenuConfigPath() + ".slots-tag.slots");

            for (int i = startIndex; i < endIndex; i++) {
                Tag t = tag.get(i);
                if (t == null) break;

                boolean hasAccess = Utils.hasTagAccess(menuUtil.getOwner(), t);
                if (SupremeTags.getInstance().getConfig().getBoolean("settings.only-show-player-access-tags")) {
                    if (!hasAccess && !menuUtil.getOwner().hasPermission("supremetags.tag.*") &&
                            (!SupremeTags.getInstance().getConfig().getBoolean("settings.locked-view") &&
                                    !SupremeTags.getInstance().getConfig().getBoolean("settings.cost-system"))) {
                        continue;
                    }
                }

                ItemStack tagItem = buildMenuTagItem(t, t.getPermission(), getMenuConfigPath().replace("gui.", ""));
                int placementSlot = getPlacementSlot(slots);

                if (placementSlot != -1) {
                    inventory.setItem(placementSlot, tagItem);
                    if (t.isAnimated()) {
                        registerAnimatedTagSlot(placementSlot, t.getIdentifier());
                    }
                }

                currentItemsOnPage++;
                index = i;
            }
        } else if (guis.getBoolean(getMenuConfigPath() + ".items.no-tags-item.enable")) {
            Material material = Material.matchMaterial(guis.getString(getMenuConfigPath() + ".items.no-tags-item.material", "BARRIER").toUpperCase(Locale.ROOT));
            this.inventory.setItem(guis.getInt(getMenuConfigPath() + ".items.no-tags-item.slot"), makeItem(material != null ? material : Material.BARRIER, guis.getString(getMenuConfigPath() + ".items.no-tags-item.displayname"), guis.getInt(getMenuConfigPath() + ".items.no-tags-item.custom-model-data"), new ArrayList<>(guis.getStringList(getMenuConfigPath() + ".items.no-tags-item.lore"))));
        }
    }

    protected int getPlacementSlot(List<String> slots) {
        boolean useDefinedSlots = guis.getBoolean(getMenuConfigPath() + ".slots-tag.enable");

        if (useDefinedSlots && currentItemsOnPage < slots.size()) {
            String rawSlot = slots.get(currentItemsOnPage);
            try {
                return Integer.parseInt(rawSlot);
            } catch (NumberFormatException ignored) {
                return inventory.firstEmpty();
            }
        }

        return inventory.firstEmpty();
    }

    protected void handleTagClick(InventoryClickEvent e, Player player, String identifier, String insufficient, String unlocked) {
        Tag t = SupremeTags.getInstance().getTagManager().getTag(identifier);
        if (t == null) return;

        boolean tagActionsEnabled = guis.getBoolean("gui.tag-actions-menu.enable");
        if (tagActionsEnabled) {
            if (!Utils.hasTagAccess(player, t) && !t.isCostTag()) {
                sendLockedMessage(player, t);
                return;
            }
            player.closeInventory();
            new TagActionsMenu(SupremeTags.getMenuUtilIdentifier(player, identifier)).open();
            return;
        }

        if (e.getClick().isRightClick() && !t.getVariants().isEmpty()) {
            player.closeInventory();
            new TagVariantsMenu(SupremeTags.getMenuUtil(player), t).open();
            return;
        }

        boolean isCostTag = t.isEcoEnabled();
        boolean hasPerm = Utils.hasTagAccess(player, t);
        boolean isActive = UserData.getActive(player.getUniqueId()).equalsIgnoreCase(identifier);
        boolean canDeactivate = SupremeTags.getInstance().isDeactivateClick();

        if (guis.getBoolean("gui.items.favourites.enable") && e.getClick().isShiftClick() && e.getClick().isLeftClick() && hasPerm) {
            List<String> favourites = UserData.getFavourites(player.getUniqueId());
            if (!favourites.contains(identifier)) {
                favourites.add(identifier);
                UserData.setFavourites(player, favourites);
                msgPlayer(player, SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get()
                        .getString("messages.favourite-added", "%prefix% &aAdded &e%identifier% &ato your favourites!")
                        .replace("%prefix%", Objects.requireNonNull(SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.prefix", "")))
                        .replace("%identifier%", identifier));
                playConfigSound(player, "favourite-added");
            }
        }

        if (hasPerm) {
            if (!isActive) {
                if (shouldConfirmTagSelection()) {
                    openTagSelectionConfirmation(player, identifier);
                    return;
                }

                handleTagAssign(player, identifier, t);
            } else if (canDeactivate) {
                handleTagReset(player, t);
            }
        } else if (isCostTag) {
            if (!Utils.hasTagRequirements(player, t)) {
                sendLockedMessage(player, t);
                return;
            }

            if (hasAmount(player, t.getEcoType(), t.getEcoAmount(), t.getIdentifier())) {
                if (shouldConfirmTagPurchase()) {
                    openTagPurchaseConfirmation(player, identifier);
                    return;
                }

                purchaseTag(player, t);
                super.refresh();
            } else if (SupremeTags.getInstance().getConfig().getBoolean("settings.gui-messages")) {
                insufficient = replacePlaceholders(menuUtil.getOwner(), insufficient);
                msgPlayer(player, insufficient.replace("%cost%", String.valueOf(t.getEcoAmount())));
            }
        } else {
            sendLockedMessage(player, t);
        }
    }

    protected void handleCustomItemClick(InventoryClickEvent e, String name) {
        for (String option : guis.getStringList("gui.tag-menu.custom-items." + name + ".click-commands")) {
            if (option.startsWith("[message]") || option.startsWith("[MESSAGE]")) {
                String message = option.replace("[message] ", "").replace("[MESSAGE] ", "");
                message = replacePlaceholders(menuUtil.getOwner(), message);
                msgPlayer(menuUtil.getOwner(), message);
            }

            if (option.startsWith("[player]") || option.startsWith("[PLAYER]")) {
                String command = option.replace("[player] ", "").replace("[PLAYER] ", "");
                command = command.replace("%player%", menuUtil.getOwner().getName());
                menuUtil.getOwner().performCommand(command);
            }

            if (option.startsWith("[console]") || option.startsWith("[CONSOLE]")) {
                String command = option.replace("[console] ", "").replace("[CONSOLE] ", "");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", menuUtil.getOwner().getName()));
            }

            if (option.startsWith("[broadcast]") || option.startsWith("[BROADCAST]")) {
                String message = option.replace("[message] ", "").replace("[BROADCAST] ", "");
                message = replacePlaceholders(menuUtil.getOwner(), message);
                Bukkit.broadcastMessage(message);
            }

            if (option.startsWith("[next-page]") || option.startsWith("[NEXT-PAGE]")) {
                nextPage(e);
            }

            if (option.startsWith("[previous-page]") || option.startsWith("[PREVIOUS-PAGE]")) {
                previousPage();
            }

            if (option.startsWith("[close]") || option.startsWith("[CLOSE]")) {
                menuUtil.getOwner().closeInventory();
            }
        }
    }

    protected void handleNamedItemClick(InventoryClickEvent e, Player player, String name, String noTagSelected) {
        if (name.equalsIgnoreCase("close")) {
            player.closeInventory();
        }

        if (name.equalsIgnoreCase("personal-tags")) {
            new PersonalTagsHubMenu(SupremeTags.getMenuUtil(player)).open();
        }

        if (name.equalsIgnoreCase("favourites")) {
            new FavouritesMenu(SupremeTags.getMenuUtil(player)).open();
        }

        if (name.equalsIgnoreCase("search")) {
            player.closeInventory();
            openSearchContainer(player);
        }

        if (name.equalsIgnoreCase("filter")) {
            cycleFilter();
        }

        if (name.equalsIgnoreCase("sort")) {
            cycleSort();
        }

        if (name.equalsIgnoreCase("reset")) {
            resetTag(player, noTagSelected);
        }

        if (name.equalsIgnoreCase("back")) {
            if (page != 0) {
                previousPage();
            } else if (isCategoryMenu()) {
                player.closeInventory();
                new MainMenu(SupremeTags.getMenuUtil(player)).open();
            } else if (shouldBackToMainMenu()) {
                player.closeInventory();
                new MainMenu(SupremeTags.getMenuUtil(player)).open();
            }
        }

        if (name.equalsIgnoreCase("next")) {
            nextPage(e);
        }
    }

    protected void cycleFilter() {
        String currentFilter = menuUtil.getFilter();
        if (currentFilter == null) currentFilter = "all";

        List<String> filterOptions = new ArrayList<>();
        if (hasConfiguredString("gui.items.filter.filters.selected.all-tags")
                || hasConfiguredString("gui.items.filter.filters.unselected.all-tags")) {
            filterOptions.add("all");
        }
        if (hasConfiguredString("gui.items.filter.filters.selected.your-tags")
                || hasConfiguredString("gui.items.filter.filters.unselected.your-tags")) {
            filterOptions.add("players");
        }

        if (!isCategoryMenu() && (hasConfiguredString("gui.items.filter.filters.selected.category")
                || hasConfiguredString("gui.items.filter.filters.unselected.category"))) {
            for (String category : SupremeTags.getInstance().getCategoryManager().getCatorgies()) {
                filterOptions.add("category:" + category.toLowerCase());
            }
        }

        if (filterOptions.isEmpty()) {
            return;
        }

        int currentIndex = filterOptions.indexOf(currentFilter.toLowerCase());
        if (currentIndex == -1) currentIndex = 0;

        page = 0;
        menuUtil.setFilter(filterOptions.get((currentIndex + 1) % filterOptions.size()));
        super.refresh();
    }

    protected void cycleSort() {
        String currentSort = menuUtil.getSort();
        Set<String> rarities = SupremeTags.getInstance().getRarityManager().getRarityMap().keySet();

        List<String> sortOptions = new ArrayList<>();
        if (hasConfiguredString("gui.items.sort.sorts.selected.no-filter")
                || hasConfiguredString("gui.items.sort.sorts.unselected.no-filter")) {
            sortOptions.add("none");
        }
        if (hasConfiguredString("gui.items.sort.sorts.selected.popularity")
                || hasConfiguredString("gui.items.sort.sorts.unselected.popularity")) {
            sortOptions.add("popularity");
        }
        if (hasConfiguredString("gui.items.sort.sorts.selected.recently-used")
                || hasConfiguredString("gui.items.sort.sorts.unselected.recently-used")) {
            sortOptions.add("recently-used");
        }
        if (hasConfiguredString("gui.items.sort.sorts.selected.rarity")
                || hasConfiguredString("gui.items.sort.sorts.unselected.rarity")) {
            for (String rarity : rarities) {
                sortOptions.add("rarity:" + rarity);
            }
        }

        if (sortOptions.isEmpty()) {
            return;
        }

        int currentIndex = sortOptions.indexOf(currentSort == null ? "none" : currentSort.toLowerCase());
        if (currentIndex == -1) currentIndex = 0;

        page = 0;
        menuUtil.setSort(sortOptions.get((currentIndex + 1) % sortOptions.size()));
        super.refresh();
    }

    private boolean hasConfiguredString(String path) {
        String value = guis.getString(path);
        return value != null && !value.isBlank();
    }

    private boolean shouldBackToMainMenu() {
        return !isCategoryMenu()
                && !isFavouritesMenu()
                && SupremeTags.getInstance().getConfig().getBoolean("settings.categories");
    }

    protected void resetTag(Player player, String noTagSelected) {
        if (menuUtil.getIdentifier() == null || menuUtil.getIdentifier().equalsIgnoreCase("none")) {
            msgPlayer(player, noTagSelected);
            return;
        }

        TagResetEvent tagEvent = new TagResetEvent(player, false);
        Bukkit.getPluginManager().callEvent(tagEvent);
        if (tagEvent.isCancelled()) return;

        Tag currentTag = SupremeTags.getInstance().getTagManager().getTag(menuUtil.getIdentifier());
        if (currentTag != null) {
            currentTag.removeEffects(menuUtil.getOwner());
        }

        String identifier = SupremeTags.getInstance().getConfig().getBoolean("settings.forced-tag")
                ? SupremeTags.getInstance().getConfig().getString("settings.default-tag")
                : "None";

        UserData.setActive(player, identifier);
        super.refresh();
        menuUtil.setIdentifier(identifier);

        if (SupremeTags.getInstance().getConfig().getBoolean("settings.gui-messages")) {
            msgPlayer(player, SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.reset-message").replace("%prefix%", Objects.requireNonNull(SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.prefix"))));
        }

        playConfigSound(player, "reset-tag");
    }

    protected void nextPage(InventoryClickEvent e) {
        List<Tag> visibleTags = getVisibleTags();
        if (visibleTags.size() > maxItems && ((page + 1) * maxItems) < visibleTags.size()) {
            page++;
            super.refresh();
        } else {
            e.setCancelled(true);
        }
    }

    protected void previousPage() {
        if (page != 0) {
            page--;
            super.refresh();
        }
    }

    protected ItemStack buildTagItem(Tag t, String permission) {
        Player owner = menuUtil.getOwner();
        String identifier = t.getIdentifier();
        boolean hasAccess = Utils.hasTagAccess(owner, t);
        boolean activeGlow = SupremeTags.getInstance().getConfig().getBoolean("settings.active-tag-glow");
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

        String material = hasAccess ? tagConfig.getString(tagPath + ".display-item", "NAME_TAG") : guis.getString("gui.tag-menu.global-locked-tag.display-item", "NAME_TAG");

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

        if (isActive && activeGlow) {
            tagMeta.addEnchant(Enchantment.KNOCKBACK, 1, true);
        }

        tagMeta.setDisplayName(format(displayname));
        addItemFlags(tagMeta, XItemFlag.HIDE_ATTRIBUTES, XItemFlag.HIDE_DYE, XItemFlag.HIDE_DESTROYS, XItemFlag.HIDE_ENCHANTS, XItemFlag.HIDE_UNBREAKABLE);

        List<String> lore = getFormattedLore(t, permission);
        String joinedDescription = t.getDescription().stream().map(Utils::format).collect(Collectors.joining("\n"));
        String joinedEffects;
        String effectsList;

        if (!t.getEffects().isEmpty()) {
            String formatEffectTemplate = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.effects-replace-style");

            joinedEffects = t.getEffects().keySet().stream()
                    .map(PotionEffectType::getName)
                    .map(Utils::format)
                    .map(effect -> formatEffectTemplate.replace("%effect%", effect))
                    .collect(Collectors.joining("\n"));

            effectsList = t.getEffects().keySet().stream()
                    .map(effect -> effect.getKey().getKey().toUpperCase(Locale.ROOT))
                    .collect(Collectors.joining(", "));
        } else {
            joinedEffects = format(SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get().getString("messages.no-effects"));
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

            String requirements = Utils.getTagRequirementsStatus(owner, t);
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
}
