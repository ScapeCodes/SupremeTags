package net.noscape.project.supremetags.guis.personaltags;

import de.tr7zw.nbtapi.NBTItem;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.api.events.TagAssignEvent;
import net.noscape.project.supremetags.api.events.TagResetEvent;
import net.noscape.project.supremetags.guis.MainMenu;
import net.noscape.project.supremetags.guis.TagMenu;
import net.noscape.project.supremetags.handlers.SetupTag;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.handlers.menu.MenuUtil;
import net.noscape.project.supremetags.handlers.menu.Paged;
import net.noscape.project.supremetags.storage.UserData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

import static net.noscape.project.supremetags.utils.Utils.*;
import static net.noscape.project.supremetags.utils.Utils.msgPlayer;

public class PersonalTagsMenu extends Paged {

    private final List<Tag> tags = SupremeTags.getInstance().getPlayerManager().getPlayerTags(menuUtil.getOwner().getUniqueId());
    private final FileConfiguration messages = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get();
    private final FileConfiguration guis = SupremeTags.getInstance().getConfigManager().getConfig("guis.yml").get();

    private int tagsOnPage = 0;

    public PersonalTagsMenu(MenuUtil menuUtil) {
        super(menuUtil);
    }

    @Override
    public String getMenuName() {
        String title = format(Objects.requireNonNull(guis.getString("gui.personal-tags.title")));
        title = globalPlaceholders(menuUtil.getOwner(), title);
        return title;
    }

    @Override
    public int getSlots() {
        return 54;
    }

    public void handleMenu(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();

        String back = this.guis.getString("gui.items.back.displayname");
        String close = this.guis.getString("gui.items.close.displayname");
        String next = this.guis.getString("gui.items.next.displayname");
        String reset = this.guis.getString("gui.items.reset.displayname");
        String createtag = this.guis.getString("gui.items.create-tag.displayname");
        String stage_one = this.messages.getString("messages.stages.stage-1").replaceAll("%prefix%", Objects.requireNonNull(this.messages.getString("messages.prefix")));

        String displayname = e.getCurrentItem().getItemMeta().getDisplayName();
        displayname = replacePlaceholders(menuUtil.getOwner(), displayname);
        displayname = displayname.replace("%identifier%", menuUtil.getIdentifier());

        NBTItem nbt = new NBTItem(e.getCurrentItem());

        if (nbt.hasTag("identifier")) {
            String identifier = nbt.getString("identifier");

            if (e.getClick() == ClickType.LEFT) {
                if (!UserData.getActive(player.getUniqueId()).equalsIgnoreCase(identifier) && identifier != null) {
                    TagAssignEvent tagevent = new TagAssignEvent(player, identifier, false);
                    Bukkit.getPluginManager().callEvent(tagevent);
                    if (tagevent.isCancelled())
                        return;

                    UserData.setActive(player, tagevent.getTag());
                    player.closeInventory();
                    open();
                    this.menuUtil.setIdentifier(tagevent.getTag());

                    if (SupremeTags.getInstance().getConfig().getBoolean("settings.gui-messages")) {
                        String select = messages.getString("messages.tag-select-message").replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
                        select = replacePlaceholders(menuUtil.getOwner(), select);
                        msgPlayer(player, select.replace("%identifier%", identifier).replaceAll("%tag%", SupremeTags.getInstance().getPlayerManager().getTag(menuUtil.getOwner().getUniqueId(), identifier).getCurrentTag()));
                    }
                }
            } else if (e.getClick() == ClickType.RIGHT) {
                this.menuUtil.setIdentifier(identifier);
                new PersonalTagEditorMenu(SupremeTags.getMenuUtilIdentifier(menuUtil.getOwner(), identifier)).open();
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
                //if (menuUtil.getIdentifier() == null || (menuUtil.getIdentifier().equalsIgnoreCase("none"))) {
                //    msgPlayer(player, no_tag_selected);
                //    return;
                //}

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

            if (name.equalsIgnoreCase("back")) {
                if (this.page != 0) {
                    this.page--;
                    open();
                } else {
                    if (SupremeTags.getInstance().getConfig().getBoolean("settings.categories")) {
                        player.closeInventory();
                        SupremeTags.getInstance().getCategoryManager().initCategories();
                        new MainMenu(SupremeTags.getMenuUtil(player)).open();
                    } else {
                        player.closeInventory();
                        new TagMenu(SupremeTags.getMenuUtil(player)).open();
                    }
                }
            }

            if (name.equalsIgnoreCase("next")) {
                if ((((this.tags.size() > 36) ? 1 : 0) & ((this.tagsOnPage >= 36) ? 1 : 0)) != 0) {
                    if (this.index + 1 < this.tags.size()) {
                        this.page++;
                        open();
                    } else {
                        e.setCancelled(true);
                    }
                } else {
                    e.setCancelled(true);
                }
            }

            if (name.equalsIgnoreCase("create-tag")) {
                boolean reachedLimit = false;
                int limit = 0;

                if (player.isOp() || player.hasPermission("supremetags.mytags.limit.*")) {
                    reachedLimit = false; // unlimited
                } else {
                    Set<String> keys = SupremeTags.getInstance().getConfig().getConfigurationSection("settings.personal-tags.limits").getKeys(false);

                    for (String str : keys) {
                        if (player.hasPermission("supremetags.mytags.limit." + str)) {
                            limit = SupremeTags.getInstance().getConfig().getInt("settings.personal-tags.limits." + str);
                            if (this.tags.size() >= limit) {
                                reachedLimit = true;
                            }
                            break; // stop at first matching permission
                        }
                    }

                    // If no matching permission found, restrict by default
                    if (limit == 0) {
                        reachedLimit = true;
                    }
                }

                if (!reachedLimit) {
                    if (!SupremeTags.getInstance().getSetupList().containsKey(player)) {
                        player.closeInventory();
                        SetupTag setup = new SetupTag(1);
                        SupremeTags.getInstance().getSetupList().put(player, setup);
                        msgPlayer(player, stage_one);
                    }
                } else {
                    String limit_reached = messages.getString("messages.ptags-limit-reached")
                            .replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")))
                            .replaceAll("%limit%", String.valueOf(limit));
                    msgPlayer(player, limit_reached);
                }
            }

        }
    }

    @Override
    public void setMenuItems() {
        applyLayout(true, false, false);

        if (tags != null && !tags.isEmpty()) {
            int maxItemsPerPage = 36;
            int startIndex = this.page * maxItemsPerPage;
            int endIndex = Math.min(startIndex + maxItemsPerPage, this.tags.size());
            this.tagsOnPage = 0;

            for (int i = startIndex; i < endIndex; i++) {
                Tag t = this.tags.get(i);
                if (t != null) {
                    ItemStack tagItem = new ItemStack(Material.NAME_TAG, 1);
                    ItemMeta tagMeta = tagItem.getItemMeta();
                    assert tagMeta != null;

                    NBTItem nbt = new NBTItem(tagItem);
                    nbt.setString("identifier", t.getIdentifier());

                    tagMeta.setDisplayName(format("&7Tag: " + t.getTag().get(0)));
                    tagMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                    try {
                        ItemFlag hideDye = ItemFlag.valueOf("HIDE_DYE");
                        tagMeta.addItemFlags(hideDye);
                    } catch (IllegalArgumentException ignored) {
                        // HIDE_DYE not available in this version — skip
                    }
                    tagMeta.addItemFlags(ItemFlag.HIDE_DESTROYS);
                    tagMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

                    ArrayList<String> lore = new ArrayList<>();

                    if (UserData.getActive(this.menuUtil.getOwner().getUniqueId()).equalsIgnoreCase(t.getIdentifier())) {
                        lore.add("&7Right-Click to edit this tag.");
                        lore.add("&7");
                        lore.add("&cYou have this tag selected!");
                    } else {
                        lore.add("&7Right-Click to edit this tag.");
                        lore.add("&7");
                        lore.add("&eLeft-Click to Assign!");
                    }

                    tagMeta.setLore(color(lore));
                    nbt.getItem().setItemMeta(tagMeta);
                    nbt.setString("identifier", t.getIdentifier());
                    this.inventory.addItem(nbt.getItem());
                    this.tagsOnPage++;
                }
            }
        } else {
            if (guis.getBoolean("gui.personal-tags.items.no-tags-item.enable")) {
                this.inventory.setItem(guis.getInt("gui.personal-tags.items.no-tags-item.slot"), makeItem(Material.valueOf(guis.getString("gui.personal-tags.items.no-tags-item.material").toUpperCase()), guis.getString("gui.personal-tags.items.no-tags-item.displayname"), guis.getInt("gui.personal-tags.items.no-tags-item.custom-model-data"), new ArrayList<>(guis.getStringList("gui.personal-tags.items.no-tags-item.lore"))));
            }
        }
    }

    public List<Tag> getTags() {
        return tags;
    }
}