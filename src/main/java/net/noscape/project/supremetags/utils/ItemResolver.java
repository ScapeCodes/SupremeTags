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

import java.util.Optional;

import static net.noscape.project.supremetags.utils.Utils.getItemWithIA;
import static net.noscape.project.supremetags.utils.Utils.getItemWithOraxen;

public class ItemResolver {

    public static ResolvedItem resolveCustomItem(Player player, String material) {

        if (material == null || material.isEmpty()) {
            return new ResolvedItem(new ItemStack(Material.STONE), null);
        }

        if (player != null && material.contains("%player_name%")) {
            material = material.replace("%player_name%", player.getName());
        }

        ItemStack item;

        try {
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

                String url = material.replace("skull-", "");
                item = XSkull.createItem()
                        .profile(Profileable.of(ProfileInputType.TEXTURE_URL, url))
                        .apply();
            } else if (material.toLowerCase().startsWith("head-")) {

                String name = material.replace("head-", "").trim();

                if (name.equalsIgnoreCase("%player_name%") && player != null) {
                    name = player.getName();
                }

                item = getSkullWithSkinRestorer(name, player);
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
                Optional<ItemStack> result = SimpleItemGenerator.get().bakeItem(id, null);
                item = result.orElseGet(() -> new ItemStack(Material.DIRT));
            } else if (material.contains(":")) {

                String[] parts = material.split(":");

                Material mat;
                try {
                    mat = Material.valueOf(parts[0].toUpperCase());
                } catch (Exception e) {
                    mat = Material.STONE;
                }

                item = new ItemStack(mat);
            } else {

                item = XMaterial.matchXMaterial(material)
                        .map(XMaterial::parseItem)
                        .orElseGet(() -> new ItemStack(Material.STONE));
            }

        } catch (Exception e) {

            SupremeTags.getInstance().getLogger().warning(
                    "Item resolve failed: " + material + " -> " + e.getMessage()
            );

            item = new ItemStack(Material.STONE);
        }

        ItemMeta meta = item.getItemMeta();
        return new ResolvedItem(item, meta);
    }

    private static ItemStack getSkullWithSkinRestorer(String name, Player viewer) {

        try {
            Class<?> apiClass = Class.forName("net.skinsrestorer.api.SkinsRestorerAPI");
            Object api = apiClass.getMethod("getApi").invoke(null);
            Object skinData = apiClass.getMethod("getSkinData", String.class)
                    .invoke(api, name);

            if (skinData != null) {
                Object property = skinData.getClass()
                        .getMethod("getProperty")
                        .invoke(skinData);

                if (property != null) {

                    String value = (String) property.getClass()
                            .getMethod("getValue")
                            .invoke(property);

                    return XSkull.createItem()
                            .profile(Profileable.of(ProfileInputType.BASE64, value))
                            .apply();
                }
            }

        } catch (Throwable ignored) {
            // SkinRestorer not installed or API changed → ignore safely
        }

        try {
            return XSkull.createItem()
                    .profile(Profileable.of(ProfileInputType.USERNAME, name))
                    .apply();
        } catch (Throwable ignored) {
            return new ItemStack(Material.PLAYER_HEAD);
        }
    }

    public static record ResolvedItem(ItemStack item, ItemMeta meta) {}
}