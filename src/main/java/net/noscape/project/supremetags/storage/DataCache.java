package net.noscape.project.supremetags.storage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataCache {

    private Map<String, String> cache = new ConcurrentHashMap<>();

    public DataCache() {}

    public String getCachedData(String key) {
        return cache.get(key);
    }

    public void cacheData(String key, String value) {
        cache.put(key, value);
    }

    public void removeFromCache(String key) {
        cache.remove(key);
    }

    public void clearCache() {
        cache.clear();
    }

    public Map<String, String> getCache() {
        return cache;
    }
}