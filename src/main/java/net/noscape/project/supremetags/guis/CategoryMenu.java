package net.noscape.project.supremetags.guis;

import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.handlers.menu.MenuUtil;

public class CategoryMenu extends BaseTagsMenu {

    public CategoryMenu(MenuUtil menuUtil) {
        super(menuUtil);
    }

    @Override
    protected boolean isCategoryMenu() {
        return true;
    }

    @Override
    protected String getRawTitle() {
        String category = menuUtil.getCategory();
        if (category == null || category.isBlank()) {
            return SupremeTags.getInstance()
                    .getConfigManager()
                    .getConfig("guis.yml")
                    .get()
                    .getString("gui.tag-menu.title", "<bold>Tags <reset><dark_gray>(%page%/%max_pages%)");
        }

        return SupremeTags.getInstance()
                .getCategoryManager()
                .getCatConfig()
                .getString("categories." + category + ".title", "<bold>" + category + " <reset><dark_gray>(%page%/%max_pages%)");
    }
}
