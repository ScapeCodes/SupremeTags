package net.noscape.project.supremetags.managers;

import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.handlers.Rarity;

import java.util.*;

public class RarityManager {

    private final Map<String, Rarity> rarityMap = new LinkedHashMap<>();

    public RarityManager() {
        loadRarities();
    }

    public void loadRarities() {
        int count = 0;

        // Temporary list to sort rarities before putting into the map
        List<Map.Entry<String, Rarity>> sortedRarities = new ArrayList<>();

        for (String name : SupremeTags.getInstance().getConfigManager().getConfig("rarities.yml").get().getConfigurationSection("rarities").getKeys(false)) {
            boolean enabled = SupremeTags.getInstance().getConfigManager().getConfig("rarities.yml").get().getBoolean("rarities." + name + ".enable");
            int order = SupremeTags.getInstance().getConfigManager().getConfig("rarities.yml").get().getInt("rarities." + name + ".order");
            String selected = SupremeTags.getInstance().getConfigManager().getConfig("rarities.yml").get().getString("rarities." + name + ".filter-labels.selected");
            String unselected = SupremeTags.getInstance().getConfigManager().getConfig("rarities.yml").get().getString("rarities." + name + ".filter-labels.unselected");
            String displayname = SupremeTags.getInstance().getConfigManager().getConfig("rarities.yml").get().getString("rarities." + name + ".displayname");

            if (enabled) {
                Rarity r = new Rarity(true, order, selected, unselected, displayname);
                sortedRarities.add(new AbstractMap.SimpleEntry<>(name, r));
                count++;
            }
        }

        // Sort the list by the order value
        sortedRarities.sort(Comparator.comparingInt(entry -> entry.getValue().getOrder()));

        // Clear the existing map and insert in sorted order
        rarityMap.clear();
        for (Map.Entry<String, Rarity> entry : sortedRarities) {
            rarityMap.put(entry.getKey(), entry.getValue());
        }

        SupremeTags.getInstance().getLogger().info("[Tag] " + count + " rarities registered!");
    }

    public void unloadRarities() {
        rarityMap.clear();
    }

    public boolean isValid(String rarity_name) {
        return rarityMap.containsKey(rarity_name);
    }

    public Rarity getRarity(String rarity_name) {
        if (!isValid(rarity_name)) return null;
        return rarityMap.get(rarity_name);
    }

    public Map<String, Rarity> getRarityMap() {
        return rarityMap;
    }
}
