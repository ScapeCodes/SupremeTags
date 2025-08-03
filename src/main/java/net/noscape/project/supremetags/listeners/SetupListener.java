package net.noscape.project.supremetags.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.handlers.SetupTag;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.storage.PlayerConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
            SupremeTags.getInstance().getSetupList().remove(player);
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

        for (String word : messages.getStringList("banned-words")) {
            if (isWordBlocked(message, word)) {
                isBannedWord = true;
                break;
            }
        }

        if (!player.hasPermission("supremetags.admin")) {
            if (containsPlaceholders(message)) {
                msgPlayer(player, placeholder_error);
                return;
            }
        }

        if (message.equalsIgnoreCase("cancel")) {
            SupremeTags.getInstance().getSetupList().remove(player);
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
        PlayerConfig.save(player);
        SupremeTags.getInstance().getPlayerManager().load(player);

        String stage_complete = messages.getString("messages.stages.complete").replaceAll("%prefix%", Objects.requireNonNull(messages.getString("messages.prefix")));

        stage_complete = stage_complete.replaceAll("%identifier%", setup.getIdentifier());
        stage_complete = stage_complete.replaceAll("%tag%", setup.getTag());

        msgPlayer(player, stage_complete);
        SupremeTags.getInstance().getSetupList().remove(player);
    }

    private boolean isWordBlocked(String message, String blockedWord) {
        String pattern = "\\b" + blockedWord + "\\b";
        Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = regex.matcher(message);

        return matcher.find();
    }

    private boolean containsPlaceholders(String message) {
        // This regex matches any substring starting with %, then any chars except %, then %
        Pattern placeholderPattern = Pattern.compile("%[^%]+%");
        Matcher matcher = placeholderPattern.matcher(message);
        return matcher.find();
    }
}