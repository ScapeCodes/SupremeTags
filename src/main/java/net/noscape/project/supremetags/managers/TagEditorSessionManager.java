package net.noscape.project.supremetags.managers;

import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.handlers.requirements.TagRequirement;
import net.noscape.project.supremetags.handlers.requirements.TagRequirements;

import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TagEditorSessionManager {

    private final Map<UUID, TagSnapshot> undoSnapshots = new ConcurrentHashMap<>();
    private final Map<String, WebSession> webSessions = new ConcurrentHashMap<>();

    public void registerWebSession(String sessionId, String applyToken) {
        registerWebSession(sessionId, applyToken, null);
    }

    public void registerWebSession(String sessionId, String applyToken, String expiresAt) {
        if (sessionId == null || sessionId.isBlank() || applyToken == null || applyToken.isBlank()) {
            return;
        }

        Instant expiry = Instant.now().plus(Duration.ofHours(1));
        if (expiresAt != null && !expiresAt.isBlank()) {
            try {
                Instant remoteExpiry = Instant.parse(expiresAt);
                if (remoteExpiry.isBefore(expiry)) {
                    expiry = remoteExpiry;
                }
            } catch (DateTimeParseException ignored) {
                // Older backends may omit a usable expiry; retain the one-hour initial limit.
            }
        }
        webSessions.entrySet().removeIf(entry -> entry.getValue().isExpired());
        webSessions.put(sessionId, new WebSession(applyToken, expiry));
    }

    public String getWebApplyToken(String sessionId) {
        WebSession session = getWebSession(sessionId);
        return session == null ? null : session.getApplyToken();
    }

    public WebSession getWebSession(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        WebSession session = webSessions.get(sessionId);
        if (session != null && session.isExpired()) {
            webSessions.remove(sessionId, session);
            return null;
        }
        return session;
    }

    public void removeWebSession(String sessionId) {
        webSessions.remove(sessionId);
    }

    public enum ApplyStatus {
        READY, BUSY, FORCE_REQUIRED, EXPIRED
    }

    public static final class WebSession {
        private final String applyToken;
        private final Instant initialExpiry;
        private boolean applied;
        private boolean applying;
        private String pendingRevision;
        private final Set<String> appliedRevisions = new HashSet<>();

        private WebSession(String applyToken, Instant initialExpiry) {
            this.applyToken = applyToken;
            this.initialExpiry = initialExpiry;
        }

        public String getApplyToken() {
            return applyToken;
        }

        public synchronized boolean isExpired() {
            // An apply admitted before expiry must retain its state until the import finishes.
            return !applied && !applying && !Instant.now().isBefore(initialExpiry);
        }

        public synchronized ApplyStatus beginApply(boolean force) {
            if (applying) {
                return ApplyStatus.BUSY;
            }
            if (isExpired()) {
                return ApplyStatus.EXPIRED;
            }
            if (applied && !force) {
                return ApplyStatus.FORCE_REQUIRED;
            }
            applying = true;
            return ApplyStatus.READY;
        }

        public synchronized void finishApply() {
            applying = false;
        }

        public synchronized void markApplied(String revision) {
            applied = true;
            pendingRevision = revision;
            appliedRevisions.add(revision);
        }

        public synchronized Set<String> getAppliedRevisions() {
            return Set.copyOf(appliedRevisions);
        }

        public synchronized String getPendingRevision() {
            return pendingRevision;
        }

        public synchronized void acknowledge(String revision) {
            if (revision != null && revision.equals(pendingRevision)) {
                pendingRevision = null;
            }
        }
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
