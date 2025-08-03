package net.noscape.project.supremetags.handlers;

import net.noscape.project.supremetags.SupremeTags;

import java.util.List;

public class Variant {

    private String identifier;
    private String parent_tag_identifier;
    private List<String> tag;
    private String permission;
    private List<String> description;
    private String rarity;

    public Variant(String identifier, String parentTagIdentifier, List<String> tag, String permission, List<String> description, String rarity) {
        this.identifier = identifier;
        parent_tag_identifier = parentTagIdentifier;
        this.tag = tag;
        this.permission = permission;
        this.description = description;
        this.rarity = rarity;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public List<String> getTag() {
        return tag;
    }

    public String getParentTagIdentifier() {
        return parent_tag_identifier;
    }

    public void setParentTagIdentifier(String parent_tag_identifier) {
        this.parent_tag_identifier = parent_tag_identifier;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public Tag getSisterTag() {
        return SupremeTags.getInstance().getTagManager().getTag(parent_tag_identifier);
    }

    public List<String> getDescription() {
        return description;
    }

    public void setDescription(List<String> description) {
        this.description = description;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }

    public String getRarity() {
        return rarity;
    }
}