// filepath: /backend/src/main/java/com/example/app/shared/ratelimit/RateLimiter.java
package com.example.app.shared.ratelimit;

import com.example.app.shared.exception.ApiException;
import com.example.app.shared.exception.ErrorCode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Rate limiter fixed-window di memori.
 * Catatan: hanya berlaku per instance. Jika backend di-scale ke beberapa instance,
 * ganti implementasi ini dengan Redis (mis. Bucket4j + Redis).
 */
@Component
public class RateLimiter {

    private record Window(long startMillis, long windowMillis, int count) {}

    private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();

    /** @throws ApiException TOO_MANY_REQUESTS jika kuota habis */
    public void consume(String key, int limit, Duration window) {
        if (!tryConsume(key, limit, window)) {
            throw new ApiException(ErrorCode.TOO_MANY_REQUESTS);
        }
    }

    public boolean tryConsume(String key, int limit, Duration window) {
        long now = System.currentTimeMillis();
        long windowMillis = window.toMillis();
        String compositeKey = key + "|" + windowMillis;
        Window updated = windows.compute(compositeKey, (k, w) -> {
            if (w == null || now - w.startMillis() >= w.windowMillis()) {
                return new Window(now, windowMillis, 1);
            }
            return new Window(w.startMillis(), w.windowMillis(), w.count() + 1);
        });
        return updated.count() <= limit;
    }

    @Scheduled(fixedDelay = 600_000)
    void evictExpired() {
        long now = System.currentTimeMillis();
        windows.entrySet().removeIf(e -> now - e.getValue().startMillis() >= e.getValue().windowMillis());
    }
}
