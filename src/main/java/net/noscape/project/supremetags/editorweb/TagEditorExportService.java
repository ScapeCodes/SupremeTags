package net.noscape.project.supremetags.editorweb;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.handlers.Variant;
import net.noscape.project.supremetags.handlers.requirements.TagRequirement;
import net.noscape.project.supremetags.handlers.requirements.TagRequirements;
import org.bukkit.Bukkit;
import org.bukkit.potion.PotionEffectType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

public class TagEditorExportService {

    private final SupremeTags plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public TagEditorExportService(SupremeTags plugin) {
        this.plugin = plugin;
    }

    public String exportJson() {
        return gson.toJson(exportPayload());
    }

    public EditorPayload exportPayload() {
        EditorPayload payload = new EditorPayload();
        payload.exportedAt = Instant.now().toString();
        payload.plugin.name = "SupremeTags";
        payload.plugin.version = plugin.getDescription().getVersion();
        payload.plugin.latestVersion = plugin.getConfig().getString("editor.latest-version-fallback", plugin.getDescription().getVersion());
        payload.plugin.serverVersion = Bukkit.getVersion();
        payload.plugin.storageMode = plugin.isDBTags() ? "DATABASE" : "FILE";
        payload.plugin.tagsLoaded = plugin.getTagManager().getTags().size();
        payload.plugin.categoriesLoaded = plugin.getCategoryManager().getCatorgies().size();

        payload.data.categories.addAll(plugin.getCategoryManager().getCatorgies());
        payload.data.rarities.addAll(plugin.getRarityManager().getRarityMap().keySet());

        for (Tag tag : plugin.getTagManager().getTags().values()) {
            payload.data.tags.add(toDto(tag));
        }

        return payload;
    }

    private EditorPayload.TagDto toDto(Tag tag) {
        EditorPayload.TagDto dto = new EditorPayload.TagDto();
        dto.identifier = tag.getIdentifier();
        dto.tag = new ArrayList<>(tag.getTag());
        dto.permission = tag.getPermission();
        dto.groups = new ArrayList<>(tag.getGroups());
        dto.description = new ArrayList<>(tag.getDescription());
        dto.category = tag.getCategory();
        dto.order = tag.getOrder();
        dto.withdrawable = tag.isWithdrawable();
        dto.rarity = tag.getRarity();
        dto.displayName = tag.getDisplayName();
        dto.displayItem = tag.getDisplayItem();
        dto.customModelData = tag.getCustomModelData();

        for (Map.Entry<PotionEffectType, Integer> entry : tag.getEffects().entrySet()) {
            dto.effects.add(entry.getKey().getKey().getKey().toUpperCase(Locale.ROOT) + ":" + entry.getValue());
        }
        dto.abilities = new ArrayList<>(tag.getAbilities());
        dto.customPlaceholders.putAll(tag.getCustomPlaceholders());

        dto.economy.enabled = tag.getEconomy().isEnabled();
        dto.economy.type = tag.getEconomy().getType();
        dto.economy.amount = tag.getEconomy().getAmount();
        dto.economy.takeCommand = tag.getEconomy().getTake_cmd();
        dto.economy.condition = tag.getEconomy().getCondition();

        dto.voucher.material = tag.getVoucherMaterial();
        dto.voucher.displayName = tag.getVoucherDisplayName();
        dto.voucher.lore = new ArrayList<>(tag.getVoucherLore());
        dto.voucher.customModelData = tag.getVoucherCustomModelData();
        dto.voucher.glow = tag.isVoucherGlow();

        for (Variant variant : tag.getVariants()) {
            dto.variants.add(toDto(variant));
        }

        dto.requirements = toDto(tag.getRequirements());
        return dto;
    }

    private EditorPayload.VariantDto toDto(Variant variant) {
        EditorPayload.VariantDto dto = new EditorPayload.VariantDto();
        dto.identifier = variant.getIdentifier();
        JsonArray tagFrames = new JsonArray();
        for (String frame : variant.getTag()) {
            tagFrames.add(frame);
        }
        dto.tag = tagFrames;
        dto.permission = variant.getPermission();
        dto.description = new ArrayList<>(variant.getDescription());
        dto.rarity = variant.getRarity();
        dto.unlockedMaterial = variant.getUnlocked_material();
        dto.unlockedDisplayName = variant.getUnlocked_displayname();
        dto.unlockedCustomModelData = variant.getUnlocked_custom_model_data();
        dto.lockedMaterial = variant.getLocked_material();
        dto.lockedDisplayName = variant.getLocked_displayname();
        dto.lockedCustomModelData = variant.getLocked_custom_model_data();
        return dto;
    }

    private EditorPayload.RequirementsDto toDto(TagRequirements requirements) {
        EditorPayload.RequirementsDto dto = new EditorPayload.RequirementsDto();
        if (requirements == null) {
            return dto;
        }

        dto.enabled = requirements.isConfiguredEnabled();
        dto.persistUnlock = requirements.isPersistUnlock();
        dto.mode = requirements.getMode().name().toLowerCase();
        for (TagRequirement requirement : requirements.getRequirements()) {
            dto.list.add(toDto(requirement));
        }
        return dto;
    }

    private EditorPayload.RequirementDto toDto(TagRequirement requirement) {
        EditorPayload.RequirementDto dto = new EditorPayload.RequirementDto();
        dto.name = requirement.getName();
        dto.type = requirement.getType();
        dto.permission = requirement.getPermission();
        dto.placeholder = requirement.getPlaceholder();
        dto.operator = requirement.getOperator();
        dto.value = requirement.getValue();
        dto.tag = requirement.getTag();
        dto.economyType = requirement.getEconomyType();
        dto.amount = requirement.getAmount();
        dto.display = requirement.getDisplay();
        dto.loreDisplay = requirement.getLoreDisplay();
        dto.message = requirement.getMessage();
        return dto;
    }
}
