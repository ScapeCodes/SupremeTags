package net.noscape.project.supremetags.managers;

import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.handlers.requirements.TagRequirement;
import net.noscape.project.supremetags.handlers.requirements.TagRequirements;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TagEditorSessionManager {

    private final Map<UUID, TagSnapshot> undoSnapshots = new ConcurrentHashMap<>();
    private final Map<String, String> webApplyTokens = new ConcurrentHashMap<>();

    public void registerWebSession(String sessionId, String applyToken) {
        if (sessionId == null || sessionId.isBlank() || applyToken == null || applyToken.isBlank()) {
            return;
        }

        webApplyTokens.put(sessionId, applyToken);
    }

    public String getWebApplyToken(String sessionId) {
        return webApplyTokens.get(sessionId);
    }

    public void removeWebSession(String sessionId) {
        webApplyTokens.remove(sessionId);
    }

    public void snapshot(Player player, Tag tag) {
        if (player == null || tag == null) {
            return;
        }

        undoSnapshots.put(player.getUniqueId(), TagSnapshot.from(tag));
    }

    public boolean undo(Player player) {
        if (player == null) {
            return false;
        }

        TagSnapshot snapshot = undoSnapshots.remove(player.getUniqueId());
        if (snapshot == null) {
            return false;
        }

        Tag tag = SupremeTags.getInstance().getTagManager().getTag(snapshot.identifier());
        if (tag == null) {
            return false;
        }

        snapshot.apply(tag);
        SupremeTags.getInstance().getTagManager().saveTag(tag);
        SupremeTags.getInstance().getTagManager().unloadTags();
        SupremeTags.getInstance().getTagManager().loadTags(true);
        SupremeTags.getInstance().getCategoryManager().initCategories();
        return true;
    }

    private record TagSnapshot(
            String identifier,
            List<String> tag,
            String category,
            String permission,
            List<String> description,
            int order,
            boolean withdrawable,
            String rarity,
            String economyType,
            double economyAmount,
            boolean economyEnabled,
            String economyTakeCommand,
            String economyCondition,
            String voucherDisplayName,
            String voucherMaterial,
            List<String> voucherLore,
            int voucherCustomModelData,
            boolean voucherGlow,
            TagRequirements requirements
    ) {

        private static TagSnapshot from(Tag tag) {
            return new TagSnapshot(
                    tag.getIdentifier(),
                    new ArrayList<>(tag.getTag()),
                    tag.getCategory(),
                    tag.getPermission(),
                    new ArrayList<>(tag.getDescription()),
                    tag.getOrder(),
                    tag.isWithdrawable(),
                    tag.getRarity(),
                    tag.getEconomy().getType(),
                    tag.getEconomy().getAmount(),
                    tag.getEconomy().isEnabled(),
                    tag.getEconomy().getTake_cmd(),
                    tag.getEconomy().getCondition(),
                    tag.getVoucherDisplayName(),
                    tag.getVoucherMaterial(),
                    new ArrayList<>(tag.getVoucherLore()),
                    tag.getVoucherCustomModelData(),
                    tag.isVoucherGlow(),
                    copyRequirements(tag.getRequirements())
            );
        }

        private static TagRequirements copyRequirements(TagRequirements requirements) {
            if (requirements == null) {
                return null;
            }

            List<TagRequirement> copied = new ArrayList<>();
            for (TagRequirement requirement : requirements.getRequirements()) {
                copied.add(new TagRequirement(
                        requirement.getName(),
                        requirement.getType(),
                        requirement.getPermission(),
                        requirement.getPlaceholder(),
                        requirement.getOperator(),
                        requirement.getValue(),
                        requirement.getTag(),
                        requirement.getEconomyType(),
                        requirement.getAmount(),
                        requirement.getDisplay(),
                        requirement.getLoreDisplay(),
                        requirement.getMessage()
                ));
            }

            return new TagRequirements(requirements.isConfiguredEnabled(), requirements.isPersistUnlock(), requirements.getMode(), copied);
        }

        private void apply(Tag tag) {
            tag.setTag(new ArrayList<>(this.tag));
            tag.setCategory(this.category);
            tag.setPermission(this.permission);
            tag.setDescription(new ArrayList<>(this.description));
            tag.setOrder(this.order);
            tag.setWithdrawable(this.withdrawable);
            tag.setRarity(this.rarity);
            tag.getEconomy().setType(this.economyType);
            tag.getEconomy().setAmount(this.economyAmount);
            tag.getEconomy().setEnabled(this.economyEnabled);
            tag.getEconomy().setTake_cmd(this.economyTakeCommand);
            tag.getEconomy().setCondition(this.economyCondition);
            tag.setVoucherDisplayName(this.voucherDisplayName);
            tag.setVoucherMaterial(this.voucherMaterial);
            tag.setVoucherLore(new ArrayList<>(this.voucherLore));
            tag.setVoucherCustomModelData(this.voucherCustomModelData);
            tag.setVoucherGlow(this.voucherGlow);
            tag.setRequirements(copyRequirements(this.requirements));
        }
    }
}
