package net.noscape.project.supremetags.guis;

import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.handlers.menu.MenuUtil;

import java.util.Objects;

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
        return Objects.requireNonNull(SupremeTags.getInstance()
                .getCategoryManager()
                .getCatConfig()
                .getString("categories." + menuUtil.getCategory() + ".title"));
    }
}
