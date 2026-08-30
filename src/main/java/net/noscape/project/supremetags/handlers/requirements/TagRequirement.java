package net.noscape.project.supremetags.handlers.requirements;

public class TagRequirement {

    private final String name;
    private final String type;
    private final String permission;
    private final String placeholder;
    private final String operator;
    private final String value;
    private final String tag;
    private final String economyType;
    private final double amount;
    private final String display;
    private final String loreDisplay;
    private final String message;

    public TagRequirement(String name, String type, String permission, String placeholder, String operator, String value, String tag, String economyType, double amount, String display, String loreDisplay, String message) {
        this.name = name;
        this.type = type;
        this.permission = permission;
        this.placeholder = placeholder;
        this.operator = operator;
        this.value = value;
        this.tag = tag;
        this.economyType = economyType;
        this.amount = amount;
        this.display = display;
        this.loreDisplay = loreDisplay;
        this.message = message;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type == null || type.isBlank() ? "placeholder" : type;
    }

    public String getPermission() {
        return permission;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public String getOperator() {
        return operator == null || operator.isBlank() ? "==" : operator;
    }

    public String getValue() {
        return value == null ? "" : value;
    }

    public String getTag() {
        return tag;
    }

    public String getEconomyType() {
        return economyType == null || economyType.isBlank() ? "VAULT" : economyType;
    }

    public double getAmount() {
        return amount;
    }

    public String getDisplay() {
        return display == null || display.isBlank() ? getMessage() : display;
    }

    public String getLoreDisplay() {
        if (loreDisplay != null && !loreDisplay.isBlank()) {
            return loreDisplay;
        }
        return display == null || display.isBlank() ? getMessage() : display;
    }

    public String getMessage() {
        return message;
    }
}
