package net.noscape.project.supremetags.listeners;

import de.tr7zw.nbtapi.NBTItem;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.handlers.Tag;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

import static net.noscape.project.supremetags.utils.Utils.addPerm;
import static net.noscape.project.supremetags.utils.Utils.msgPlayer;

public class VoucherListener implements Listener {

    private FileConfiguration messages = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get();
    private FileConfiguration tags = SupremeTags.getInstance().getConfigManager().getConfig("tags.yml").get();

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        ItemStack item = getItemInHand(player);

        if (item == null || item.getType() == Material.AIR) return;

        NBTItem nbtItem = new NBTItem(item);
        if (nbtItem.hasKey("identifier")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = getItemInHand(player);

        if (item == null || item.getType() == Material.AIR) return;

        NBTItem nbtItem = new NBTItem(item);
        if (!nbtItem.hasKey("tags:identifier")) return;

        String identifier = nbtItem.getString("tags:identifier");

        Tag tag = SupremeTags.getInstance().getTagManager().getTag(identifier);

        if (tag == null) {
            String invalid = messages.getString("messages.invalid-tag");
            invalid = invalid.replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
            msgPlayer(player, invalid);
            return;
        }

        String already_have_tag = messages.getString("messages.already-have-tag")
                .replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));

        if (SupremeTags.getInstance().getConfig().getBoolean("settings.voucher-redeem-permission")) {
            if (!player.hasPermission("supremetags.voucher." + identifier)) {
                String invalid = messages.getString("messages.no-permission-voucher")
                        .replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")))
                        .replaceAll("%identifier%", tag.getIdentifier())
                        .replaceAll("%tag%", tag.getTag().get(0));
                msgPlayer(player, invalid);
                return;
            }
        }

        if (player.hasPermission(tag.getPermission())) {
            msgPlayer(player, already_have_tag);
            return;
        }

        addPerm(player, tag.getPermission());

        item.setAmount(item.getAmount() - 1);

        String received_voucher = messages.getString("messages.received-voucher")
                .replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")))
                .replaceAll("%identifier%", tag.getIdentifier())
                .replaceAll("%tag%", tag.getTag().get(0));

        msgPlayer(player, received_voucher);
    }

    public static ItemStack getItemInHand(Player player) {
        try {
            return player.getInventory().getItemInMainHand();
        } catch (NoSuchMethodError e) {
            return player.getItemInHand();
        }
    }
}
