package net.noscape.project.supremetags.guis;

import net.noscape.project.supremetags.handlers.menu.MenuUtil;

import java.util.Objects;

public class FavouritesMenu extends BaseTagsMenu {

    public FavouritesMenu(MenuUtil menuUtil) {
        super(menuUtil);
    }

    @Override
    protected boolean isCategoryMenu() {
        return false;
    }

    @Override
    protected boolean isFavouritesMenu() {
        return true;
    }

    @Override
    protected String getRawTitle() {
        return Objects.requireNonNull(guis.getString("gui.favourites-menu.title"));
    }
}
