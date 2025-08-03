package net.noscape.project.supremetags.handlers;

public class Rarity {

    private boolean enable;
    private int order;
    private String selected;
    private String unselected;
    private String displayname;

    public Rarity(boolean enable, int order, String selected, String unselected, String displayname) {
        this.enable = enable;
        this.order = order;
        this.selected = selected;
        this.unselected = unselected;
        this.displayname = displayname;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getSelected() {
        if (selected == null) {
            return "";
        }

        return selected;
    }

    public void setSelected(String selected) {
        this.selected = selected;
    }

    public String getUnselected() {
        return unselected;
    }

    public void setUnselected(String unselected) {
        this.unselected = unselected;
    }

    public String getDisplayname() {
        if (displayname == null) {
            return "";
        }

        return displayname;
    }

    public void setDisplayname(String displayname) {
        this.displayname = displayname;
    }
}