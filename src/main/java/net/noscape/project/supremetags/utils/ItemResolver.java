package net.noscape.project.supremetags.utils;

import com.cryptomorin.xseries.XMaterial;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import net.noscape.project.supremetags.SupremeTags;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ua.valeriishymchuk.simpleitemgenerator.api.SimpleItemGenerator;

import java.util.Objects;
import java.util.Optional;

import static net.noscape.project.supremetags.utils.Utils.getItemWithIA;
import static net.noscape.project.supremetags.utils.Utils.getItemWithOraxen;

public class ItemResolver {

    public static ResolvedItem resolveCustomItem(Player player, String material) {
        // Replace placeholder
        if (material.contains("%player_name%")) {
            material = material.replace("%player_name%", player.getName());
        }

        ItemStack item;
        ItemMeta meta;

        if (material.contains("hdb-")) {
            int id = Integer.parseInt(material.replace("hdb-", ""));
            HeadDatabaseAPI api = new HeadDatabaseAPI();
            item = api.getItemHead(String.valueOf(id));
        } else if (material.contains("basehead-")) {
            String id = material.replace("basehead-", "");
            item = SkullUtil.getSkullByBase64EncodedTextureUrl(SupremeTags.getInstance(), id);
        } else if (material.contains("itemsadder-")) {
            String id = material.replace("itemsadder-", "");
            item = getItemWithIA(id);
        } else if (material.contains("nexo-")) {
            String id = material.replace("nexo-", "");
            item = SupremeTags.getInstance().getItemWithNexo(id);
        } else if (material.contains("oraxen-")) {
            String id = material.replace("oraxen-", "");
            item = getItemWithOraxen(id);
        } else if (material.contains("sig-")) {
            String id = material.replace("sig-", "");
            Optional<ItemStack> resultOpt = SimpleItemGenerator.get().bakeItem(id, null);

            if (!SimpleItemGenerator.get().hasKey(id)) {
                item = new ItemStack(Material.DIRT, 1);
            } else {
                item = resultOpt.orElseGet(() -> new ItemStack(Material.DIRT, 1));
            }
        } else if (material.toLowerCase().startsWith("head-")) {
            String playerName = material.replace("head-", "");

            if (playerName.equalsIgnoreCase("%player_name%")) {
                playerName = player.getName();
            }

            item = new ItemStack(Material.PLAYER_HEAD, 1);
            ItemMeta headMeta = item.getItemMeta();
            if (headMeta instanceof org.bukkit.inventory.meta.SkullMeta skullMeta) {
                skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(playerName));
                item.setItemMeta(skullMeta);
            }
        } else if (material.contains(":")) {
            String[] parts = material.split(":");
            Material mat = Material.valueOf(parts[0].toUpperCase());
            short data = Short.parseShort(parts[1]);
            item = new ItemStack(mat, 1, data);
        } else {
            item = new ItemStack(Objects.requireNonNull(XMaterial.matchXMaterial(material).get().get()), 1);
        }

        meta = item.getItemMeta();
        return new ResolvedItem(item, meta);
    }

    public static record ResolvedItem(ItemStack item, ItemMeta meta) {}
}