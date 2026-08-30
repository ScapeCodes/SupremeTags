package net.noscape.project.supremetags.storage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataCache {

    private Map<String, String> cache = new ConcurrentHashMap<>();

    public DataCache() {}

    public String getCachedData(String key) {
        if (key == null) {
            return null;
        }

        return cache.get(key);
    }

    public void cacheData(String key, String value) {
        if (key == null || value == null) {
            return;
        }

        cache.put(key, value);
    }

    public void removeFromCache(String key) {
        if (key == null) {
            return;
        }

        cache.remove(key);
    }

    public void clearCache() {
        cache.clear();
    }

    public Map<String, String> getCache() {
        return cache;
    }
}
