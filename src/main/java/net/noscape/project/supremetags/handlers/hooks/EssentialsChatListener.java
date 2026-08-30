package net.noscape.project.supremetags.handlers.hooks;

import net.essentialsx.api.v2.events.chat.GlobalChatEvent;
import net.essentialsx.api.v2.events.chat.LocalChatEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.noscape.project.supremetags.handlers.TagFormatter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EssentialsChatListener implements Listener {

    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();
    private static final Pattern FORMAT_TOKEN = Pattern.compile(
            "%(?:%|n|(?:\\d+\\$)?[-#+ 0,(<]*\\d*(?:\\.\\d+)?(?:[bBhHsScCdoxXeEfgGaA]|[tT][A-Za-z]))"
    );

    @EventHandler
    public void onGlobalChat(GlobalChatEvent event) {
        String message = event.getFormat();
        message = replaceTagPlaceholders(message, event.getPlayer().getUniqueId());

        event.setFormat(escapeLiteralPercents(message));
    }

    @EventHandler
    public void onLocalChat(LocalChatEvent event) {
        String message = event.getFormat();
        message = replaceTagPlaceholders(message, event.getPlayer().getUniqueId());

        event.setFormat(escapeLiteralPercents(message));
    }

    private String replaceTagPlaceholders(String text, UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return text;

        String tag = LEGACY_SECTION.serialize(TagFormatter.getFormattedTagComponent(player, TagFormatter.Context.CHAT));

        return text
                .replace("{tag}", tag)
                .replace("{TAG}", tag)
                .replace("{supremetags_tag}", tag);
    }

    private String escapeLiteralPercents(String text) {
        StringBuilder escaped = new StringBuilder(text.length());

        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (character != '%') {
                escaped.append(character);
                continue;
            }

            Matcher matcher = FORMAT_TOKEN.matcher(text);
            matcher.region(index, text.length());
            if (matcher.lookingAt()) {
                escaped.append(matcher.group());
                index = matcher.end() - 1;
                continue;
            }

            escaped.append("%%");
        }

        return escaped.toString();
    }
}
