package net.noscape.project.supremetags.listeners;

import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.enums.TPermissions;
import net.noscape.project.supremetags.guis.search.SearchResultMenu;
import net.noscape.project.supremetags.handlers.SetupTag;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.managers.PlayerManager;
import net.noscape.project.supremetags.managers.TagManager;
import net.noscape.project.supremetags.storage.user.PlayerConfig;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.noscape.project.supremetags.utils.Utils.*;

public class SetupListener implements Listener {

    private FileConfiguration bannedwords = SupremeTags.getInstance().getConfigManager().getConfig("banned-words.yml").get();
    private FileConfiguration messages = SupremeTags.getInstance().getConfigManager().getConfig("messages.yml").get();

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        SetupTag setup = SupremeTags.getInstance().getSetupList().get(player);

        if (setup == null) {
            return;
        }

        int currentStage = setup.getStage();

        // Convert the Component message to legacy String format
        String message = LegacyComponentSerializer.legacySection().serialize(event.message());

        if (currentStage == 1) {
            handleIdentifierInput(player, setup, message);
            event.setCancelled(true);
        } else if (currentStage == 2) {
            handleTagInput(player, setup, message);
            event.setCancelled(true);
        }
    }

    private void handleIdentifierInput(Player player, SetupTag setup, String message) {
        boolean isBannedWord = false;

        String bad_word = messages.getString("messages.bad-word").replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
        String stages_cancelled = messages.getString("messages.stages.cancelled").replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));

        for (String word : bannedwords.getStringList("banned-words")) {
            if (isWordBlocked(message, word)) {
                isBannedWord = true;
                break;
            }
        }

        if (message.equalsIgnoreCase("cancel")) {
            SupremeTags.getInstance().removeSetup(player);
            msgPlayer(player, stages_cancelled);
            return;
        }

        if (!isBannedWord) {
            setup.setStage(2);

            String stage_two = messages.getString("messages.stages.stage-2").replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));

            setup.setIdentifier(deformat(message));
            setup.setStage(2);

            msgPlayer(player, stage_two);
        } else {
            msgPlayer(player, bad_word);
        }
    }

    private void handleTagInput(Player player, SetupTag setup, String message) {
        boolean isBannedWord = false;

        String bad_word = messages.getString("messages.bad-word").replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
        String stages_cancelled = messages.getString("messages.stages.cancelled").replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
        String placeholder_error = messages.getString("messages.placeholder-error").replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
        String similar_error = messages.getString("messages.similar-error").replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));

        for (String word : messages.getStringList("banned-words")) {
            if (isWordBlocked(message, word)) {
                isBannedWord = true;
                break;
            }
        }

        if (!player.hasPermission(TPermissions.ADMIN)) {
            if (containsPlaceholders(message)) {
                msgPlayer(player, placeholder_error);
                return;
            }
        }

        if (!player.hasPermission(TPermissions.ADMIN)) {
            if (isTooCloseToOthers(message)) {
                msgPlayer(player, similar_error);
                return;
            }
        }

        if (message.equalsIgnoreCase("cancel")) {
            SupremeTags.getInstance().removeSetup(player);
            msgPlayer(player, stages_cancelled);
            return;
        }

        if (!isBannedWord) {
            if (setup.isIdentifierSet() && !setup.isTagSet()) {
                setup.setTag(message);
                handleTagComplete(player, setup);
            } else {
                // If the player hasn't set the identifier yet, request it again
                String stage_2 = messages.getString("messages.stages.stage-2").replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));

                stage_2 = stage_2.replaceAll("%identifier%", setup.getIdentifier());
                stage_2 = stage_2.replaceAll("%tag%", setup.getTag());
                msgPlayer(player, stage_2);

            }
        } else {
            msgPlayer(player, bad_word);
        }
    }

    private void handleTagComplete(Player player, SetupTag setup) {
        List<String> tagList = new ArrayList<>();

        String t = setup.getTag();

        if (!player.hasPermission("supremetags.mytags.color")) {
            t = deformat(t);
        }

        String replace_tag = SupremeTags.getInstance().getConfig().getString("settings.personal-tags.format-replace").replace("%tag%", t);

        tagList.add(replace_tag);

        // Save the tag and perform any necessary actions
        Tag tag = new Tag(setup.getIdentifier(), tagList, new ArrayList<>());
        SupremeTags.getInstance().getPlayerManager().addTag(player, tag);
        PlayerConfig.save(player.getUniqueId());
        SupremeTags.getInstance().getPlayerManager().load(player);

        String stage_complete = messages.getString("messages.stages.complete").replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));

        stage_complete = stage_complete.replaceAll("%identifier%", setup.getIdentifier());
        stage_complete = stage_complete.replaceAll("%tag%", setup.getTag());

        msgPlayer(player, stage_complete);
        SupremeTags.getInstance().removeSetup(player);
    }

    private boolean isWordBlocked(String message, String blockedWord) {
        String pattern = "\\b" + blockedWord + "\\b";
        Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = regex.matcher(message);

        return matcher.find();
    }

    private boolean containsPlaceholders(String message) {
        Pattern placeholderPattern = Pattern.compile("%[^%]+%");
        Matcher matcher = placeholderPattern.matcher(message);

        if (SupremeTags.getInstance().isPlaceholderAPI()) {
            return matcher.find() || PlaceholderAPI.containsPlaceholders(message);
        }

        return matcher.find();
    }

    public boolean isTooCloseToOthers(String tag) {
        String normalizedTag = deformat(tag).toLowerCase(Locale.ROOT);
        Set<String> allTags = new HashSet<>();

        SupremeTags plugin = SupremeTags.getInstance();
        TagManager tagManager = plugin.getTagManager();
        PlayerManager playerManager = plugin.getPlayerManager();

        tagManager.getTags().values().forEach(t -> allTags.add(deformat(t.getTag().getFirst()).toLowerCase(Locale.ROOT)));
        tagManager.getVariants().forEach(v -> allTags.add(deformat(v.getTag().getFirst()).toLowerCase(Locale.ROOT)));

        // OLD
//        Arrays.stream(Bukkit.getOfflinePlayers())
//                .flatMap(p -> playerManager.getPlayerTags(p.getUniqueId()).stream())
//                .map(t -> deformat(t.getTag().getFirst()).toLowerCase(Locale.ROOT))
//                .forEach(allTags::add);

        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            UUID uuid = player.getUniqueId();

            for (var t : playerManager.getPlayerTags(uuid)) {
                allTags.add(
                        deformat(t.getTag().getFirst()).toLowerCase(Locale.ROOT)
                );
            }
        }

        return allTags.contains(normalizedTag);
    }

    @EventHandler
    public void onSearchDialog(PlayerCustomClickEvent event) {

        if (!event.getIdentifier().equals(Key.key("supremetags:search"))) {
            return;
        }

        if (event.getDialogResponseView() == null) {
            return;
        }

        if (!(event.getCommonConnection() instanceof PlayerGameConnection connection)) {
            return;
        }

        Player player = connection.getPlayer();

        DialogResponseView view = event.getDialogResponseView();

        String search = view.getText("search");

        if (search == null || search.isBlank()) {
            String searchInvalid = messages.getString("messages.search-invalid-2")
                    .replace("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
            msgPlayer(player, searchInvalid);
            return;
        }

        if (SupremeTags.getInstance().getCategoryManager().isCategoryNearName(search)
                || SupremeTags.getInstance().getTagManager().tagExistsNearName(search)) {

            runMain(
                    () -> new SearchResultMenu(
                            SupremeTags.getMenuUtil(player),
                            search
                    ).open()
            );

        } else {
            String searchInvalid = messages.getString("messages.search-invalid-1")
                    .replace("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));
            msgPlayer(player, searchInvalid);
        }
    }
    @EventHandler
    public void onCreateTag(PlayerCustomClickEvent event) {

        if (!event.getIdentifier().equals(Key.key("supremetags:create_tag"))) {
            return;
        }

        if (event.getDialogResponseView() == null) {
            return;
        }

        if (!(event.getCommonConnection() instanceof PlayerGameConnection connection)) {
            return;
        }

        Player player = connection.getPlayer();

        DialogResponseView view = event.getDialogResponseView();

        String identifier = view.getText("identifier");
        String tag = view.getText("tag");

        FileConfiguration bannedWords = SupremeTags.getInstance()
                .getConfigManager()
                .getConfig("banned-words.yml")
                .get();

        FileConfiguration messages = SupremeTags.getInstance()
                .getConfigManager()
                .getConfig("messages.yml")
                .get();

        // Empty checks
        if (identifier.isEmpty() || tag.isEmpty()) {
            msgPlayer(player, "&cBoth fields must be filled in.");
            return;
        }

        // Identifier bad words
        for (String word : bannedWords.getStringList("banned-words")) {
            if (isWordBlocked(identifier, word)) {
                msgPlayer(player,
                        messages.getString("messages.bad-word")
                                .replace("%prefix%", messages.getString("messages.prefix")));
                return;
            }
        }

        // Tag bad words
        for (String word : bannedWords.getStringList("banned-words")) {
            if (isWordBlocked(tag, word)) {
                msgPlayer(player,
                        messages.getString("messages.bad-word")
                                .replace("%prefix%", messages.getString("messages.prefix")));
                return;
            }
        }

        // Placeholder check
        if (!player.hasPermission(TPermissions.ADMIN) && containsPlaceholders(tag)) {
            msgPlayer(player,
                    messages.getString("messages.placeholder-error")
                            .replace("%prefix%", messages.getString("messages.prefix")));
            return;
        }

        if (!player.hasPermission(TPermissions.ADMIN)
                && isTooCloseToOthers(tag)) {

            msgPlayer(player,
                    messages.getString("messages.similar-error")
                            .replace("%prefix%", messages.getString("messages.prefix")));
            return;
        }

        identifier = deformat(identifier);

        if (!player.hasPermission("supremetags.mytags.color")) {
            tag = deformat(tag);
        }

        String formattedTag = SupremeTags.getInstance()
                .getConfig()
                .getString("settings.personal-tags.format-replace")
                .replace("%tag%", tag);

        List<String> tagList = new ArrayList<>();
        tagList.add(formattedTag);

        Tag personalTag = new Tag(identifier, tagList, new ArrayList<>());

        SupremeTags.getInstance().getPlayerManager().addTag(player, personalTag);

        PlayerConfig.save(player.getUniqueId());
        SupremeTags.getInstance().getPlayerManager().load(player);

        String complete = messages.getString("messages.stages.complete")
                .replace("%prefix%", messages.getString("messages.prefix"))
                .replace("%identifier%", identifier)
                .replace("%tag%", tag);

        msgPlayer(player, complete);
    }

}