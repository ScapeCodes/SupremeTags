package net.noscape.project.supremetags.guis.personaltags;

import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.enums.EditingType;
import net.noscape.project.supremetags.handlers.Editor;
import net.noscape.project.supremetags.handlers.Tag;
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

import static net.noscape.project.supremetags.utils.Utils.*;
import static net.noscape.project.supremetags.utils.Utils.msgPlayer;

public class PersonalTagEditorMenu extends Menu {

    private FileConfiguration guis = SupremeTags.getInstance().getConfigManager().getConfig("guis.yml").get();
    private FileConfiguration messages = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get();

    public PersonalTagEditorMenu(MenuUtil menuUtil) {
        super(menuUtil);
    }

    @Override
    public String getMenuName() {
        String title = format(Objects.requireNonNull(guis.getString("gui.personal-tags-editor.title")));
        title = title.replaceAll("%identifier%", menuUtil.getIdentifier());
        title = globalPlaceholders(menuUtil.getOwner(), title);
        return title;
    }

    @Override
    public int getSlots() {
        if (guis.isConfigurationSection("gui.personal-tags.size"))
            return guis.getInt("gui.personal-tags.size"); // Added 2.1.6-d12

        return 27;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        ItemStack i = e.getCurrentItem();

        if (i == null) return;
        if (i.getItemMeta() == null) return;
        if (SupremeTags.getInstance().getEditorList().containsKey(player)) return;

        if (e.getSlot() == 11) {
            Editor editor = new Editor(menuUtil.getIdentifier(), EditingType.CHANGING_TAG, true);
            SupremeTags.getInstance().getEditorList().put(player, editor);
            player.closeInventory();

            String newtag = messages.getString("messages.ptags-setting-new-tag").replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
            msgPlayer(player, newtag);
        } else if (e.getSlot() == 13) {
            Editor editor = new Editor(menuUtil.getIdentifier(), EditingType.CHANGING_DESCRIPTION, true);
            SupremeTags.getInstance().getEditorList().put(player, editor);
            player.closeInventory();

            String newdesc = messages.getString("messages.ptags-setting-new-description").replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
            msgPlayer(player, newdesc);
        } else if (e.getSlot() == 15) {
            if (UserData.getActive(menuUtil.getOwner().getUniqueId()).equalsIgnoreCase(menuUtil.getIdentifier())) {
                String unselect = messages.getString("messages.ptags-unselect-warning").replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
                msgPlayer(player, unselect);
                e.setCancelled(true);
                return;
            }

            SupremeTags.getInstance().getPlayerManager().delete(player, menuUtil.getIdentifier());
            player.closeInventory();
            String deleted = messages.getString("messages.ptags-tag-deleted").replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
            msgPlayer(player, deleted);
        } else {
            e.setCancelled(true);
        }
    }

    @Override
    public void setMenuItems() {
        if (menuUtil.getIdentifier() != null) {
            Tag t = SupremeTags.getInstance().getPlayerManager().getTag(menuUtil.getOwner().getUniqueId(), menuUtil.getIdentifier());

            List<String> lore = new ArrayList<>();

            lore.add("&7Identifier: &6" + t.getIdentifier());
            lore.add("&7Description:");
            if (t.getDescription().isEmpty()) {
                lore.add("&6> ");
            } else {
                lore.add("&6> " + t.getDescription());
            }

            String displayname = format("&7Tag: " + t.getTag().get(0));

            getInventory().setItem(4, makeItem(Material.BOOK, format(displayname), lore));

            String c_tag_title = guis.getString("gui.tag-editor-menu.editor-items.change-tag");
            String c_description_title = guis.getString("gui.tag-editor-menu.editor-items.change-description");
            String d_tag_title = guis.getString("gui.tag-editor-menu.editor-items.delete-tag");

            List<String> c_tag = new ArrayList<>();
            c_tag.add("&7Current: &6" + t.getTag().get(0));
            getInventory().setItem(11, makeItem(Material.NAME_TAG, format(c_tag_title), c_tag));

            List<String> c_desc = new ArrayList<>();
            c_desc.add("&7Current: &6" + t.getDescription());
            getInventory().setItem(13, makeItem(Material.OAK_SIGN, format(c_description_title), c_desc));

            List<String> c_delete = new ArrayList<>();
            c_delete.add("&7This cannot be undone!");
            getInventory().setItem(15, makeItem(Material.RED_WOOL, format(d_tag_title), c_delete));
        }
        fillEmpty();
    }
}
