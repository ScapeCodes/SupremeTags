package net.noscape.project.supremetags.handlers.packets;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.chat.message.ChatMessage;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChatMessage;

import com.google.gson.*;

import net.noscape.project.supremetags.handlers.TagFormatter;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PacketEventsChatListener implements PacketListener {

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.CHAT_MESSAGE &&
                event.getPacketType() != PacketType.Play.Server.SYSTEM_CHAT_MESSAGE) return;

        WrapperPlayServerChatMessage packet = new WrapperPlayServerChatMessage(event);

        try {

            Component originalComponent = packet.getMessage().getChatContent();
            if (originalComponent == null) return;

            String json = GsonComponentSerializer.gson().serialize(originalComponent);

            JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();

            String senderName = extractSenderFromJson(jsonObject);
            Player sender = senderName != null ? Bukkit.getPlayerExact(senderName) : null;
            UUID senderUUID = (sender != null) ? sender.getUniqueId() : null;

            Component modifiedComponent = replaceTagPlaceholders(originalComponent, senderUUID);

            packet.setMessage((ChatMessage) modifiedComponent);

        } catch (Exception ignored) {}
    }

    private Component replaceTagPlaceholders(Component component, UUID uuid) {
        if (uuid == null) return component;

        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return component;

        Component tag = TagFormatter.getFormattedTagComponent(player, TagFormatter.Context.CHAT);

        return component
                .replaceText(TextReplacementConfig.builder().matchLiteral("{tag}").replacement(tag).build())
                .replaceText(TextReplacementConfig.builder().matchLiteral("{TAG}").replacement(tag).build())
                .replaceText(TextReplacementConfig.builder().matchLiteral("{supremetags_tag}").replacement(tag).build());
    }

    private String extractSenderFromJson(JsonObject jsonObject) {
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
