package net.noscape.project.supremetags.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.noscape.project.supremetags.SupremeTags;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.UUID;

public class SkullUtil {

    private static final Gson GSON = new Gson();

    @NotNull
    public static String getEncoded(@NotNull final String url) {
        final byte[] encodedData = Base64.getEncoder().encode(String
                .format("{textures:{SKIN:{url:\"%s\"}}}", "https://textures.minecraft.net/texture/" + url)
                .getBytes());
        return new String(encodedData);
    }

    @NotNull
    public static ItemStack getSkullByBase64EncodedTextureUrl(@NotNull final SupremeTags plugin, @NotNull final String base64Url) {
        final ItemStack head = plugin.getHead().clone();
        if (base64Url.isEmpty()) {
            return head;
        }

        final SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        if (headMeta == null) {
            return head;
        }

        final GameProfile profile = getGameProfile(base64Url);
        final Field profileField;
        try {
            profileField = headMeta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(headMeta, profile);
        } catch (final NoSuchFieldException | IllegalArgumentException | IllegalAccessException ignored) {

        }
        head.setItemMeta(headMeta);
        return head;
    }

    public static String getTextureFromSkull(final SupremeTags plugin, ItemStack item) {
        if (!(item.getItemMeta() instanceof SkullMeta)) return null;
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        GameProfile profile;
        try {
            final Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profile = (GameProfile) profileField.get(meta);
        } catch (final NoSuchFieldException | IllegalArgumentException | IllegalAccessException exception) {
            return null;
        }

        for (Property property : profile.getProperties().get("textures")) {
            if (property.getName().equals("textures")) {
                return decodeSkinUrl(property.getValue());
            }
        }
        return null;
    }

    @NotNull
    public static ItemStack getSkullByName(@NotNull final SupremeTags plugin, @NotNull final String playerName) {
        final ItemStack head = plugin.getHead().clone();
        if (playerName.isEmpty()) {
            return head;
        }

        final SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        if (headMeta == null) {
            return head;
        }

        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);

        headMeta.setOwner(offlinePlayer.getName());

        head.setItemMeta(headMeta);
        return head;
    }

    public static String getSkullOwner(ItemStack skull) {
        if (skull == null || !(skull.getItemMeta() instanceof SkullMeta)) return null;
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        if (meta.getOwningPlayer() == null) return null;
        return meta.getOwningPlayer().getName();
    }

    @NotNull
    private static GameProfile getGameProfile(@NotNull final String base64Url) {
        GameProfile profile = new GameProfile(UUID.randomUUID(), "");
        profile.getProperties().put("textures", new Property("textures", base64Url));
        return profile;
    }

    @Nullable
    public static String decodeSkinUrl(@NotNull final String base64Texture) {
        final String decoded = new String(Base64.getDecoder().decode(base64Texture));
        final JsonObject object = GSON.fromJson(decoded, JsonObject.class);

        final JsonElement textures = object.get("textures");

        if (textures == null) {
            return null;
        }

        final JsonElement skin = textures.getAsJsonObject().get("SKIN");

        if (skin == null) {
            return null;
        }

        final JsonElement url = skin.getAsJsonObject().get("url");
        return url == null ? null : url.getAsString();
    }
}
