package net.noscape.project.supremetags.handlers.packets;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.plugin.Plugin;

public class ProtocolLibHandler {

    private ProtocolManager protocolManager;
    private SystemChatPacketListener listener;

    public ProtocolLibHandler(Plugin plugin) {
        this.listener = new SystemChatPacketListener(plugin);
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    public void register() {
        protocolManager.addPacketListener(listener);
    }

    public void unRegister() {
        protocolManager.removePacketListener(listener);
    }
}