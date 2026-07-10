package net.noscape.project.supremetags.utils;

import net.noscape.project.supremetags.SupremeTags;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.logging.Logger;

public class BungeeMessaging implements PluginMessageListener {

    private static final Logger LOGGER = Logger.getLogger("SupremeTags/PluginMessenger");
    private static final String BUNGEE_CHANNEL = "BungeeCord";
    private static final String VELOCITY_CHANNEL = "velocity:plugin";
    private static final String CUSTOM_SUBCHANNEL = "supremetags:reload";

    public BungeeMessaging() {}

    public static void sendReload() {
        if (!SupremeTags.getInstance().getConfig().getBoolean("settings.bungee-messaging")) {
            return;
        }

        Player player = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if (player == null) {
            return;
        }

        try (
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(stream);
                ByteArrayOutputStream msgBytes = new ByteArrayOutputStream();
                DataOutputStream msgOut = new DataOutputStream(msgBytes)
        ) {
            msgOut.writeUTF("reload");

            if (SupremeTags.getInstance().isBungeeCord()) {
                out.writeUTF("Forward");
                out.writeUTF("ALL");
                out.writeUTF(CUSTOM_SUBCHANNEL);
                out.writeShort(msgBytes.toByteArray().length);
                out.write(msgBytes.toByteArray());

                player.sendPluginMessage(SupremeTags.getInstance(), BUNGEE_CHANNEL, stream.toByteArray());
            } else {
                player.sendPluginMessage(SupremeTags.getInstance(), VELOCITY_CHANNEL, msgBytes.toByteArray());
            }

        } catch (IOException ex) {
            LOGGER.severe("Failed to send reload message: " + ex.getMessage());
        }
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte[] message) {
        if (SupremeTags.getInstance().isBungeeCord() && !channel.equalsIgnoreCase(BUNGEE_CHANNEL)) return;
        if (!SupremeTags.getInstance().isBungeeCord() && !channel.equalsIgnoreCase(VELOCITY_CHANNEL)) return;

        try {
            if (SupremeTags.getInstance().isBungeeCord()) {
                try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
                    String subChannel = in.readUTF();
                    short len = in.readShort();
                    byte[] data = new byte[len];
                    in.readFully(data);

                    try (DataInputStream msgIn = new DataInputStream(new ByteArrayInputStream(data))) {
                        handleMessage(msgIn);
                    }
                }
            } else {
                // Velocity: no Forward structure, just the message
                try (DataInputStream msgIn = new DataInputStream(new ByteArrayInputStream(message))) {
                    handleMessage(msgIn);
                }
            }

        } catch (IOException ex) {
            LOGGER.severe("Failed to read plugin message: " + ex.getMessage());
        }
    }

    private void handleMessage(DataInputStream in) throws IOException {
        String command = in.readUTF();
        if (command.equalsIgnoreCase("reload")) {
            SupremeTags.getInstance().reload();
        }
    }

    public static void registerChannels() {
        SupremeTags plugin = SupremeTags.getInstance();
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, BUNGEE_CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, BUNGEE_CHANNEL, new BungeeMessaging());

        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, VELOCITY_CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, VELOCITY_CHANNEL, new BungeeMessaging());
    }
}
