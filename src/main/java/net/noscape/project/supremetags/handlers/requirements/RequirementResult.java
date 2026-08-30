package net.noscape.project.supremetags.handlers.requirements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RequirementResult {

    private final boolean passed;
    private final List<String> failedMessages;

    private RequirementResult(boolean passed, List<String> failedMessages) {
        this.passed = passed;
        this.failedMessages = failedMessages == null ? new ArrayList<>() : new ArrayList<>(failedMessages);
    }

    public static RequirementResult pass() {
        return new RequirementResult(true, new ArrayList<>());
    }

    public static RequirementResult fail(List<String> failedMessages) {
        return new RequirementResult(false, failedMessages);
    }

    public boolean isPassed() {
        return passed;
    }

    public List<String> getFailedMessages() {
        return Collections.unmodifiableList(failedMessages);
    }

    public String getFirstMessage() {
        return failedMessages.isEmpty() ? "" : failedMessages.get(0);
    }

    public String getMessage() {
        return String.join("\n", failedMessages);
    }
}
