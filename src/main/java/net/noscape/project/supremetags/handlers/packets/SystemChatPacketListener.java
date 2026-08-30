package net.noscape.project.supremetags.handlers.packets;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.google.gson.*;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.noscape.project.supremetags.handlers.TagFormatter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public class SystemChatPacketListener extends PacketAdapter {

    public SystemChatPacketListener(Plugin plugin) {
        super(plugin, ListenerPriority.MONITOR,
                PacketType.Play.Server.CHAT,
                PacketType.Play.Server.SYSTEM_CHAT);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        Player viewer = event.getPlayer();

        WrappedChatComponent chatComponent = packet.getChatComponents().readSafely(0);
        if (chatComponent != null) {
            String messageJson = chatComponent.getJson();
            try {
                JsonObject jsonObject = JsonParser.parseString(messageJson).getAsJsonObject();

                String senderName = extractSenderFromJson(jsonObject);
                Player sender = senderName != null ? Bukkit.getPlayerExact(senderName) : null;
                UUID senderUUID = sender != null ? sender.getUniqueId() : null;

                Component component = GsonComponentSerializer.gson().deserialize(messageJson);
                Component replacedComponent = replaceTagPlaceholders(component, senderUUID, viewer);

                String replacedJson = GsonComponentSerializer.gson().serialize(replacedComponent);
                packet.getChatComponents().write(0, WrappedChatComponent.fromJson(replacedJson));
            } catch (Exception e) {

            }
        }
    }

    private Component replaceTagPlaceholders(Component component, UUID uuid, Player viewer) {
        if (uuid == null) uuid = viewer.getUniqueId();

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
