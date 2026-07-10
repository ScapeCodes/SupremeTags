package net.noscape.project.supremetags.handlers.packets;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;

public class PacketEventsHandler {

    public PacketEventsHandler() {
        register();
    }

    public void register() {
        PacketEvents.getAPI().getEventManager().registerListener(
                new PacketEventsChatListener(), PacketListenerPriority.NORMAL);
    }

    public void unRegister() {
        PacketEvents.getAPI().getEventManager().unregisterAllListeners();
    }

}