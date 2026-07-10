package net.noscape.project.supremetags.handlers.hooks;

import net.essentialsx.api.v2.events.chat.GlobalChatEvent;
import net.essentialsx.api.v2.events.chat.LocalChatEvent;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.handlers.TagFormatter;
import net.noscape.project.supremetags.handlers.Variant;
import net.noscape.project.supremetags.storage.UserData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static net.noscape.project.supremetags.utils.Utils.format;
import static net.noscape.project.supremetags.utils.Utils.replacePlaceholders;

public class EssentialsChatListener implements Listener {

    @EventHandler
    public void onGlobalChat(GlobalChatEvent event) {
        String message = event.getFormat();
        message = replaceTagPlaceholders(message, event.getPlayer().getUniqueId());
        //message = sanitizeForEssentials(message);
        event.setFormat(message);
    }

    @EventHandler
    public void onLocalChat(LocalChatEvent event) {
        String message = event.getFormat();
        message = replaceTagPlaceholders(message, event.getPlayer().getUniqueId());
        //message = sanitizeForEssentials(message);
        event.setFormat(message);
    }

    /**
     * Escapes problematic format symbols and ensures UTF-8 safe strings.
     * EssentialsX internally uses String.format(), so we must escape '%' and
     * ensure no invalid color encoding breaks it.
     */
    private String sanitizeForEssentials(String message) {
        if (message == null) return "";

        // Escape '%' because Essentials uses String.format()
        message = message.replace("%", "%%");

        // Force UTF-8 normalization (remove invalid characters)
        byte[] utf8Bytes = message.getBytes(StandardCharsets.UTF_8);
        message = new String(utf8Bytes, StandardCharsets.UTF_8);

        return message;
    }

    private String replaceTagPlaceholders(String text, UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return text;

        String tag = TagFormatter.getFormattedTag(player, TagFormatter.Context.CHAT);

        return text
                .replace("{tag}", tag)
                .replace("{TAG}", tag)
                .replace("{supremetags_tag}", tag);
    }
}