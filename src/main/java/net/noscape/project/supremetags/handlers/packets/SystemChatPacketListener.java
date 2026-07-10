package net.noscape.project.supremetags.handlers.packets;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.google.gson.*;

import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.handlers.Variant;
import net.noscape.project.supremetags.storage.UserData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;

import static net.noscape.project.supremetags.utils.Utils.format;
import static net.noscape.project.supremetags.utils.Utils.replacePlaceholders;

public class SystemChatPacketListener extends PacketAdapter {

    public SystemChatPacketListener(Plugin plugin) {
        super(plugin, ListenerPriority.MONITOR,
                PacketType.Play.Server.CHAT,
                PacketType.Play.Server.SYSTEM_CHAT);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        Player viewer = event.getPlayer(); // Player receiving the packet

        WrappedChatComponent chatComponent = packet.getChatComponents().readSafely(0);
        if (chatComponent != null) {
            String messageJson = chatComponent.getJson();
            try {
                JsonObject jsonObject = JsonParser.parseString(messageJson).getAsJsonObject();

                // Try to detect sender
                String senderName = extractSenderFromJson(jsonObject);
                Player sender = senderName != null ? Bukkit.getPlayerExact(senderName) : null;
                UUID senderUUID = sender != null ? sender.getUniqueId() : null;

                // Always replace placeholders (fallback to viewer if sender unknown)
                replacePlaceholdersInJson(jsonObject, senderUUID, viewer);

                // Write modified JSON back
                String replacedJson = jsonObject.toString();
                packet.getChatComponents().write(0, WrappedChatComponent.fromJson(replacedJson));
            } catch (Exception e) {
                // Bukkit.getLogger().warning("[SupremeTags] Failed to parse chat JSON: " + e.getMessage());
            }
        }
    }

    private void replacePlaceholdersInJson(JsonElement element, UUID senderUUID, Player viewer) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString()) {
                    String original = entry.getValue().getAsString();
                    String replaced = replaceTagPlaceholders(original, senderUUID, viewer);
                    obj.addProperty(entry.getKey(), replaced);
                } else {
                    replacePlaceholdersInJson(entry.getValue(), senderUUID, viewer);
                }
            }
        } else if (element.isJsonArray()) {
            for (JsonElement item : element.getAsJsonArray()) {
                replacePlaceholdersInJson(item, senderUUID, viewer);
            }
        }
    }

    private String replaceTagPlaceholders(String text, UUID uuid, Player viewer) {
        if (uuid == null) uuid = viewer.getUniqueId(); // Fallback to viewer

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

        displayTag = replacePlaceholders(Bukkit.getPlayer(uuid), displayTag);
        displayTag = format(displayTag);

        String formatted = SupremeTags.getInstance().getConfig().getString("placeholders.chat.format");
        formatted = formatted.replace("%tag%", displayTag);

        return format(text
                .replace("{tag}", formatted)
                .replace("{TAG}", formatted)
                .replace("{supremetags_tag}", formatted));
    }

    private String extractSenderFromJson(JsonObject jsonObject) {
        // Try extra[] array first
        if (jsonObject.has("extra") && jsonObject.get("extra").isJsonArray()) {
            JsonArray extras = jsonObject.getAsJsonArray("extra");
            for (JsonElement element : extras) {
                if (element.isJsonObject()) {
                    JsonObject obj = element.getAsJsonObject();
                    if (obj.has("text")) {
                        String text = obj.get("text").getAsString().trim();
                        if (!text.isEmpty() && Bukkit.getPlayerExact(text) != null) {
                            return text;
                        }
                    }
                }
            }
        }

        // Fallback for "<Name>: message" style
        if (jsonObject.has("text")) {
            String raw = jsonObject.get("text").getAsString();
            if (raw.contains(":")) {
                String nameGuess = raw.split(":")[0].replace("<", "").replace(">", "").trim();
                if (!nameGuess.isEmpty() && Bukkit.getPlayerExact(nameGuess) != null) {
                    return nameGuess;
                }
            }
        }

        return null;
    }
}
