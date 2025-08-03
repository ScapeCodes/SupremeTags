package net.noscape.project.supremetags.utils;

import com.ssomar.executableblocks.api.ExecutableBlocksAPI;
import com.ssomar.score.api.executableblocks.config.ExecutableBlockInterface;
import com.ssomar.score.api.executableitems.ExecutableItemsAPI;
import com.ssomar.score.api.executableitems.config.ExecutableItemInterface;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import net.noscape.project.supremetags.SupremeTags;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ua.valeriishymchuk.simpleitemgenerator.api.SimpleItemGenerator;

import java.util.Optional;

import static net.noscape.project.supremetags.utils.Utils.getItemWithIA;
import static net.noscape.project.supremetags.utils.Utils.getItemWithOraxen;

public class ItemResolver {

    public static ResolvedItem resolveCustomItem(String material) {
        ItemStack item = null;
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
        } else if (material.contains("executableitems-")) {
            String id = material.replace("executableitems-", "");
            Optional<ExecutableItemInterface> eiOpt = ExecutableItemsAPI.getExecutableItemsManager().getExecutableItem(id);
            if(eiOpt.isPresent()) item = eiOpt.get().buildItem(1, Optional.empty());
        } else if (material.contains("executableblocks-")) {
            String id = material.replace("executableblocks-", "");
            Optional<ExecutableBlockInterface> ebOpt = ExecutableBlocksAPI.getExecutableBlocksManager().getExecutableBlock(id);
            if(ebOpt.isPresent()) item = ebOpt.get().buildItem(1, Optional.empty());
        } else if (material.contains(":")) {
            String[] parts = material.split(":");
            Material mat = Material.valueOf(parts[0].toUpperCase());
            short data = Short.parseShort(parts[1]);
            item = new ItemStack(mat, 1, data);
        } else {
            item = new ItemStack(Material.valueOf(material.toUpperCase()), 1);
        }

        meta = item.getItemMeta();
        return new ResolvedItem(item, meta);
    }

    public static record ResolvedItem(ItemStack item, ItemMeta meta) {}
}