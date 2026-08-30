package net.noscape.project.supremetags.redis;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.storage.DataCache;
import org.bukkit.configuration.file.FileConfiguration;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class RedisUpdateService {

    public static final String EVENT_PLAYER_ACTIVE = "PLAYER_ACTIVE";
    public static final String EVENT_PLAYER_ACTIVE_ALL = "PLAYER_ACTIVE_ALL";
    public static final String EVENT_PLAYER_CUSTOM_TAG = "PLAYER_CUSTOM_TAG";
    public static final String EVENT_PLAYER_FAVOURITES = "PLAYER_FAVOURITES";
    public static final String EVENT_PLAYER_UNLOCKED_TAGS = "PLAYER_UNLOCKED_TAGS";
    public static final String EVENT_PLAYER_TAG_CREDITS = "PLAYER_TAG_CREDITS";
    public static final String EVENT_TAG_CREATED = "TAG_CREATED";
    public static final String EVENT_TAG_UPDATED = "TAG_UPDATED";
    public static final String EVENT_TAG_DELETED = "TAG_DELETED";
    public static final String EVENT_TAG_EDITOR_APPLIED = "TAG_EDITOR_APPLIED";

    private final SupremeTags plugin;
    private final Gson gson = new Gson();
    private final String serverId = UUID.randomUUID().toString();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ExecutorService redisExecutor;
    private final ThreadLocal<Boolean> publishingSuppressed = ThreadLocal.withInitial(() -> false);

    private RedisConfig config;
    private JedisPool pool;
    private volatile JedisPubSub subscriber;
    private volatile long lastFailureLog;

    public RedisUpdateService(SupremeTags plugin) {
        this.plugin = plugin;
        this.redisExecutor = Executors.newFixedThreadPool(2, new RedisThreadFactory());
    }

    public void start() {
        FileConfiguration dataConfig = plugin.getConfigManager().getConfig("data.yml").get();
        config = RedisConfig.from(dataConfig);

        if (!config.enabled()) {
            return;
        }

        if (!running.compareAndSet(false, true)) {
            return;
        }

        try {
            pool = new JedisPool(poolConfig(), new HostAndPort(config.host(), config.port()), clientConfig());
            redisExecutor.execute(this::subscribeLoop);
            plugin.getLogger().info("> Redis: Enabled on channel '" + config.channel() + "'");
        } catch (RuntimeException exception) {
            running.set(false);
            closePool();
            plugin.getLogger().warning("> Redis: Failed to initialize. SupremeTags will continue without Redis. " + safeError(exception));
        }
    }

    public void reload() {
        stop();
        start();
    }

    public void stop() {
        running.set(false);

        JedisPubSub currentSubscriber = subscriber;
        if (currentSubscriber != null) {
            try {
                currentSubscriber.unsubscribe();
            } catch (RuntimeException ignored) {
            }
        }

        closePool();
    }

    public void shutdown() {
        stop();
        redisExecutor.shutdownNow();
        try {
            if (!redisExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("> Redis: Timed out while stopping Redis tasks.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    public boolean isEnabled() {
        return config != null && config.enabled() && running.get();
    }

    public void setPublishingSuppressedForCurrentThread(boolean suppressed) {
        publishingSuppressed.set(suppressed);
    }

    public void publishPlayerActive(UUID uuid, String activeTag) {
        RedisUpdateMessage message = baseMessage(EVENT_PLAYER_ACTIVE);
        message.playerUuid = uuid.toString();
        message.activeTag = activeTag;
        publish(message);
    }

    public void publishPlayerActiveForEveryone(String activeTag) {
        RedisUpdateMessage message = baseMessage(EVENT_PLAYER_ACTIVE_ALL);
        message.activeTag = activeTag;
        publish(message);
    }

    public void publishPlayerCustomTag(UUID uuid, String customTag) {
        RedisUpdateMessage message = baseMessage(EVENT_PLAYER_CUSTOM_TAG);
        message.playerUuid = uuid.toString();
        message.customTag = customTag;
        publish(message);
    }

    public void publishPlayerFavourites(UUID uuid, List<String> favourites) {
        RedisUpdateMessage message = baseMessage(EVENT_PLAYER_FAVOURITES);
        message.playerUuid = uuid.toString();
        message.favourites = favourites;
        publish(message);
    }

    public void publishPlayerUnlockedTags(UUID uuid, List<String> unlockedTags) {
        RedisUpdateMessage message = baseMessage(EVENT_PLAYER_UNLOCKED_TAGS);
        message.playerUuid = uuid.toString();
        message.unlockedTags = unlockedTags;
        publish(message);
    }

    public void publishPlayerTagCredits(UUID uuid, long tagCredits) {
        RedisUpdateMessage message = baseMessage(EVENT_PLAYER_TAG_CREDITS);
        message.playerUuid = uuid.toString();
        message.tagCredits = tagCredits;
        publish(message);
    }

    public void publishTagCreated(Tag tag) {
        publishTagEvent(EVENT_TAG_CREATED, tag);
    }

    public void publishTagUpdated(Tag tag) {
        publishTagEvent(EVENT_TAG_UPDATED, tag);
    }

    public void publishTagDeleted(String identifier) {
        RedisUpdateMessage message = baseMessage(EVENT_TAG_DELETED);
        message.tagIdentifier = identifier;
        publish(message);
    }

    public void publishTagEditorApplied(Collection<Tag> tags) {
        RedisUpdateMessage message = baseMessage(EVENT_TAG_EDITOR_APPLIED);
        if (tags != null) {
            message.tagIdentifiers = tags.stream()
                    .filter(tag -> tag != null && tag.getIdentifier() != null)
                    .map(Tag::getIdentifier)
                    .toList();
        }
        publish(message);
    }

    private void publishTagEvent(String eventType, Tag tag) {
        if (tag == null) {
            return;
        }

        RedisUpdateMessage message = baseMessage(eventType);
        message.tagIdentifier = tag.getIdentifier();
        message.tagText = tag.getTag();
        publish(message);
    }

    private RedisUpdateMessage baseMessage(String eventType) {
        RedisUpdateMessage message = new RedisUpdateMessage();
        message.sourceServer = serverId;
        message.eventType = eventType;
        message.timestamp = System.currentTimeMillis();
        return message;
    }

    private void publish(RedisUpdateMessage message) {
        if (!isEnabled() || pool == null || message == null || Boolean.TRUE.equals(publishingSuppressed.get())) {
            return;
        }

        String payload = gson.toJson(message);
        redisExecutor.execute(() -> {
            try (Jedis jedis = pool.getResource()) {
                jedis.publish(config.channel(), payload);
            } catch (RuntimeException exception) {
                logRedisFailure("publish", exception);
            }
        });
    }

    private void subscribeLoop() {
        while (running.get()) {
            JedisPubSub pubSub = new JedisPubSub() {
                @Override
                public void onMessage(String channel, String message) {
                    handleMessage(message);
                }
            };

            subscriber = pubSub;

            try (Jedis jedis = pool.getResource()) {
                jedis.subscribe(pubSub, config.channel());
            } catch (RuntimeException exception) {
                if (running.get()) {
                    logRedisFailure("subscribe", exception);
                    sleepBeforeReconnect();
                }
            } finally {
                subscriber = null;
            }
        }
    }

    private void handleMessage(String payload) {
        RedisUpdateMessage message;
        try {
            message = gson.fromJson(payload, RedisUpdateMessage.class);
        } catch (JsonSyntaxException exception) {
            plugin.getLogger().warning("> Redis: Ignoring malformed update message.");
            return;
        }

        if (message == null || serverId.equals(message.sourceServer) || message.eventType == null) {
            return;
        }

        switch (message.eventType) {
            case EVENT_PLAYER_ACTIVE -> applyPlayerActive(message);
            case EVENT_PLAYER_ACTIVE_ALL -> clearPlayerCache();
            case EVENT_PLAYER_CUSTOM_TAG -> cachePlayerValue(message.playerUuid, "customtag_", safe(message.customTag));
            case EVENT_PLAYER_FAVOURITES -> cachePlayerValue(message.playerUuid, "favourites_", serialize(message.favourites));
            case EVENT_PLAYER_UNLOCKED_TAGS -> cachePlayerValue(message.playerUuid, "unlockedtags_", serialize(message.unlockedTags));
            case EVENT_PLAYER_TAG_CREDITS -> {
                if (message.tagCredits != null) {
                    cachePlayerValue(message.playerUuid, "tagcredits_", String.valueOf(Math.max(0L, message.tagCredits)));
                }
            }
            case EVENT_TAG_CREATED, EVENT_TAG_UPDATED, EVENT_TAG_DELETED, EVENT_TAG_EDITOR_APPLIED -> refreshDatabaseTags();
            default -> {
            }
        }
    }

    private void applyPlayerActive(RedisUpdateMessage message) {
        if (message.playerUuid == null) {
            return;
        }

        DataCache cache = plugin.getDataCache();
        if (cache != null) {
            cache.cacheData(message.playerUuid, safe(message.activeTag));
        }
    }

    private void cachePlayerValue(String uuid, String prefix, String value) {
        if (uuid == null || uuid.isBlank()) {
            return;
        }

        DataCache cache = plugin.getDataCache();
        if (cache != null) {
            cache.cacheData(prefix + uuid, value);
        }
    }

    private void clearPlayerCache() {
        DataCache cache = plugin.getDataCache();
        if (cache != null) {
            cache.clearCache();
        }
    }

    private void refreshDatabaseTags() {
        if (!plugin.isDBTags() || plugin.getTagManager() == null) {
            return;
        }

        redisExecutor.execute(() -> {
            try {
                plugin.getTagManager().refreshDatabaseTags(false);
                if (plugin.getCategoryManager() != null) {
                    plugin.getCategoryManager().initCategories();
                }
            } catch (RuntimeException exception) {
                plugin.getLogger().warning("> Redis: Failed to refresh database tags after update. " + safeError(exception));
            }
        });
    }

    private JedisPoolConfig poolConfig() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(4);
        poolConfig.setMaxIdle(2);
        poolConfig.setMinIdle(0);
        poolConfig.setTestOnBorrow(true);
        return poolConfig;
    }

    private JedisClientConfig clientConfig() {
        DefaultJedisClientConfig.Builder builder = DefaultJedisClientConfig.builder()
                .connectionTimeoutMillis(2000)
                .socketTimeoutMillis(0)
                .database(config.database())
                .ssl(config.ssl());

        if (!isBlank(config.username())) {
            builder.user(config.username());
        }

        if (!isBlank(config.password())) {
            builder.password(config.password());
        }

        return builder.build();
    }

    private void closePool() {
        JedisPool currentPool = pool;
        pool = null;
        if (currentPool != null) {
            try {
                currentPool.close();
            } catch (RuntimeException ignored) {
            }
        }
    }

    private void logRedisFailure(String action, RuntimeException exception) {
        long now = System.currentTimeMillis();
        if (now - lastFailureLog < 30000L) {
            return;
        }

        lastFailureLog = now;
        plugin.getLogger().warning("> Redis: Unable to " + action + ". SupremeTags will continue using the database. " + safeError(exception));
    }

    private String safeError(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }

        message = redact(message, config != null ? config.username() : null);
        message = redact(message, config != null ? config.password() : null);
        return exception.getClass().getSimpleName() + ": " + message;
    }

    private String redact(String message, String credential) {
        if (credential == null || credential.isBlank()) {
            return message;
        }

        return message.replace(credential, "[redacted]");
    }

    private void sleepBeforeReconnect() {
        try {
            Thread.sleep(5000L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private String serialize(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }

        return String.join(",", values);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static class RedisThreadFactory implements ThreadFactory {
        private int threadCount;

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "SupremeTags-Redis-" + (++threadCount));
            thread.setDaemon(true);
            return thread;
        }
    }
}
