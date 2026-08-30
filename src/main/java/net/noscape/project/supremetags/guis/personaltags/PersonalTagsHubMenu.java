package net.noscape.project.supremetags.guis.personaltags;

import net.noscape.project.supremetags.utils.ItemData;

import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.guis.MainMenu;
import net.noscape.project.supremetags.guis.TagMenu;
import net.noscape.project.supremetags.handlers.menu.Menu;
import net.noscape.project.supremetags.handlers.menu.MenuUtil;
import net.noscape.project.supremetags.storage.UserData;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.noscape.project.supremetags.utils.Utils.format;
import static net.noscape.project.supremetags.utils.Utils.globalPlaceholders;
import static net.noscape.project.supremetags.utils.Utils.msgPlayer;

public class PersonalTagsHubMenu extends Menu {

    private final FileConfiguration guis = SupremeTags.getInstance().getConfigManager().getConfig("guis.yml").get();
    private final FileConfiguration messages = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get();

    public PersonalTagsHubMenu(MenuUtil menuUtil) {
        super(menuUtil);
    }

    @Override
    public String getMenuName() {
        String title = guis.getString("gui.personal-tags-hub.title", "&lPersonal Tags");
        return globalPlaceholders(menuUtil.getOwner(), format(title));
    }

    @Override
    public int getSlots() {
        return guis.getInt("gui.personal-tags-hub.size", 27);
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();

        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;

        ItemStack nbt = e.getCurrentItem();
        if (!ItemData.has(nbt, "name")) return;

        String name = ItemData.getString(nbt, "name");

        if (name.equalsIgnoreCase("your-tags")) {
            new PersonalTagsMenu(SupremeTags.getMenuUtil(player)).open();
        } else if (name.equalsIgnoreCase("browse-tags")) {
            new PublicPersonalTagsMenu(SupremeTags.getMenuUtil(player)).open();
        } else if (name.equalsIgnoreCase("credits")) {
            msgPlayer(player, messages.getString("messages.tag-credits-balance", "%prefix% &7You have &e%credits% Tag Credits&7.")
                    .replace("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix", "")))
                    .replace("%credits%", String.valueOf(UserData.getDisplayTagCredits(player.getUniqueId()))));
        } else if (name.equalsIgnoreCase("close")) {
            player.closeInventory();
        } else if (name.equalsIgnoreCase("back")) {
            if (SupremeTags.getInstance().getConfig().getBoolean("settings.categories")) {
                new MainMenu(SupremeTags.getMenuUtil(player)).open();
            } else {
                new TagMenu(SupremeTags.getMenuUtil(player)).open();
            }
        }
    }

    @Override
    public void setMenuItems() {
        setHubItem("your-tags");
        setHubItem("browse-tags");
        setHubItem("credits");

        if (guis.getBoolean("gui.personal-tags-hub.items.close.enable", true)) {
            setHubItem("close");
        }

        if (guis.getBoolean("gui.personal-tags-hub.items.back.enable", true)) {
            setHubItem("back");
        }
    }

    private void setHubItem(String key) {
        String path = "gui.personal-tags-hub.items." + key;
        if (!guis.getBoolean(path + ".enable", true)) return;

        String materialName = guis.getString(path + ".material", "BOOK");
        Material material = Material.matchMaterial(Objects.requireNonNull(materialName).toUpperCase());
        if (material == null) material = Material.BOOK;

        String displayName = replaceCreditPlaceholders(guis.getString(path + ".displayname", "&f" + key));
        List<String> lore = new ArrayList<>();
        for (String line : guis.getStringList(path + ".lore")) {
            lore.add(replaceCreditPlaceholders(line));
        }

        ItemStack item = makeItem(material, displayName, guis.getInt(path + ".custom-model-data", 0), lore);
        ItemStack nbt = item;
        ItemData.setString(nbt, "name", key);
        inventory.setItem(guis.getInt(path + ".slot", 0), nbt);
    }

    private String replaceCreditPlaceholders(String input) {
        if (input == null) return "";
        return input
                .replace("%tag_credits%", String.valueOf(UserData.getDisplayTagCredits(menuUtil.getOwner().getUniqueId())))
                .replace("%tag_credits_creation_cost%", String.valueOf(SupremeTags.getInstance().getConfig().getLong("settings.personal-tags.credits.creation-cost", 0L)));
    }
}
