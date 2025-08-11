package net.noscape.project.supremetags.handlers.hooks;

import net.essentialsx.api.v2.events.chat.GlobalChatEvent;
import net.essentialsx.api.v2.events.chat.LocalChatEvent;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.handlers.Variant;
import net.noscape.project.supremetags.storage.UserData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;

import static net.noscape.project.supremetags.utils.Utils.format;
import static net.noscape.project.supremetags.utils.Utils.replacePlaceholders;

public class EssentialsChatListener implements Listener {

    @EventHandler
    public void onGlobalChat(GlobalChatEvent event) {
        String message = event.getFormat(); // Get EssentialsX's already-built chat format
        message = replaceTagPlaceholders(message, event.getPlayer().getUniqueId());
        event.setFormat(message);
    }

    @EventHandler
    public void onLocalChat(LocalChatEvent event) {
        String message = event.getFormat();
        message = replaceTagPlaceholders(message, event.getPlayer().getUniqueId());
        event.setFormat(message);
    }

    private String replaceTagPlaceholders(String text, UUID uuid) {
        if (uuid == null) return text;

        String activeTag = UserData.getActive(uuid);
        String displayTag = SupremeTags.getInstance().getConfig().getString("placeholders.chat.none-output");

        Tag tag = SupremeTags.getInstance().getTagManager().getTags().get(activeTag);
        Tag personalTag = SupremeTags.getInstance().getPlayerManager().loadAllPlayerTags(uuid).get(activeTag);
        Variant var = SupremeTags.getInstance().getTagManager().getVariantTag(Bukkit.getPlayer(uuid));

        if (tag != null && tag.getTag() != null) {
            displayTag = tag.getCurrentTag() != null ? tag.getCurrentTag() : tag.getTag().get(0);
        } else if (personalTag != null) {
            displayTag = personalTag.getTag().get(0);
        } else if (var != null) {
            displayTag = var.getTag().get(0);
        }

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            displayTag = replacePlaceholders(player, displayTag);
        }

        displayTag = format(displayTag);

        String formatted = SupremeTags.getInstance().getConfig().getString("placeholders.chat.format");
        formatted = formatted.replace("%tag%", displayTag);

        return text
                .replace("{tag}", formatted)
                .replace("{TAG}", formatted)
                .replace("{supremetags_tag}", formatted);
    }
}