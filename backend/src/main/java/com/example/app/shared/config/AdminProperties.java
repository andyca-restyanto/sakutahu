// filepath: /backend/src/main/java/com/example/app/shared/config/AdminProperties.java
package com.example.app.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Konfigurasi panel admin (application*.yml → app.admin.*). Sengaja terpisah dari konfigurasi user:
 * secret JWT, masa sesi, dan cookie admin berbeda dari milik user.
 */
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(
        String jwtSecret,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        String refreshCookieName,
        String refreshCookiePath,
        Duration invitationTtl,
        int maxFailedLogins,
        Duration lockDuration,
        Bootstrap bootstrap
) {
    /** OWNER pertama dibuat otomatis saat start jika belum ada admin sama sekali. */
    public record Bootstrap(String email, String fullName, String password) {}
}
