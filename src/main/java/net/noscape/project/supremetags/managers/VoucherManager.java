package net.noscape.project.supremetags.managers;

import de.tr7zw.nbtapi.NBTItem;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.storage.UserData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Method;
import java.util.*;

import static net.noscape.project.supremetags.utils.Utils.*;

public class VoucherManager {

    private FileConfiguration bannedwords = SupremeTags.getInstance().getConfigManager().getConfig("banned-words.yml").get();
    private FileConfiguration messages = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get();
    private FileConfiguration tags = SupremeTags.getInstance().getConfigManager().getConfig("tags.yml").get();

    private final Map<UUID, Long> delayList = new HashMap<>();
    private final long cooldownMillis = 3000;

    public VoucherManager() {
        if (!SupremeTags.getInstance().isFoliaFound()) {
            // Non-Folia (Spigot/Paper) support using BukkitRunnable
            new BukkitRunnable() {
                @Override
                public void run() {
                    delayList.clear();
                }
            }.runTaskTimerAsynchronously(SupremeTags.getInstance(), 0, 20 * 3); // Run every 3 seconds
        } else {
            // Folia support using reflection to access GlobalRegionScheduler
            try {
                Object server = Bukkit.getServer();
                Method getSchedulerMethod = server.getClass().getMethod("getGlobalRegionScheduler");
                Object scheduler = getSchedulerMethod.invoke(server);
                Method runAtFixedRateMethod = scheduler.getClass().getMethod(
                        "runAtFixedRate",
                        Plugin.class, Runnable.class, long.class, long.class
                );

                Runnable task = delayList::clear;
                runAtFixedRateMethod.invoke(scheduler, SupremeTags.getInstance(), task, 0L, 60L); // Runs every 60 ticks (3 seconds)
            } catch (Exception e) {
                SupremeTags.getInstance().getLogger().warning("Folia scheduler not found: " + e.getMessage());
            }
        }

    }

    public void withdrawTag(Player player, String tag_name) {

        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        String inventory_full = messages.getString("messages.inventory-full").replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
        String withdraw_failed_selected = messages.getString("messages.withdraw-failed-selected").replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
        String vault_not_found = messages.getString("messages.vault-not-found").replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
        String invalidtag = messages.getString("messages.invalid-tag").replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
        String not_owned_tag = messages.getString("messages.not-owned-tag").replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
        String not_withdrawable = messages.getString("messages.tag-not-withdrawable").replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
        String tag_withdrawn = messages.getString("messages.tag-withdrawn").replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
        String noperm = messages.getString("messages.no-permission").replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));

        String voucher_spam = messages.getString("messages.voucher-spam-warning").replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));

        if (!player.hasPermission("supremetags.withdraw")) {
            msgPlayer(player, noperm);
            return;
        }

        // Check if the player is still within the cooldown period
        if (!SupremeTags.getInstance().isFoliaFound()) {
            if (delayList.containsKey(playerId)) {
                long lastExecutionTime = delayList.get(playerId);
                if (currentTime - lastExecutionTime < cooldownMillis) {
                    msgPlayer(player, voucher_spam);
                    return;
                }
            }
        }

        if (isInventoryFull(player)) {
            msgPlayer(player, inventory_full);
            return;
        }

        if (UserData.getActive(player.getUniqueId()).equalsIgnoreCase(tag_name)) {
            msgPlayer(player, withdraw_failed_selected);
            return;
        }

        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            msgPlayer(player, vault_not_found);
            return;
        }

        Tag t = SupremeTags.getInstance().getTagManager().getTag(tag_name);

        if (t == null) {
            msgPlayer(player, invalidtag);
            return;
        }

        if (!player.hasPermission(t.getPermission())) {
            msgPlayer(player, not_owned_tag);
            return;
        }

        if (!t.isWithdrawable()) {
            msgPlayer(player, not_withdrawable);
            return;
        }

        if (!SupremeTags.getInstance().isFoliaFound()) {
            delayList.put(playerId, currentTime);
        }

        /*
         * REMOVE THE PERMISSION FROM THE PLAYER.
         */
        removePerm(player, t.getPermission());

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " permission set " + t.getPermission() + " false");

        giveVoucher(player, t.getIdentifier());

        msgPlayer(player, tag_withdrawn.replaceAll("%identifier%", t.getIdentifier()));
    }

    public boolean isInventoryFull(Player player) {
        PlayerInventory inventory = player.getInventory();
        int firstEmptySlot = inventory.firstEmpty();
        return firstEmptySlot == -1;
    }

    public void giveVoucher(Player player, String name) {
        Tag t = SupremeTags.getInstance().getTagManager().getTag(name);

        String invalidtag = messages.getString("messages.invalid-tag")
                .replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));

        if (t == null) {
            msgPlayer(player, invalidtag);
            return;
        }

        ItemStack tag;
        String displayname;
        List<String> lore;
        boolean glow;
        int custom_model_data;

        if (!tags.isConfigurationSection("tags." + name + ".voucher-item")) {
            tag = new ItemStack(Material.NAME_TAG, 1);
            lore = new ArrayList<>();
            lore.add("&7Right-Click to assign tag: &f" + t.getIdentifier());
            displayname = "&7Tag: " + t.getTag().get(0);
            glow = false;
            custom_model_data = 0;
        } else {
            tag = new ItemStack(Material.valueOf(tags.getString("tags." + name + ".voucher-item.material")), 1);
            lore = new ArrayList<>(tags.getStringList("tags." + name + ".voucher-item.lore"));
            displayname = tags.getString("tags." + name + ".voucher-item.displayname");
            glow = tags.getBoolean("tags." + name + ".voucher-item.glow");
            custom_model_data = tags.getInt("tags." + name + ".custom-model-data");
        }

        displayname = displayname.replace("%tag%", t.getTag().get(0));
        displayname = displayname.replace("%identifier%", t.getIdentifier());
        displayname = replacePlaceholders(player, displayname);

        ItemMeta tagMeta = tag.getItemMeta();
        tagMeta.setDisplayName(format(displayname));
        if (custom_model_data > 0) {
            tagMeta.setCustomModelData(custom_model_data);
        }
        tagMeta.setLore(color(lore));

        if (glow) {
            tagMeta.addEnchant(Enchantment.KNOCKBACK, 1, true);
        }

        tagMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        tag.setItemMeta(tagMeta);

        // Apply NBT Data using NBT API
        NBTItem nbtItem = new NBTItem(tag);
        nbtItem.setString("tags:identifier", t.getIdentifier()); // Store the tag identifier

        player.getInventory().addItem(nbtItem.getItem()); // Give the modified item to the player
    }

    public void remove(Player player) {
        delayList.remove(player.getUniqueId());
    }
}