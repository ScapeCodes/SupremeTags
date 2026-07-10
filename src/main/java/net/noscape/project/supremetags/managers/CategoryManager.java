package net.noscape.project.supremetags.managers;

import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.handlers.Tag;
import org.bukkit.configuration.ConfigurationSection;
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
        ConfigurationSection categoriesSection = getCatConfig().getConfigurationSection("categories");
        if (categoriesSection != null) {
            catorgies.addAll(categoriesSection.getKeys(false));
        }
        loadCategoriesTags();
    }

    public void loadCategoriesTags() {
        catorgiesTags.clear();

        for (String cats : getCatorgies()) {
            int value = 0;

            for (Tag tag : SupremeTags.getInstance().getTagManager().getTags().values()) {
                if (cats.trim().equalsIgnoreCase(tag.getCategory().trim())) {
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

    public void deleteCategory(String category) {
        FileConfiguration config = getCatConfig();
        config.set("categories." + category, null);
        SupremeTags.getInstance().getConfigManager().saveConfig("categories.yml");
        initCategories();
    }
}
