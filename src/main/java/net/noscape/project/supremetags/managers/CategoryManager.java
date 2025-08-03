package net.noscape.project.supremetags.managers;

import net.noscape.project.supremetags.SupremeTags;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class CategoryManager {

    private final List<String> catorgies = new ArrayList<>();
    private final Map<String, Integer> catorgiesTags = new HashMap<>();

    public CategoryManager() {
        initCategories();
    }

    public void initCategories() {
        catorgies.clear();
        catorgies.addAll(Objects.requireNonNull(getCatConfig().getConfigurationSection("categories")).getKeys(false));
        loadCategoriesTags();
    }

    public void loadCategoriesTags() {
        catorgiesTags.clear();

        for (String cats : getCatorgies()) {
            int value = 0;
            for (String tags : Objects.requireNonNull(SupremeTags.getInstance().getTagManager().getTagConfig().getConfigurationSection("tags")).getKeys(false)) {
                String cat = SupremeTags.getInstance().getTagManager().getTagConfig().getString("tags." + tags + ".category");
                if (cats.equalsIgnoreCase(cat)) {
                    value++;
                }
            }
            catorgiesTags.put(cats, value);
        }
    }

    public boolean isCategory(String category) {
       for (String cats : catorgies) {
           if (cats.equalsIgnoreCase(category)) {
               return true;
           }
       }

       return false;
    }

    public boolean isCategoryNearName(String category) {
        for (String cats : catorgies) {
            if (cats.contains(category) || cats.equalsIgnoreCase(category)) {
                return true;
            }
        }

        return false;
    }

    public List<String> getCatorgies() {
        return catorgies;
    }

    public Map<String, Integer> getCatorgiesTags() {
        return catorgiesTags;
    }

    public FileConfiguration getCatConfig() {
        return SupremeTags.getInstance().getConfigManager().getConfig("categories.yml").get();
    }
}
