package net.noscape.project.supremetags.handlers.packets;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChatMessage;
import com.google.gson.*;

import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.handlers.Variant;
import net.noscape.project.supremetags.storage.UserData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

import static net.noscape.project.supremetags.utils.Utils.*;

public class PacketEventsChatListener implements PacketListener {

    @SuppressWarnings("deprecation")
    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.CHAT_MESSAGE &&
                event.getPacketType() != PacketType.Play.Server.SYSTEM_CHAT_MESSAGE) return;

        WrapperPlayServerChatMessage packet = new WrapperPlayServerChatMessage(event);

        try {
            String json = packet.readComponentJSON();  // deprecated
            if (json == null) return;

            JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();

            String senderName = extractSenderFromJson(jsonObject);
            Player sender = senderName != null ? Bukkit.getPlayerExact(senderName) : null;
            UUID senderUUID = (sender != null) ? sender.getUniqueId() : null;

            replacePlaceholdersInJson(jsonObject, senderUUID);

            String replacedJson = jsonObject.toString();

            packet.writeComponentJSON(replacedJson);  // deprecated
        } catch (Exception ignored) {}
    }


    private void replacePlaceholdersInJson(JsonElement element, UUID senderUUID) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString()) {
                    String original = entry.getValue().getAsString();
                    String replaced = replaceTagPlaceholders(original, senderUUID);
                    obj.addProperty(entry.getKey(), replaced);
                } else {
                    replacePlaceholdersInJson(entry.getValue(), senderUUID);
                }
            }
        } else if (element.isJsonArray()) {
            for (JsonElement item : element.getAsJsonArray()) {
                replacePlaceholdersInJson(item, senderUUID);
            }
        }
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

        displayTag = replacePlaceholders(Bukkit.getPlayer(uuid), displayTag);
        displayTag = format(displayTag);

        String formatted = SupremeTags.getInstance().getConfig().getString("placeholders.chat.format");
        formatted = formatted.replace("%tag%", displayTag);

        return text
                .replace("{tag}", formatted)
                .replace("{TAG}", formatted)
                .replace("{supremetags_tag}", formatted);
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
