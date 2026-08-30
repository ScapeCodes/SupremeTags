package net.noscape.project.supremetags.guis;

import net.noscape.project.supremetags.handlers.menu.MenuUtil;

import java.util.Objects;

public class TagMenu extends BaseTagsMenu {

    public TagMenu(MenuUtil menuUtil) {
        super(menuUtil);
    }

    @Override
    protected boolean isCategoryMenu() {
        return false;
    }

    @Override
    protected String getRawTitle() {
        return Objects.requireNonNull(guis.getString("gui.tag-menu.title"));
    }
}
