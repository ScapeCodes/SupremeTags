package net.noscape.project.supremetags.editorweb;

import net.noscape.project.supremetags.SupremeTags;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;
import com.google.gson.JsonElement;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class TagEditorValidationService {

    private static final Pattern IDENTIFIER = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");
    private static final Set<String> REQUIREMENT_MODES = Set.of("all", "any");
    private static final Set<String> REQUIREMENT_OPERATORS = Set.of(">=", "<=", ">", "<", "==", "=", "equals", "!=", "not", "contains", "starts-with", "starts_with", "ends-with", "ends_with");

    private final SupremeTags plugin;

    public TagEditorValidationService(SupremeTags plugin) {
        this.plugin = plugin;
    }

    public TagEditorValidationResult validate(EditorPayload payload) {
        TagEditorValidationResult result = new TagEditorValidationResult();
        if (payload == null) {
            result.error("Payload is empty or invalid JSON.");
            return result;
        }
        if (payload.schemaVersion != 1) {
            result.error("Unsupported editor payload schema version: " + payload.schemaVersion);
            return result;
        }
        if (payload.data == null || payload.data.tags == null) {
            result.error("Payload is missing data.tags.");
            return result;
        }

        Set<String> identifiers = new HashSet<>();
        Set<String> variantIdentifiers = new HashSet<>();

        for (EditorPayload.TagDto tag : payload.data.tags) {
            validateTag(tag, identifiers, variantIdentifiers, result);
        }

        return result;
    }

    private void validateTag(EditorPayload.TagDto tag, Set<String> identifiers, Set<String> variantIdentifiers, TagEditorValidationResult result) {
        if (tag == null) {
            result.error("A tag entry is null.");
            return;
        }

        if (!isIdentifier(tag.identifier)) {
            result.error("Invalid tag identifier: " + tag.identifier);
            return;
        }
        if (!identifiers.add(tag.identifier.toLowerCase(Locale.ROOT))) {
            result.error("Duplicate tag identifier: " + tag.identifier);
        }
        if (tag.tag == null || tag.tag.isEmpty()) {
            result.error("Tag '" + tag.identifier + "' must have at least one display frame.");
        }
        if (tag.permission == null || tag.permission.isBlank()) {
            result.error("Tag '" + tag.identifier + "' is missing a permission.");
        }
        if (tag.category == null || !plugin.getCategoryManager().isCategory(tag.category)) {
            result.error("Tag '" + tag.identifier + "' references an unknown category: " + tag.category);
        }
        if (tag.rarity == null || !plugin.getRarityManager().isValid(tag.rarity)) {
            result.error("Tag '" + tag.identifier + "' references an unknown rarity: " + tag.rarity);
        }
        if (tag.order < 0) {
            result.error("Tag '" + tag.identifier + "' has a negative order.");
        }
        if (tag.customModelData < 0) {
            result.error("Tag '" + tag.identifier + "' has negative custom model data.");
        }
        tag.nameWrapperFormat = defaultString(tag.nameWrapperFormat, "%player_name%");
        if (tag.nameWrapperEnabled && !tag.nameWrapperFormat.contains("%player_name%")
                && !tag.nameWrapperFormat.contains("%player%")
                && !tag.nameWrapperFormat.contains("{player_name}")
                && !tag.nameWrapperFormat.contains("{player}")) {
            result.warning("Tag '" + tag.identifier + "' has an enabled name wrapper without a player placeholder.");
        }
        validateMaterial(tag.identifier, "display item", tag.displayItem, result);
        if (tag.voucher != null) {
            validateMaterial(tag.identifier, "voucher material", tag.voucher.material, result);
            if (tag.voucher.customModelData < 0) {
                result.error("Tag '" + tag.identifier + "' has negative voucher custom model data.");
            }
        }
        if (tag.effects != null) {
            for (String effect : tag.effects) {
                validateEffect(tag.identifier, effect, result);
            }
        }
        if (tag.variants != null) {
            for (EditorPayload.VariantDto variant : tag.variants) {
                validateVariant(tag.identifier, variant, variantIdentifiers, result);
            }
        }
        if (tag.requirements != null) {
            validateRequirements(tag.identifier, tag.requirements, result);
        }
    }

    private void validateVariant(String parent, EditorPayload.VariantDto variant, Set<String> variantIdentifiers, TagEditorValidationResult result) {
        if (variant == null) {
            result.error("Tag '" + parent + "' has a null variant.");
            return;
        }
        if (!isIdentifier(variant.identifier)) {
            result.error("Tag '" + parent + "' has an invalid variant identifier: " + variant.identifier);
            return;
        }
        if (!variantIdentifiers.add(variant.identifier.toLowerCase(Locale.ROOT))) {
            result.error("Duplicate variant identifier: " + variant.identifier);
        }
        if (isEmptyTagValue(variant.tag)) {
            result.error("Variant '" + variant.identifier + "' must have at least one display frame.");
        }
        if (variant.permission == null || variant.permission.isBlank()) {
            result.error("Variant '" + variant.identifier + "' is missing a permission.");
        }
        if (variant.rarity != null && !variant.rarity.isBlank() && !plugin.getRarityManager().isValid(variant.rarity)) {
            result.error("Variant '" + variant.identifier + "' references an unknown rarity: " + variant.rarity);
        }
        variant.unlockedMaterial = defaultString(variant.unlockedMaterial, "NAME_TAG");
        variant.lockedMaterial = defaultString(variant.lockedMaterial, "BARRIER");
        variant.unlockedDisplayName = defaultString(variant.unlockedDisplayName, "&7Variant: %tag%");
        variant.lockedDisplayName = defaultString(variant.lockedDisplayName, "&cLocked Variant: %tag%");
        validateMaterial(variant.identifier, "unlocked material", variant.unlockedMaterial, result);
        validateMaterial(variant.identifier, "locked material", variant.lockedMaterial, result);
    }

    private void validateRequirements(String tagId, EditorPayload.RequirementsDto requirements, TagEditorValidationResult result) {
        String mode = requirements.mode == null ? "all" : requirements.mode.toLowerCase(Locale.ROOT);
        if (!REQUIREMENT_MODES.contains(mode)) {
            result.error("Tag '" + tagId + "' has invalid requirement mode: " + requirements.mode);
        }
        if (requirements.list == null) {
            return;
        }
        Set<String> names = new HashSet<>();
        for (EditorPayload.RequirementDto requirement : requirements.list) {
            if (requirement == null || !isIdentifier(requirement.name)) {
                result.error("Tag '" + tagId + "' has an invalid requirement name.");
                continue;
            }
            if (!names.add(requirement.name.toLowerCase(Locale.ROOT))) {
                result.error("Tag '" + tagId + "' has duplicate requirement name: " + requirement.name);
            }
            String operator = requirement.operator == null ? "==" : requirement.operator.toLowerCase(Locale.ROOT);
            if (!REQUIREMENT_OPERATORS.contains(operator)) {
                result.error("Requirement '" + requirement.name + "' has invalid operator: " + requirement.operator);
            }
        }
    }

    private void validateEffect(String tagId, String effect, TagEditorValidationResult result) {
        if (effect == null || effect.isBlank()) {
            return;
        }
        String[] parts = effect.split(":");
        if (parts.length != 2) {
            result.error("Tag '" + tagId + "' has invalid effect format: " + effect);
            return;
        }
        if (PotionEffectType.getByName(parts[0].toUpperCase(Locale.ROOT)) == null) {
            result.error("Tag '" + tagId + "' has unknown effect: " + parts[0]);
        }
        try {
            int level = Integer.parseInt(parts[1]);
            if (level <= 0) {
                result.error("Tag '" + tagId + "' has non-positive effect level: " + effect);
            }
        } catch (NumberFormatException exception) {
            result.error("Tag '" + tagId + "' has non-numeric effect level: " + effect);
        }
    }

    private void validateMaterial(String owner, String field, String material, TagEditorValidationResult result) {
        if (material == null || material.isBlank()) {
            result.error("'" + owner + "' has an empty " + field + ".");
            return;
        }
        if (material.contains("..") || material.contains("/") || material.contains("\\")) {
            result.error("'" + owner + "' has an unsafe " + field + ": " + material);
            return;
        }
        if (material.contains(":")) {
            return;
        }
        try {
            Material.valueOf(material.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            result.warning("'" + owner + "' uses a non-Bukkit " + field + ": " + material);
        }
    }

    private boolean isIdentifier(String value) {
        return value != null && IDENTIFIER.matcher(value).matches();
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private boolean isEmptyTagValue(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return true;
        }
        if (element.isJsonArray()) {
            return element.getAsJsonArray().isEmpty();
        }
        return element.getAsString().isBlank();
    }
}
