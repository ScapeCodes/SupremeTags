package net.noscape.project.supremetags.guis.tageditor;

import de.tr7zw.nbtapi.NBTItem;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.guis.MainMenu;
import net.noscape.project.supremetags.guis.TagMenu;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.handlers.menu.MenuUtil;
import net.noscape.project.supremetags.handlers.menu.Paged;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

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
        getTagItemsEditor();
        applyEditorLayout();
    }
}