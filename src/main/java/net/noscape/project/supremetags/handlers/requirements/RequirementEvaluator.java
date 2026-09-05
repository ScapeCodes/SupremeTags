package net.noscape.project.supremetags.handlers.requirements;

import me.clip.placeholderapi.PlaceholderAPI;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.enums.TPermissions;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.storage.UserData;
import net.noscape.project.supremetags.utils.Utils;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RequirementEvaluator {

    private RequirementEvaluator() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static RequirementResult evaluate(Player player, Tag tag) {
        if (player == null || tag == null || !tag.hasRequirements()) {
            return RequirementResult.pass();
        }

        if (canBypassRequirements(player)) {
            return RequirementResult.pass();
        }

        TagRequirements requirements = tag.getRequirements();
        if (requirements.isPersistUnlock() && UserData.hasUnlockedTag(player.getUniqueId(), tag.getIdentifier())) {
            return RequirementResult.pass();
        }

        List<String> failedMessages = new ArrayList<>();
        int passed = 0;

        for (TagRequirement requirement : requirements.getRequirements()) {
            boolean result = evaluateRequirement(player, tag, requirement);
            if (result) {
                passed++;
            } else {
                String message = requirement.getMessage();
                if (message != null && !message.isBlank()) {
                    failedMessages.add(message);
                }
            }
        }

        boolean success = requirements.getMode() == TagRequirements.Mode.ANY
                ? passed > 0
                : passed == requirements.getRequirements().size();

        if (success) {
            if (requirements.isPersistUnlock()) {
                UserData.addUnlockedTag(player, tag.getIdentifier());
            }
            return RequirementResult.pass();
        }

        return RequirementResult.fail(failedMessages);
    }

    public static String formatStatus(Player player, Tag tag) {
        return String.join("\n", formatStatusLines(player, tag));
    }

    private static List<String> formatStatusLines(Player player, Tag tag) {
        List<String> lines = new ArrayList<>();
        if (player == null || tag == null || !tag.hasRequirements()) {
            return lines;
        }

        TagRequirements requirements = tag.getRequirements();
        boolean persistedUnlock = requirements.isPersistUnlock()
                && UserData.hasUnlockedTag(player.getUniqueId(), tag.getIdentifier());
        boolean bypassed = canBypassRequirements(player);

        for (TagRequirement requirement : requirements.getRequirements()) {
            boolean passed = bypassed || persistedUnlock || evaluateRequirement(player, tag, requirement);
            lines.add(formatRequirementLine(requirement, passed));
        }

        return lines;
    }

    private static boolean canBypassRequirements(Player player) {
        return player != null && (player.isOp() || player.hasPermission(TPermissions.ADMIN));
    }

    private static String formatRequirementLine(TagRequirement requirement, boolean passed) {
        return requirement.getLoreDisplay();
    }

    private static boolean evaluateRequirement(Player player, Tag tag, TagRequirement requirement) {
        if (requirement == null) {
            return false;
        }

        return switch (requirement.getType().toLowerCase(Locale.ROOT)) {
            case "permission", "perm" -> evaluatePermission(player, requirement);
            case "placeholder", "papi" -> evaluatePlaceholder(player, requirement);
            case "economy", "balance" -> Utils.hasAmount(player, requirement.getEconomyType(), requirement.getAmount(), tag.getIdentifier());
            case "owns-tag", "owns_tag", "tag" -> evaluateOwnsTag(player, requirement);
            default -> false;
        };
    }

    private static boolean evaluatePermission(Player player, TagRequirement requirement) {
        String permission = requirement.getPermission();
        return permission != null && (permission.equalsIgnoreCase("none") || player.hasPermission(permission));
    }

    private static boolean evaluatePlaceholder(Player player, TagRequirement requirement) {
        if (!SupremeTags.getInstance().isPlaceholderAPI() || requirement.getPlaceholder() == null) {
            return false;
        }

        String output = PlaceholderAPI.setPlaceholders(player, requirement.getPlaceholder());
        return compare(output, requirement.getOperator(), requirement.getValue());
    }

    private static boolean evaluateOwnsTag(Player player, TagRequirement requirement) {
        String requiredTag = requirement.getTag();
        if (requiredTag == null || requiredTag.isBlank()) {
            return false;
        }

        String activeTag = UserData.getActive(player.getUniqueId());
        if (activeTag != null && activeTag.equalsIgnoreCase(requiredTag)) {
            return true;
        }

        if (UserData.hasUnlockedTag(player.getUniqueId(), requiredTag)) {
            return true;
        }

        Tag tag = SupremeTags.getInstance().getTagManager().getTag(requiredTag);
        return tag != null && Utils.hasBaseTagAccess(player, tag);
    }

    private static boolean compare(String left, String operator, String right) {
        if (left == null) left = "";
        if (right == null) right = "";

        String cleanOperator = operator == null ? "==" : operator.toLowerCase(Locale.ROOT);
        Double leftNumber = parseDouble(left);
        Double rightNumber = parseDouble(right);

        if (leftNumber != null && rightNumber != null) {
            return switch (cleanOperator) {
                case ">=" -> leftNumber >= rightNumber;
                case "<=" -> leftNumber <= rightNumber;
                case ">" -> leftNumber > rightNumber;
                case "<" -> leftNumber < rightNumber;
                case "!=", "not" -> !leftNumber.equals(rightNumber);
                case "==", "=", "equals" -> leftNumber.equals(rightNumber);
                default -> false;
            };
        }

        return switch (cleanOperator) {
            case "contains" -> left.toLowerCase(Locale.ROOT).contains(right.toLowerCase(Locale.ROOT));
            case "starts-with", "starts_with" -> left.toLowerCase(Locale.ROOT).startsWith(right.toLowerCase(Locale.ROOT));
            case "ends-with", "ends_with" -> left.toLowerCase(Locale.ROOT).endsWith(right.toLowerCase(Locale.ROOT));
            case "!=", "not" -> !left.equalsIgnoreCase(right);
            case "==", "=", "equals" -> left.equalsIgnoreCase(right);
            default -> false;
        };
    }

    private static Double parseDouble(String value) {
        try {
            return Double.parseDouble(value.replace(",", "").trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
