package net.noscape.project.supremetags.editorweb;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.handlers.TagEconomy;
import net.noscape.project.supremetags.handlers.Variant;
import net.noscape.project.supremetags.handlers.requirements.TagRequirement;
import net.noscape.project.supremetags.handlers.requirements.TagRequirements;
import net.noscape.project.supremetags.managers.TagManager;
import net.noscape.project.supremetags.redis.RedisUpdateService;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class TagEditorImportService {

    private final SupremeTags plugin;
    private final Gson gson = new Gson();
    private final TagEditorValidationService validationService;

    public TagEditorImportService(SupremeTags plugin) {
        this.plugin = plugin;
        this.validationService = new TagEditorValidationService(plugin);
    }

    public ApplyResult applyJson(String json, CommandSender sender) {
        EditorPayload payload;
        try {
            payload = gson.fromJson(json, EditorPayload.class);
        } catch (JsonSyntaxException exception) {
            return ApplyResult.failed("Invalid editor JSON: " + exception.getMessage());
        }

        TagEditorValidationResult validation = validationService.validate(payload);
        if (!validation.isValid()) {
            return ApplyResult.invalid(validation);
        }

        int created = 0;
        int updated = 0;
        int deleted = 0;
        List<String> changes = new ArrayList<>();
        Map<String, Tag> appliedTags = new LinkedHashMap<>();
        boolean changedAny = false;
        Set<String> payloadIdentifiers = new HashSet<>();
        for (EditorPayload.TagDto tagDto : payload.data.tags) {
            payloadIdentifiers.add(tagDto.identifier);
        }

        RedisUpdateService redis = plugin.getRedisUpdateService();
        boolean batchRedisUpdate = redis != null && redis.isEnabled();

        if (batchRedisUpdate) {
            redis.setPublishingSuppressedForCurrentThread(true);
        }

        boolean completedApplyLoop = false;
        try {
            plugin.getTagManager().beginTagFileSyncBatch();
            for (String existingIdentifier : new ArrayList<>(plugin.getTagManager().getTags().keySet())) {
                if (payloadIdentifiers.contains(existingIdentifier)) {
                    continue;
                }

                if (plugin.getTagManager().deleteTag(existingIdentifier, false)) {
                    deleted++;
                    changedAny = true;
                    changes.add("&cDeleted " + existingIdentifier);
                }
            }

            for (EditorPayload.TagDto tagDto : payload.data.tags) {
                Tag existing = plugin.getTagManager().getTag(tagDto.identifier);
                if (existing == null) {
                    String firstFrame = tagDto.tag == null || tagDto.tag.isEmpty() ? "" : tagDto.tag.getFirst();
                    changes.add("&aCreated " + tagDto.identifier + " &7-> &f" + firstFrame);
                } else {
                    changes.addAll(compareTag(existing, tagDto));
                }

                if (existing != null && !hasEditableChanges(existing, tagDto)) {
                    continue;
                }

                Tag tag = existing == null ? createManagedTag(tagDto) : existing;
                apply(tag, tagDto);
                plugin.getTagManager().saveTag(tag);
                appliedTags.put(tag.getIdentifier(), tag);
                changedAny = true;
                if (existing == null) {
                    created++;
                } else {
                    updated++;
                }
            }
            completedApplyLoop = true;
        } finally {
            if (!completedApplyLoop) {
                plugin.getTagManager().endTagFileSyncBatch(false);
            }
            if (batchRedisUpdate) {
                redis.setPublishingSuppressedForCurrentThread(false);
            }
        }

        if (changedAny) {
            reloadAppliedTags();
            plugin.getTagManager().endTagFileSyncBatch(true);
        } else {
            plugin.getTagManager().endTagFileSyncBatch(false);
        }
        if (batchRedisUpdate) {
            redis.publishTagEditorApplied(appliedTags.values());
        }
        if (changedAny) {
            changes.add("&aReloaded SupremeTags runtime so applied changes are live.");
        }
        List<String> persistenceWarnings = verifyPersisted(payload);
        for (String warning : persistenceWarnings) {
            validation.warning(warning);
        }

        return ApplyResult.success(created, updated, deleted, payload.data.tags.size(), changes, validation);
    }

    private void reloadAppliedTags() {
        if (!plugin.getTagManager().isDBTags()) {
            plugin.getConfigManager().reloadTagConfigs();
        }
        plugin.getTagManager().unloadTags();
        plugin.getTagManager().loadTags(false);
        plugin.getTagManager().getDataItem().clear();
        plugin.getCategoryManager().initCategories();
    }

    private List<String> verifyPersisted(EditorPayload payload) {
        List<String> warnings = new ArrayList<>();
        for (EditorPayload.TagDto edited : payload.data.tags) {
            Tag reloaded = plugin.getTagManager().getTag(edited.identifier);
            if (reloaded == null) {
                warnings.add("Tag '" + edited.identifier + "' was not found after reload.");
                continue;
            }
            if (!reloaded.getTag().equals(copy(edited.tag))) {
                warnings.add("Tag '" + edited.identifier + "' display frames did not persist after reload.");
            }
            if (!same(reloaded.getPermission(), edited.permission)) {
                warnings.add("Tag '" + edited.identifier + "' permission did not persist after reload.");
            }
            if (!same(reloaded.getCategory(), edited.category)) {
                warnings.add("Tag '" + edited.identifier + "' category did not persist after reload.");
            }
            if (!same(reloaded.getRarity(), edited.rarity)) {
                warnings.add("Tag '" + edited.identifier + "' rarity did not persist after reload.");
            }
            if (edited.economy != null && Double.compare(reloaded.getEconomy().getAmount(), edited.economy.amount) != 0) {
                warnings.add("Tag '" + edited.identifier + "' cost did not persist after reload.");
            }
        }
        return warnings;
    }

    private List<String> compareTag(Tag existing, EditorPayload.TagDto edited) {
        List<String> changes = new ArrayList<>();
        String id = existing.getIdentifier();

        String beforeDisplay = first(existing.getTag());
        String afterDisplay = first(edited.tag);
        if (!beforeDisplay.equals(afterDisplay)) {
            changes.add(beforeDisplay + " &7-> " + afterDisplay);
        }
        if (!existing.getTag().equals(copy(edited.tag))) {
            changes.add("&e" + id + " &7tag frames changed &8(" + existing.getTag().size() + " -> " + copy(edited.tag).size() + ")");
        }
        if (!same(existing.getPermission(), edited.permission)) {
            changes.add("&e" + id + " &7permission: &f" + existing.getPermission() + " &7-> &f" + edited.permission);
        }
        if (!same(existing.getCategory(), edited.category)) {
            changes.add("&e" + id + " &7category: &f" + existing.getCategory() + " &7-> &f" + edited.category);
        }
        if (!same(existing.getRarity(), edited.rarity)) {
            changes.add("&e" + id + " &7rarity: &f" + existing.getRarity() + " &7-> &f" + edited.rarity);
        }
        if (existing.getOrder() != edited.order) {
            changes.add("&e" + id + " &7order: &f" + existing.getOrder() + " &7-> &f" + edited.order);
        }
        if (existing.isWithdrawable() != edited.withdrawable) {
            changes.add("&e" + id + " &7withdrawable: &f" + existing.isWithdrawable() + " &7-> &f" + edited.withdrawable);
        }
        if (edited.economy != null) {
            if (existing.getEconomy().isEnabled() != edited.economy.enabled) {
                changes.add("&e" + id + " &7economy enabled: &f" + existing.getEconomy().isEnabled() + " &7-> &f" + edited.economy.enabled);
            }
            if (!same(existing.getEconomy().getType(), edited.economy.type)) {
                changes.add("&e" + id + " &7economy type: &f" + existing.getEconomy().getType() + " &7-> &f" + edited.economy.type);
            }
            if (Double.compare(existing.getEconomy().getAmount(), edited.economy.amount) != 0) {
                changes.add("&e" + id + " &7cost: &f" + existing.getEconomy().getAmount() + " &7-> &f" + edited.economy.amount);
            }
        }
        if (edited.voucher != null) {
            if (!same(existing.getVoucherMaterial(), edited.voucher.material)) {
                changes.add("&e" + id + " &7voucher material changed");
            }
            if (!same(existing.getVoucherDisplayName(), edited.voucher.displayName)) {
                changes.add("&e" + id + " &7voucher name changed");
            }
            if (!existing.getVoucherLore().equals(copy(edited.voucher.lore))) {
                changes.add("&e" + id + " &7voucher lore changed");
            }
        }
        if (existing.getVariants().size() != (edited.variants == null ? 0 : edited.variants.size())) {
            changes.add("&e" + id + " &7variants: &f" + existing.getVariants().size() + " &7-> &f" + (edited.variants == null ? 0 : edited.variants.size()));
        }
        int existingRequirements = existing.getRequirements() == null ? 0 : existing.getRequirements().getRequirements().size();
        int editedRequirements = edited.requirements == null || edited.requirements.list == null ? 0 : edited.requirements.list.size();
        if (existingRequirements != editedRequirements) {
            changes.add("&e" + id + " &7requirements: &f" + existingRequirements + " &7-> &f" + editedRequirements);
        }

        return changes;
    }

    private String first(List<String> values) {
        return values == null || values.isEmpty() ? "" : values.getFirst();
    }

    private boolean same(String first, String second) {
        return Objects.equals(first == null ? "" : first, second == null ? "" : second);
    }

    private Tag createManagedTag(EditorPayload.TagDto dto) {
        String firstFrame = dto.tag == null || dto.tag.isEmpty() ? "" : dto.tag.getFirst();
        TagEconomy economy = new TagEconomy("VAULT", 0, false);
        return new Tag(dto.identifier, List.of(firstFrame), dto.category, dto.permission, dto.description == null ? new ArrayList<>() : new ArrayList<>(dto.description), dto.order, dto.withdrawable, dto.rarity, new LinkedHashMap<>(), economy, new ArrayList<>());
    }

    private boolean hasEditableChanges(Tag existing, EditorPayload.TagDto edited) {
        if (!existing.getTag().equals(copy(edited.tag))) return true;
        if (!same(existing.getPermission(), edited.permission)) return true;
        if (!existing.getGroups().equals(copy(edited.groups))) return true;
        if (!existing.getDescription().equals(copy(edited.description))) return true;
        if (!same(existing.getCategory(), edited.category)) return true;
        if (existing.getOrder() != edited.order) return true;
        if (existing.isWithdrawable() != edited.withdrawable) return true;
        if (!same(existing.getRarity(), edited.rarity)) return true;
        if (!same(existing.getDisplayName(), edited.displayName)) return true;
        if (!same(existing.getDisplayItem(), edited.displayItem)) return true;
        if (existing.getCustomModelData() != edited.customModelData) return true;
        if (!existing.getEffects().equals(TagManager.parseEffects(copy(edited.effects)))) return true;
        if (!existing.getAbilities().equals(copy(edited.abilities))) return true;
        if (!existing.getCustomPlaceholders().equals(edited.customPlaceholders == null ? new LinkedHashMap<>() : new LinkedHashMap<>(edited.customPlaceholders))) return true;

        if (edited.economy != null) {
            if (existing.getEconomy().isEnabled() != edited.economy.enabled) return true;
            if (!same(existing.getEconomy().getType(), edited.economy.type)) return true;
            if (Double.compare(existing.getEconomy().getAmount(), edited.economy.amount) != 0) return true;
            if (!same(existing.getEconomy().getTake_cmd(), edited.economy.takeCommand)) return true;
            if (!same(existing.getEconomy().getCondition(), edited.economy.condition)) return true;
        }

        if (edited.voucher != null) {
            if (!same(existing.getVoucherMaterial(), edited.voucher.material)) return true;
            if (!same(existing.getVoucherDisplayName(), edited.voucher.displayName)) return true;
            if (!existing.getVoucherLore().equals(copy(edited.voucher.lore))) return true;
            if (existing.getVoucherCustomModelData() != edited.voucher.customModelData) return true;
            if (existing.isVoucherGlow() != edited.voucher.glow) return true;
        }

        return existing.getVariants().size() != (edited.variants == null ? 0 : edited.variants.size())
                || requirementsChanged(existing, edited);
    }

    private boolean requirementsChanged(Tag existing, EditorPayload.TagDto edited) {
        int existingRequirements = existing.getRequirements() == null ? 0 : existing.getRequirements().getRequirements().size();
        int editedRequirements = edited.requirements == null || edited.requirements.list == null ? 0 : edited.requirements.list.size();
        return existingRequirements != editedRequirements;
    }

    private void apply(Tag tag, EditorPayload.TagDto dto) {
        tag.setIdentifier(dto.identifier);
        tag.setTag(copy(dto.tag));
        tag.setPermission(dto.permission);
        tag.setGroups(copy(dto.groups));
        tag.setDescription(copy(dto.description));
        tag.setCategory(dto.category);
        tag.setOrder(dto.order);
        tag.setWithdrawable(dto.withdrawable);
        tag.setRarity(dto.rarity);
        tag.setDisplayName(dto.displayName);
        tag.setDisplayItem(dto.displayItem);
        tag.setCustomModelData(dto.customModelData);
        tag.getEffects().clear();
        tag.getEffects().putAll(TagManager.parseEffects(copy(dto.effects)));
        tag.setAbilities(copy(dto.abilities));
        tag.setCustomPlaceholders(dto.customPlaceholders == null ? new LinkedHashMap<>() : new LinkedHashMap<>(dto.customPlaceholders));

        if (dto.economy != null) {
            tag.getEconomy().setEnabled(dto.economy.enabled);
            tag.getEconomy().setType(dto.economy.type);
            tag.getEconomy().setAmount(dto.economy.amount);
            tag.getEconomy().setTake_cmd(dto.economy.takeCommand);
            tag.getEconomy().setCondition(dto.economy.condition);
        }

        if (dto.voucher != null) {
            tag.setVoucherMaterial(dto.voucher.material);
            tag.setVoucherDisplayName(dto.voucher.displayName);
            tag.setVoucherLore(copy(dto.voucher.lore));
            tag.setVoucherCustomModelData(dto.voucher.customModelData);
            tag.setVoucherGlow(dto.voucher.glow);
        }

        List<Variant> variants = new ArrayList<>();
        if (dto.variants != null) {
            for (EditorPayload.VariantDto variantDto : dto.variants) {
                variants.add(toVariant(dto.identifier, variantDto));
            }
        }
        tag.setVariants(variants);
        tag.setRequirements(toRequirements(dto.requirements));
    }

    private Variant toVariant(String parent, EditorPayload.VariantDto dto) {
        Variant variant = new Variant(dto.identifier, parent, readStringList(dto.tag), dto.permission, copy(dto.description), dto.rarity);
        variant.setUnlocked_material(defaultString(dto.unlockedMaterial, "NAME_TAG"));
        variant.setUnlocked_displayname(defaultString(dto.unlockedDisplayName, "&7Variant: %tag%"));
        variant.setUnlocked_custom_model_data(dto.unlockedCustomModelData);
        variant.setLocked_material(defaultString(dto.lockedMaterial, "BARRIER"));
        variant.setLocked_displayname(defaultString(dto.lockedDisplayName, "&cLocked Variant: %tag%"));
        variant.setLocked_custom_model_data(dto.lockedCustomModelData);
        return variant;
    }

    private TagRequirements toRequirements(EditorPayload.RequirementsDto dto) {
        if (dto == null) {
            return null;
        }
        List<TagRequirement> requirements = new ArrayList<>();
        if (dto.list != null) {
            for (EditorPayload.RequirementDto requirement : dto.list) {
                requirements.add(new TagRequirement(
                        requirement.name,
                        requirement.type,
                        requirement.permission,
                        requirement.placeholder,
                        requirement.operator,
                        requirement.value,
                        requirement.tag,
                        requirement.economyType,
                        requirement.amount,
                        requirement.display,
                        requirement.loreDisplay,
                        requirement.message
                ));
            }
        }
        return new TagRequirements(dto.enabled, dto.persistUnlock, TagRequirements.Mode.from(dto.mode), requirements);
    }

    private List<String> copy(List<String> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private List<String> readStringList(JsonElement element) {
        List<String> values = new ArrayList<>();
        if (element == null || element.isJsonNull()) {
            return values;
        }
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                if (!child.isJsonNull()) {
                    values.add(child.getAsString());
                }
            }
            return values;
        }
        values.add(element.getAsString());
        return values;
    }

    public static class ApplyResult {
        private final boolean success;
        private final String message;
        private final int created;
        private final int updated;
        private final int deleted;
        private final int payloadTagCount;
        private final List<String> displayChanges;
        private final TagEditorValidationResult validation;

        private ApplyResult(boolean success, String message, int created, int updated, int deleted, int payloadTagCount, List<String> displayChanges, TagEditorValidationResult validation) {
            this.success = success;
            this.message = message;
            this.created = created;
            this.updated = updated;
            this.deleted = deleted;
            this.payloadTagCount = payloadTagCount;
            this.displayChanges = displayChanges == null ? new ArrayList<>() : new ArrayList<>(displayChanges);
            this.validation = validation;
        }

        public static ApplyResult success(int created, int updated, int deleted, int payloadTagCount, List<String> displayChanges, TagEditorValidationResult validation) {
            return new ApplyResult(true, "Applied editor payload.", created, updated, deleted, payloadTagCount, displayChanges, validation);
        }

        public static ApplyResult invalid(TagEditorValidationResult validation) {
            return new ApplyResult(false, "Editor payload failed validation.", 0, 0, 0, 0, null, validation);
        }

        public static ApplyResult failed(String message) {
            return new ApplyResult(false, message, 0, 0, 0, 0, null, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public int getCreated() {
            return created;
        }

        public int getUpdated() {
            return updated;
        }

        public int getDeleted() {
            return deleted;
        }

        public int getPayloadTagCount() {
            return payloadTagCount;
        }

        public List<String> getDisplayChanges() {
            return new ArrayList<>(displayChanges);
        }

        public TagEditorValidationResult getValidation() {
            return validation;
        }
    }
}
