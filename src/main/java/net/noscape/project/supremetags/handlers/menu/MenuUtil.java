package net.noscape.project.supremetags.handlers.menu;

import org.bukkit.entity.*;

public class MenuUtil {

    private Player owner;
    private String identifier;
    private String category;
    private String searchResult;

    /*
     * - all
     * - players
     * - category:name
     */
    private String filter;

    /*
     * - rarities
     */
    private String sort;

    public MenuUtil(Player owner, String identifier, String category) {
        this.owner = owner;
        this.identifier = identifier;
        this.category = category;
    }

    public MenuUtil(Player owner, String identifier) {
        this.owner = owner;
        this.identifier = identifier;
    }

    public Player getOwner() {
        return owner;
    }

    public void setOwner(Player owner) {
        this.owner = owner;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;

        // WIP for 2.0.12?
        //addTagDisplayName(owner, identifier);
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSearchResult() {
        return searchResult;
    }

    public void setSearchResult(String searchResult) {
        this.searchResult = searchResult;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }
}