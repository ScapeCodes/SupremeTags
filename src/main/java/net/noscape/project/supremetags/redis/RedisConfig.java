package net.noscape.project.supremetags.redis;

import org.bukkit.configuration.file.FileConfiguration;

public class RedisConfig {

    private static final String BASE_PATH = "data.redis.";

    private final boolean enabled;
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final int database;
    private final boolean ssl;
    private final String channel;

    private RedisConfig(boolean enabled, String host, int port, String username, String password, int database, boolean ssl, String channel) {
        this.enabled = enabled;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.database = database;
        this.ssl = ssl;
        this.channel = channel;
    }

    public static RedisConfig from(FileConfiguration config) {
        return new RedisConfig(
                config.getBoolean(BASE_PATH + "enabled", false),
                config.getString(BASE_PATH + "host", "localhost"),
                config.getInt(BASE_PATH + "port", 6379),
                config.getString(BASE_PATH + "username", ""),
                config.getString(BASE_PATH + "password", ""),
                Math.max(0, config.getInt(BASE_PATH + "database", 0)),
                config.getBoolean(BASE_PATH + "ssl", false),
                config.getString(BASE_PATH + "channel", "supremetags:updates")
        );
    }

    public boolean enabled() {
        return enabled;
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    public int database() {
        return database;
    }

    public boolean ssl() {
        return ssl;
    }

    public String channel() {
        return channel;
    }
}
