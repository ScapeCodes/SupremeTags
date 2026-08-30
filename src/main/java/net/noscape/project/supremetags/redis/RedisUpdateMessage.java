package net.noscape.project.supremetags.redis;

import java.util.List;

class RedisUpdateMessage {

    String version = "1";
    String sourceServer;
    String eventType;
    String playerUuid;
    String activeTag;
    String customTag;
    List<String> favourites;
    List<String> unlockedTags;
    Long tagCredits;
    String tagIdentifier;
    List<String> tagIdentifiers;
    List<String> tagText;
    long timestamp;
}
