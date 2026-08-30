package net.noscape.project.supremetags.managers;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class FileSyncingManager implements PluginMessageListener {

    private static final String LEGACY_CHANNEL = "BungeeCord";
    private static final String MODERN_CHANNEL = "bungeecord:main";
    private static final String[] MESSENGER_CHANNELS = {LEGACY_CHANNEL, MODERN_CHANNEL};
    private static final String FORWARD_CHANNEL = "SupremeTagsFileSync";
    private static final String ACTION_TAG_FILES = "SYNC_TAG_FILES";
    private static final String ACTION_REQUEST_TAG_FILES = "REQUEST_TAG_FILES";
    private static final int MAX_CHUNK_SIZE = 24000;

    private final SupremeTags plugin;
    private final UUID serverId = UUID.randomUUID();
    private final Map<UUID, IncomingSync> incomingSyncs = new ConcurrentHashMap<>();
    private final Set<UUID> processedSyncs = ConcurrentHashMap.newKeySet();
    private volatile boolean applyingIncomingSync;
    private volatile boolean pendingSync;

    public FileSyncingManager(SupremeTags plugin) {
        this.plugin = plugin;
    }

    public void registerChannels() {
        for (String channel : MESSENGER_CHANNELS) {
            Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, channel);
            Bukkit.getMessenger().registerIncomingPluginChannel(plugin, channel, this);
        }
    }

    public void unregisterChannels() {
        for (String channel : MESSENGER_CHANNELS) {
            Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, channel);
            Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, channel, this);
        }
        incomingSyncs.clear();
        processedSyncs.clear();
        pendingSync = false;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("settings.proxy-file-syncing", false);
    }

    public void syncTagFiles() {
        if (!isEnabled() || applyingIncomingSync || plugin.getTagManager() == null || plugin.getTagManager().isDBTags()) {
            return;
        }

        sendPluginMessageWhenPossible(this::sendTagFilesNow);
    }

    private void sendTagFilesNow(Player player) {
        if (!isEnabled() || applyingIncomingSync || plugin.getTagManager() == null || plugin.getTagManager().isDBTags()) {
            return;
        }

        pendingSync = false;

        Utils.runAsync(() -> {
            try {
                byte[] zippedFiles = zipTagFiles();
                UUID syncId = UUID.randomUUID();
                int totalChunks = Math.max(1, (int) Math.ceil(zippedFiles.length / (double) MAX_CHUNK_SIZE));
                plugin.getLogger().info("[SupremeTags] Sending proxy tag file sync " + syncId + " in " + totalChunks + " chunk(s).");

                for (int index = 0; index < totalChunks; index++) {
                    int start = index * MAX_CHUNK_SIZE;
                    int end = Math.min(zippedFiles.length, start + MAX_CHUNK_SIZE);
                    byte[] chunk = Arrays.copyOfRange(zippedFiles, start, end);
                    byte[] payload = createPayload(syncId, index, totalChunks, chunk);

                    Utils.runMain(() -> sendForwardMessage(player, payload));
                }
            } catch (IOException exception) {
                pendingSync = true;
                plugin.getLogger().warning("[SupremeTags] Failed to prepare tag files for proxy syncing: " + exception.getMessage());
            }
        });
    }

    private void sendPluginMessageWhenPossible(java.util.function.Consumer<Player> sender) {
        Player player = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if (player != null) {
            sender.accept(player);
            return;
        }

        pendingSync = true;
        plugin.getLogger().warning("[SupremeTags] Could not sync tag files across the proxy because no players are online to send a plugin message. Retrying shortly.");

        for (long delay : new long[]{20L, 100L, 200L, 600L}) {
            Utils.runMainLater(() -> {
                if (!pendingSync || applyingIncomingSync || !isEnabled()) {
                    return;
                }

                Player retryPlayer = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
                if (retryPlayer == null) {
                    return;
                }

                sender.accept(retryPlayer);
            }, delay);
        }
    }

    private void requestTagFilesSync(Player player) {
        if (!isEnabled() || plugin.getTagManager() == null || plugin.getTagManager().isDBTags()) {
            return;
        }

        if (player == null || !player.isOnline()) {
            pendingSync = true;
            return;
        }

        try {
            byte[] payload = createRequestPayload();
            sendForwardMessage(player, payload);
        } catch (IOException exception) {
            plugin.getLogger().warning("[SupremeTags] Failed to request proxy tag file sync: " + exception.getMessage());
        }
    }

    public void flushPendingSync() {
        if (pendingSync) {
            syncTagFiles();
        }
    }

    public void requestTagFilesSync() {
        sendPluginMessageWhenPossible(this::requestTagFilesSync);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!isMessengerChannel(channel) || !isEnabled()) {
            return;
        }

        ByteArrayDataInput input = ByteStreams.newDataInput(message);
        String subChannel = input.readUTF();
        if (!subChannel.equals(FORWARD_CHANNEL)) {
            return;
        }

        int length = input.readUnsignedShort();
        byte[] payload = new byte[length];
        input.readFully(payload);

        handlePayload(payload);
    }

    private void handlePayload(byte[] payload) {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
            String action = input.readUTF();
            if (!action.equals(ACTION_TAG_FILES) && !action.equals(ACTION_REQUEST_TAG_FILES)) {
                return;
            }

            UUID origin = UUID.fromString(input.readUTF());
            if (origin.equals(serverId)) {
                return;
            }

            if (action.equals(ACTION_REQUEST_TAG_FILES)) {
                plugin.getLogger().info("[SupremeTags] Received proxy tag file sync request from another server.");
                syncTagFiles();
                return;
            }

            UUID syncId = UUID.fromString(input.readUTF());
            if (processedSyncs.contains(syncId)) {
                return;
            }

            int chunkIndex = input.readInt();
            int totalChunks = input.readInt();
            int length = input.readInt();
            byte[] chunk = new byte[length];
            input.readFully(chunk);

            IncomingSync incoming = incomingSyncs.computeIfAbsent(syncId, id -> new IncomingSync(totalChunks));
            incoming.addChunk(chunkIndex, chunk);

            if (incoming.isComplete()) {
                incomingSyncs.remove(syncId);
                processedSyncs.add(syncId);
                byte[] zippedFiles = incoming.combine();
                plugin.getLogger().info("[SupremeTags] Received complete proxy tag file sync " + syncId + ". Applying files.");
                Utils.runAsync(() -> applyIncomingTagFiles(zippedFiles));
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("[SupremeTags] Failed to handle incoming proxy tag file sync: " + exception.getMessage());
        }
    }

    private void sendForwardMessage(Player player, byte[] payload) {
        if (!player.isOnline()) {
            pendingSync = true;
            return;
        }

        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeUTF("Forward");
        output.writeUTF("ALL");
        output.writeUTF(FORWARD_CHANNEL);
        if (payload.length > 65535) {
            plugin.getLogger().warning("[SupremeTags] Proxy tag sync payload chunk is too large to send: " + payload.length + " bytes.");
            pendingSync = true;
            return;
        }
        output.writeShort(payload.length);
        output.write(payload);
        byte[] message = output.toByteArray();
        for (String channel : MESSENGER_CHANNELS) {
            if (Bukkit.getMessenger().isOutgoingChannelRegistered(plugin, channel)) {
                player.sendPluginMessage(plugin, channel, message);
            }
        }
    }

    private byte[] createPayload(UUID syncId, int chunkIndex, int totalChunks, byte[] chunk) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(byteStream)) {
            output.writeUTF(ACTION_TAG_FILES);
            output.writeUTF(serverId.toString());
            output.writeUTF(syncId.toString());
            output.writeInt(chunkIndex);
            output.writeInt(totalChunks);
            output.writeInt(chunk.length);
            output.write(chunk);
        }
        return byteStream.toByteArray();
    }

    private byte[] createRequestPayload() throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(byteStream)) {
            output.writeUTF(ACTION_REQUEST_TAG_FILES);
            output.writeUTF(serverId.toString());
        }
        return byteStream.toByteArray();
    }

    private byte[] zipTagFiles() throws IOException {
        File tagsFolder = getTagsFolder();
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

        try (ZipOutputStream zipOutput = new ZipOutputStream(byteStream, StandardCharsets.UTF_8)) {
            File[] files = tagsFolder.listFiles();
            if (files != null) {
                addFilesToZip(tagsFolder, files, zipOutput);
            }
        }

        return byteStream.toByteArray();
    }

    private void addFilesToZip(File root, File[] files, ZipOutputStream zipOutput) throws IOException {
        Arrays.sort(files, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        for (File file : files) {
            if (file.isDirectory()) {
                File[] childFiles = file.listFiles();
                if (childFiles != null) {
                    addFilesToZip(root, childFiles, zipOutput);
                }
                continue;
            }

            if (!file.getName().toLowerCase().endsWith(".yml")) {
                continue;
            }

            String relativePath = root.toPath().relativize(file.toPath()).toString().replace(File.separatorChar, '/');
            zipOutput.putNextEntry(new ZipEntry(relativePath));
            Files.copy(file.toPath(), zipOutput);
            zipOutput.closeEntry();
        }
    }

    private void applyIncomingTagFiles(byte[] zippedFiles) {
        applyingIncomingSync = true;
        try {
            File tagsFolder = getTagsFolder();
            if (!tagsFolder.exists() && !tagsFolder.mkdirs()) {
                plugin.getLogger().warning("[SupremeTags] Could not create tags folder for incoming proxy file sync.");
                applyingIncomingSync = false;
                return;
            }

            try (ZipInputStream zipInput = new ZipInputStream(new ByteArrayInputStream(zippedFiles), StandardCharsets.UTF_8)) {
                ZipEntry entry;
                while ((entry = zipInput.getNextEntry()) != null) {
                    if (entry.isDirectory() || !entry.getName().toLowerCase().endsWith(".yml")) {
                        zipInput.closeEntry();
                        continue;
                    }

                    File destination = new File(tagsFolder, entry.getName());
                    String rootPath = tagsFolder.getCanonicalPath();
                    String destinationPath = destination.getCanonicalPath();
                    if (!destinationPath.startsWith(rootPath + File.separator) && !destinationPath.equals(rootPath)) {
                        zipInput.closeEntry();
                        continue;
                    }

                    File parent = destination.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }
                    Files.copy(zipInput, destination.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    zipInput.closeEntry();
                }
            }

            Utils.runMain(() -> {
                try {
                    plugin.getConfigManager().reloadTagConfigs();
                    plugin.getTagManager().unloadTags();
                    plugin.getTagManager().loadTags(true);
                    plugin.getTagManager().getDataItem().clear();
                    plugin.getCategoryManager().initCategories();
                    plugin.getLogger().info("[SupremeTags] Synced tag files from another proxy server.");
                } finally {
                    applyingIncomingSync = false;
                }
            });
        } catch (IOException exception) {
            applyingIncomingSync = false;
            plugin.getLogger().warning("[SupremeTags] Failed to apply incoming proxy tag file sync: " + exception.getMessage());
        }
    }

    private File getTagsFolder() {
        return new File(plugin.getDataFolder(), "tags");
    }

    private boolean isMessengerChannel(String channel) {
        for (String messengerChannel : MESSENGER_CHANNELS) {
            if (messengerChannel.equalsIgnoreCase(channel)) {
                return true;
            }
        }
        return false;
    }

    private static class IncomingSync {
        private final byte[][] chunks;

        private IncomingSync(int totalChunks) {
            this.chunks = new byte[totalChunks][];
        }

        private void addChunk(int index, byte[] chunk) {
            if (index >= 0 && index < chunks.length) {
                chunks[index] = chunk;
            }
        }

        private boolean isComplete() {
            for (byte[] chunk : chunks) {
                if (chunk == null) {
                    return false;
                }
            }
            return true;
        }

        private byte[] combine() throws IOException {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            for (byte[] chunk : chunks) {
                output.write(chunk);
            }
            return output.toByteArray();
        }
    }
}
