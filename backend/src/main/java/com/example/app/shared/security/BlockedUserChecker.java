// filepath: /backend/src/main/java/com/example/app/shared/security/BlockedUserChecker.java
package com.example.app.shared.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cek cepat apakah user diblokir, dipakai di setiap request API user dan pesan Telegram.
 * Hasil di-cache 30 detik per user; admin yang memblokir memanggil {@link #evict} agar langsung berlaku
 * (di instance yang sama; instance lain menyusul maksimal 30 detik).
 */
@Component
public class BlockedUserChecker {

    static final Duration TTL = Duration.ofSeconds(30);

    private record Entry(boolean blocked, Instant loadedAt) {}

    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final Map<UUID, Entry> cache = new ConcurrentHashMap<>();

    public BlockedUserChecker(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    public boolean isBlocked(UUID userId) {
        Instant now = clock.instant();
        Entry entry = cache.get(userId);
        if (entry == null || entry.loadedAt().plus(TTL).isBefore(now)) {
            List<Boolean> rows = jdbc.queryForList(
                    "SELECT blocked_at IS NOT NULL FROM users WHERE id = ?", Boolean.class, userId);
            // User yang sudah tidak ada diperlakukan seperti diblokir (token-nya tidak boleh dipakai)
            entry = new Entry(rows.isEmpty() || Boolean.TRUE.equals(rows.get(0)), now);
            if (cache.size() > 50_000) {
                cache.clear();
            }
            cache.put(userId, entry);
        }
        return entry.blocked();
    }

    public void evict(UUID userId) {
        cache.remove(userId);
    }
}
