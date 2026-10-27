// filepath: /backend/src/main/java/com/example/app/shared/config/AppProperties.java
package com.example.app.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String frontendUrl,
        String backendUrl,
        List<String> corsAllowedOrigins,
        String mailFrom,
        Jwt jwt,
        Cookie cookie,
        Verification verification,
        Security security,
        Telegram telegram
) {
    public record Jwt(String secret, Duration accessTokenTtl, Duration refreshTokenTtl) {}

    public record Cookie(boolean secure, String refreshCookieName, String refreshCookiePath) {}

    public record Verification(Duration tokenTtl, Duration unverifiedAccountRetention) {}

    public record Security(int maxFailedLogins, Duration lockDuration) {}

    public record Telegram(
            String botToken,
            String botUsername,
            String webhookSecret,
            Duration linkTokenTtl
    ) {}
}
