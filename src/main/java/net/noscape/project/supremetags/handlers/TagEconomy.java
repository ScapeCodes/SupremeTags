package net.noscape.project.supremetags.handlers;

public class TagEconomy {

    private String type;
    private double amount;
    private boolean enabled;

    // custom economy
    private String take_cmd;
    private String condition;

    public TagEconomy(String type, double amount, boolean enabled) {
        this.type = type;
        this.amount = amount;
        this.enabled = enabled;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getAmount() {
        return amount;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setAmount(double cost) {
        this.amount = cost;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getTake_cmd() {
        return take_cmd;
    }

    public void setTake_cmd(String take_cmd) {
        this.take_cmd = take_cmd;
    }
}
