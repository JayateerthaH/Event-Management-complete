package com.event.aimodule.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** Small bounded in-memory cache with per-entry expiry (one per AI feature). */
public class TtlCache<K, V> {

    private record Entry<V>(V value, Instant expiresAt) {}

    private final Map<K, Entry<V>> entries = new ConcurrentHashMap<>();
    private final Duration ttl;
    private final int maxEntries;
    private final Clock clock;

    public TtlCache(Duration ttl, int maxEntries) {
        this(ttl, maxEntries, Clock.systemUTC());
    }

    TtlCache(Duration ttl, int maxEntries, Clock clock) {
        this.ttl = ttl;
        this.maxEntries = maxEntries;
        this.clock = clock;
    }

    /** Cached value, or the loader's result (cached only when present). */
    public Optional<V> get(K key, Supplier<Optional<V>> loader) {
        Instant now = clock.instant();
        Entry<V> hit = entries.get(key);
        if (hit != null && hit.expiresAt().isAfter(now)) {
            return Optional.of(hit.value());
        }
        Optional<V> loaded = loader.get();
        loaded.ifPresent(v -> {
            if (entries.size() >= maxEntries) {
                entries.entrySet().removeIf(e -> !e.getValue().expiresAt().isAfter(now));
                if (entries.size() >= maxEntries) entries.clear();   // crude but bounded
            }
            entries.put(key, new Entry<>(v, now.plus(ttl)));
        });
        return loaded;
    }
}
