package net.noscape.project.supremetags.utils;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.profiles.builder.XSkull;
import com.cryptomorin.xseries.profiles.objects.ProfileInputType;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import net.noscape.project.supremetags.SupremeTags;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ua.valeriishymchuk.simpleitemgenerator.api.SimpleItemGenerator;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static net.noscape.project.supremetags.utils.Utils.getItemWithIA;
import static net.noscape.project.supremetags.utils.Utils.getItemWithOraxen;

public class ItemResolver {

    public static ResolvedItem resolveCustomItem(Player player, String material) {
        if (material.contains("%player_name%")) {
            material = material.replace("%player_name%", player.getName());
        }

        ItemStack item;
        ItemMeta meta;

        if (material.startsWith("hdb-")) {
            int id = Integer.parseInt(material.replace("hdb-", ""));
            HeadDatabaseAPI api = new HeadDatabaseAPI();
            item = api.getItemHead(String.valueOf(id));
        } else if (material.startsWith("basehead-")) {
            String base64 = material.replace("basehead-", "");
            item = XSkull.createItem()
                    .profile(Profileable.of(ProfileInputType.BASE64, base64))
                    .apply();
        } else if (material.startsWith("skull-")) {
            String textureUrl = material.replace("skull-", "");
            item = XSkull.createItem()
                    .profile(Profileable.of(ProfileInputType.TEXTURE_URL, textureUrl))
                    .apply();
        } else if (material.toLowerCase().startsWith("head-")) {
            String identifier = material.replace("head-", "").trim();

            if (identifier.equalsIgnoreCase("%player_name%")) {
                identifier = player.getName();
            }

            Profileable profile;
            try {
                // Try to parse as UUID first
                UUID uuid = UUID.fromString(identifier);
                profile = Profileable.of(uuid);
            } catch (IllegalArgumentException ignored) {
                // Fallback to player name
                profile = Profileable.of(ProfileInputType.USERNAME, identifier);
            }

            item = XSkull.createItem()
                    .profile(profile)
                    .apply();
        } else if (material.startsWith("itemsadder-")) {
            String id = material.replace("itemsadder-", "");
            item = getItemWithIA(id);
        } else if (material.startsWith("nexo-")) {
            String id = material.replace("nexo-", "");
            item = SupremeTags.getInstance().getItemWithNexo(id);
        } else if (material.startsWith("oraxen-")) {
            String id = material.replace("oraxen-", "");
            item = getItemWithOraxen(id);
        } else if (material.startsWith("sig-")) {
            String id = material.replace("sig-", "");
            Optional<ItemStack> resultOpt = SimpleItemGenerator.get().bakeItem(id, null);
            item = resultOpt.orElseGet(() -> new ItemStack(Material.DIRT, 1));
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
