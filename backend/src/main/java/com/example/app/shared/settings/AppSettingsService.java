// filepath: /backend/src/main/java/com/example/app/shared/settings/AppSettingsService.java
package com.example.app.shared.settings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Pengaturan aplikasi dari tabel app_settings. Di-cache maksimal 60 detik, jadi perubahan lewat SQL
 * berlaku tanpa restart.
 */
@Service
public class AppSettingsService {

    private static final Logger log = LoggerFactory.getLogger(AppSettingsService.class);
    static final Duration TTL = Duration.ofSeconds(60);

    private final JdbcTemplate jdbc;
    private final Clock clock;

    private volatile Map<String, String> cache = Map.of();
    private volatile Instant loadedAt = Instant.EPOCH;

    public AppSettingsService(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    public Optional<String> get(String key) {
        return Optional.ofNullable(all().get(key));
    }

    public String getString(String key, String defaultValue) {
        return get(key).map(String::trim).filter(v -> !v.isEmpty()).orElse(defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        return get(key).map(v -> {
            try {
                return Integer.parseInt(v.trim());
            } catch (NumberFormatException e) {
                log.warn("app_settings '{}' bukan angka: '{}', memakai default {}", key, v, defaultValue);
                return defaultValue;
            }
        }).orElse(defaultValue);
    }

    /** Untuk test: paksa baca ulang dari database. */
    public void invalidate() {
        loadedAt = Instant.EPOCH;
    }

    private Map<String, String> all() {
        Instant now = clock.instant();
        if (loadedAt.plus(TTL).isBefore(now)) {
            synchronized (this) {
                if (loadedAt.plus(TTL).isBefore(now)) {
                    Map<String, String> loaded = new HashMap<>();
                    jdbc.query("SELECT key, value FROM app_settings",
                            (RowCallbackHandler) rs -> loaded.put(rs.getString("key"), rs.getString("value")));
                    cache = Map.copyOf(loaded);
                    loadedAt = now;
                }
            }
        }
        return cache;
    }
}
