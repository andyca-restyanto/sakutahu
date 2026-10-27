// filepath: /backend/src/main/java/com/example/app/modules/telegram/dto/TelegramLinkResponse.java
package com.example.app.modules.telegram.dto;

import java.time.Instant;

public record TelegramLinkResponse(
        String deepLink,
        String botUsername,
        Instant expiresAt
) {}
