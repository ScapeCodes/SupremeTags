package net.noscape.project.supremetags.guis.search;

import net.noscape.project.supremetags.guis.BaseTagsMenu;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.handlers.menu.MenuUtil;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class SearchResultMenu extends BaseTagsMenu {

    public SearchResultMenu(MenuUtil menuUtil, String searchResult) {
        super(menuUtil);
        menuUtil.setSearchResult(searchResult);
    }

    @Override
    protected boolean isCategoryMenu() {
        return false;
    }

    @Override
    protected String getRawTitle() {
        return "Search Result: " + menuUtil.getSearchResult();
    }

    @Override
    protected List<Tag> getVisibleTags() {
        List<Tag> visibleTags = super.getVisibleTags();
        String searchTerm = menuUtil.getSearchResult();
        if (searchTerm == null || searchTerm.isBlank()) {
            return visibleTags;
        }

        String normalizedSearch = searchTerm.toLowerCase(Locale.ROOT);
        return visibleTags.stream()
                .filter(tag -> tag.getIdentifier().toLowerCase(Locale.ROOT).contains(normalizedSearch)
                        || tag.getCategory().toLowerCase(Locale.ROOT).contains(normalizedSearch))
                .collect(Collectors.toList());
    }
}
