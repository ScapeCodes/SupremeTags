package net.noscape.project.supremetags.handlers.requirements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TagRequirements {

    public enum Mode {
        ALL,
        ANY;

        public static Mode from(String mode) {
            if (mode != null && mode.equalsIgnoreCase("any")) {
                return ANY;
            }
            return ALL;
        }
    }

    private final boolean enabled;
    private final boolean persistUnlock;
    private final Mode mode;
    private final List<TagRequirement> requirements;

    public TagRequirements(boolean enabled, boolean persistUnlock, Mode mode, List<TagRequirement> requirements) {
        this.enabled = enabled;
        this.persistUnlock = persistUnlock;
        this.mode = mode == null ? Mode.ALL : mode;
        this.requirements = requirements == null ? new ArrayList<>() : new ArrayList<>(requirements);
    }

    public boolean isEnabled() {
        return enabled && !requirements.isEmpty();
    }

    public boolean isConfiguredEnabled() {
        return enabled;
    }

    public boolean isPersistUnlock() {
        return persistUnlock;
    }

    public Mode getMode() {
        return mode;
    }

    public List<TagRequirement> getRequirements() {
        return Collections.unmodifiableList(requirements);
    }
}
