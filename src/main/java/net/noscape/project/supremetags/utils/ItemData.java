package net.noscape.project.supremetags.utils;

import net.noscape.project.supremetags.SupremeTags;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

public final class ItemData {

    private static final String SIGNATURE_SUFFIX = "_signature";
    private static final Set<String> REGISTERED_KEYS = new HashSet<>();
    private static volatile byte[] secret;

    static {
        register(
                "category",
                "configPath",
                "configSectionBack",
                "custom-item",
                "identifier",
                "isVariant",
                "name",
                "owner",
                "supremetags_click_commands",
                "variant_identifier",
                "voucher_identifier"
        );
    }

    private ItemData() {
    }

    public static void register(String... keys) {
        if (keys == null) return;

        for (String key : keys) {
            REGISTERED_KEYS.add(normalize(key));
        }
    }

    public static boolean has(ItemStack item, String key) {
        ItemMeta meta = meta(item);
        if (meta == null) return false;

        String normalized = requireRegistered(key);
        PersistentDataContainer container = meta.getPersistentDataContainer();

        String value = container.get(key(normalized), PersistentDataType.STRING);
        if (value != null && verify(normalized, value, container)) return true;

        Byte bool = container.get(key(normalized), PersistentDataType.BYTE);
        return bool != null && verify(normalized, Boolean.toString(bool == 1), container);
    }

    public static String getString(ItemStack item, String key) {
        ItemMeta meta = meta(item);
        if (meta == null) return "";

        String normalized = requireRegistered(key);
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String value = container.get(key(normalized), PersistentDataType.STRING);

        if (value == null || !verify(normalized, value, container)) {
            return "";
        }

        return value;
    }

    public static void setString(ItemStack item, String key, String value) {
        if (item == null) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String normalized = requireRegistered(key);
        String safeValue = value == null ? "" : value;
        PersistentDataContainer container = meta.getPersistentDataContainer();

        container.set(key(normalized), PersistentDataType.STRING, safeValue);
        container.set(signatureKey(normalized), PersistentDataType.STRING, sign(normalized, safeValue));
        item.setItemMeta(meta);
    }

    public static boolean getBoolean(ItemStack item, String key) {
        ItemMeta meta = meta(item);
        if (meta == null) return false;

        String normalized = requireRegistered(key);
        PersistentDataContainer container = meta.getPersistentDataContainer();
        Byte value = container.get(key(normalized), PersistentDataType.BYTE);

        return value != null
                && verify(normalized, Boolean.toString(value == 1), container)
                && value == 1;
    }

    public static void setBoolean(ItemStack item, String key, boolean value) {
        if (item == null) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String normalized = requireRegistered(key);
        String payload = Boolean.toString(value);
        PersistentDataContainer container = meta.getPersistentDataContainer();

        container.set(key(normalized), PersistentDataType.BYTE, (byte) (value ? 1 : 0));
        container.set(signatureKey(normalized), PersistentDataType.STRING, sign(normalized, payload));
        item.setItemMeta(meta);
    }

    public static String getVoucherIdentifier(ItemStack item) {
        return getString(item, "voucher_identifier");
    }

    public static void setVoucherIdentifier(ItemStack item, String identifier) {
        setString(item, "voucher_identifier", identifier);
    }

    private static boolean verify(String key, String value, PersistentDataContainer container) {
        String signature = container.get(signatureKey(key), PersistentDataType.STRING);
        if (signature == null || signature.isEmpty()) return false;

        byte[] expected = sign(key, value).getBytes(StandardCharsets.UTF_8);
        byte[] actual = signature.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    private static String sign(String key, String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret(), "HmacSHA256"));
            byte[] digest = mac.doFinal((key + "\0" + value).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign item data", e);
        }
    }

    private static byte[] secret() {
        byte[] current = secret;
        if (current != null) return current;

        synchronized (ItemData.class) {
            if (secret != null) return secret;

            Path path = SupremeTags.getInstance().getDataFolder().toPath().resolve("item-data.key");
            try {
                Files.createDirectories(path.getParent());
                if (Files.exists(path)) {
                    secret = Base64.getDecoder().decode(Files.readString(path).trim());
                    return secret;
                }

                byte[] generated = new byte[32];
                new SecureRandom().nextBytes(generated);
                Files.writeString(path, Base64.getEncoder().encodeToString(generated));
                secret = generated;
                return secret;
            } catch (IOException | IllegalArgumentException e) {
                throw new IllegalStateException("Unable to load item data secret", e);
            }
        }
    }

    private static ItemMeta meta(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta();
    }

    private static NamespacedKey key(String key) {
        return new NamespacedKey(SupremeTags.getInstance(), key);
    }

    private static NamespacedKey signatureKey(String key) {
        return key(key + SIGNATURE_SUFFIX);
    }

    private static String requireRegistered(String key) {
        String normalized = normalize(key);
        if (!REGISTERED_KEYS.contains(normalized)) {
            throw new IllegalArgumentException("Unregistered item data key: " + key);
        }

        return normalized;
    }

    private static String normalize(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Item data key cannot be blank");
        }

        return key.toLowerCase().replace(':', '_');
    }
}
