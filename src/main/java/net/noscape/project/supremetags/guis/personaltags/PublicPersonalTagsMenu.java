package net.noscape.project.supremetags.guis.personaltags;

import net.noscape.project.supremetags.utils.ItemData;

import com.cryptomorin.xseries.XItemFlag;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.handlers.menu.MenuUtil;
import net.noscape.project.supremetags.handlers.menu.Paged;
import net.noscape.project.supremetags.storage.user.PlayerConfig;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static net.noscape.project.supremetags.utils.Utils.*;

public class PublicPersonalTagsMenu extends Paged {

    private final FileConfiguration guis = SupremeTags.getInstance().getConfigManager().getConfig("guis.yml").get();
    private final List<PublicPersonalTag> publicTags = loadPublicTags();

    public PublicPersonalTagsMenu(MenuUtil menuUtil) {
        super(menuUtil);
    }

    @Override
    public String getMenuName() {
        String title = guis.getString("gui.personal-tags-browser.title", "&lBrowse Personal Tags &8(%page%/%max_pages%)");
        title = title.replace("%page%", String.valueOf(page + 1)).replace("%max_pages%", String.valueOf(getMaxPages()));
        return globalPlaceholders(menuUtil.getOwner(), format(title));
    }

    @Override
    public int getSlots() {
        return guis.getInt("gui.personal-tags-browser.size", 54);
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;

        ItemStack nbt = e.getCurrentItem();
        if (!ItemData.has(nbt, "name")) return;

        String name = ItemData.getString(nbt, "name");
        if (name.equalsIgnoreCase("close")) {
            player.closeInventory();
        } else if (name.equalsIgnoreCase("back")) {
            if (page > 0) {
                page--;
                open();
            } else {
                new PersonalTagsHubMenu(SupremeTags.getMenuUtil(player)).open();
            }
        } else if (name.equalsIgnoreCase("next")) {
            if (page + 1 < getMaxPages()) {
                page++;
                open();
            }
        }
    }

    @Override
    public void setMenuItems() {
        applyLayout(false, false, false, true);
        setNavigationItem("back", 48, "&f&lBack", "ARROW", List.of("&7Go back to the personal tags hub."));
        setNavigationItem("close", 49, "&c&lClose", "BARRIER", List.of("&7Close the menu."));

        if (page + 1 < getMaxPages()) {
            setNavigationItem("next", 50, "&f&lNext", "ARROW", List.of("&7Go to the next page."));
        }

        int perPage = guis.getInt("gui.personal-tags-browser.tags-per-page", 28);
        int start = page * perPage;
        int end = Math.min(start + perPage, publicTags.size());

        if (publicTags.isEmpty()) {
            String path = "gui.personal-tags-browser.items.no-tags-item";
            if (guis.getBoolean(path + ".enable", true)) {
                Material material = Material.matchMaterial(guis.getString(path + ".material", "ANVIL").toUpperCase());
                if (material == null) material = Material.ANVIL;
                inventory.setItem(guis.getInt(path + ".slot", 22), makeItem(material, guis.getString(path + ".displayname", "&cNo personal tags found!"), guis.getInt(path + ".custom-model-data", 0), guis.getStringList(path + ".lore")));
            }
            return;
        }

        List<Integer> configuredSlots = guis.getIntegerList("gui.personal-tags-browser.slots-tag.slots");
        boolean useSlots = guis.getBoolean("gui.personal-tags-browser.slots-tag.enable", false) && !configuredSlots.isEmpty();

        int slotIndex = 0;
        for (int i = start; i < end; i++) {
            PublicPersonalTag publicTag = publicTags.get(i);
            ItemStack item = new ItemStack(Material.NAME_TAG);
            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;

            meta.setDisplayName(format(guis.getString("gui.personal-tags-browser.tag-item.displayname", "&7%player% &8➜ %tag%")
                    .replace("%player%", publicTag.ownerName())
                    .replace("%identifier%", publicTag.identifier())
                    .replace("%tag%", publicTag.tag().getCurrentTag())));

            List<String> lore = new ArrayList<>();
            for (String line : guis.getStringList("gui.personal-tags-browser.tag-item.lore")) {
                lore.add(line
                        .replace("%player%", publicTag.ownerName())
                        .replace("%identifier%", publicTag.identifier())
                        .replace("%tag%", publicTag.tag().getCurrentTag()));
            }
            meta.setLore(color(lore));
            addItemFlags(meta, XItemFlag.HIDE_ATTRIBUTES, XItemFlag.HIDE_DYE, XItemFlag.HIDE_DESTROYS, XItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);

            ItemStack nbt = item;
            ItemData.setString(nbt, "name", "public-personal-tag");
            ItemData.setString(nbt, "identifier", publicTag.identifier());
            ItemData.setString(nbt, "owner", publicTag.ownerUuid().toString());

            if (useSlots) {
                if (slotIndex >= configuredSlots.size()) break;
                inventory.setItem(configuredSlots.get(slotIndex), nbt);
            } else {
                inventory.addItem(nbt);
            }
            slotIndex++;
        }
    }

    private int getMaxPages() {
        int perPage = Math.max(1, guis.getInt("gui.personal-tags-browser.tags-per-page", 28));
        return Math.max(1, (int) Math.ceil(publicTags.size() / (double) perPage));
    }

    private void setNavigationItem(String name, int defaultSlot, String defaultDisplayName, String defaultMaterial, List<String> defaultLore) {
        String path = "gui.personal-tags-browser.items." + name;
        if (!guis.getBoolean(path + ".enable", true)) return;

        String materialName = guis.getString(path + ".material", defaultMaterial);
        Material material = Material.matchMaterial(Objects.requireNonNull(materialName).toUpperCase());
        if (material == null) material = Material.ARROW;

        List<String> lore = guis.contains(path + ".lore") ? guis.getStringList(path + ".lore") : defaultLore;
        ItemStack item = makeItem(material, guis.getString(path + ".displayname", defaultDisplayName), guis.getInt(path + ".custom-model-data", 0), lore);
        ItemStack nbt = item;
        ItemData.setString(nbt, "name", name);
        inventory.setItem(guis.getInt(path + ".slot", defaultSlot), nbt);
    }

    private List<PublicPersonalTag> loadPublicTags() {
        List<PublicPersonalTag> tags = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            for (Tag tag : SupremeTags.getInstance().getPlayerManager().getPlayerTags(player.getUniqueId())) {
                tags.add(new PublicPersonalTag(player.getUniqueId(), player.getName(), tag.getIdentifier(), tag));
            }
        }

        File folder = new File(SupremeTags.getInstance().getDataFolder(), "data");
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null) return tags;

        for (File file : files) {
            String fileName = file.getName().substring(0, file.getName().length() - 4);
            UUID uuid;
            try {
                uuid = UUID.fromString(fileName);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            if (tags.stream().anyMatch(tag -> tag.ownerUuid().equals(uuid))) continue;

            FileConfiguration config = PlayerConfig.get(uuid);
            ConfigurationSection section = config.getConfigurationSection("tags");
            if (section == null) continue;

            OfflinePlayer owner = Bukkit.getOfflinePlayer(uuid);
            String ownerName = owner.getName() != null ? owner.getName() : uuid.toString();

            for (String identifier : section.getKeys(false)) {
                String tagText = config.getString("tags." + identifier + ".tag", identifier);
                String description = config.getString("tags." + identifier + ".description", "");

                List<String> tagList = new ArrayList<>();
                tagList.add(tagText);
                List<String> descriptionList = new ArrayList<>();
                if (description != null && !description.isBlank()) descriptionList.add(description);

                tags.add(new PublicPersonalTag(uuid, ownerName, identifier, new Tag(identifier, tagList, descriptionList)));
            }
        }

        return tags;
    }

    private record PublicPersonalTag(UUID ownerUuid, String ownerName, String identifier, Tag tag) {}
}
