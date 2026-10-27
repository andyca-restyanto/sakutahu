// filepath: /backend/src/main/java/com/example/app/modules/telegram/dto/TelegramStatusResponse.java
package com.example.app.modules.telegram.dto;

import java.time.Instant;

public record TelegramStatusResponse(
        boolean linked,
        String botUsername,
        String username,
        Instant linkedAt
) {
    public static TelegramStatusResponse notLinked(String botUsername) {
        return new TelegramStatusResponse(false, botUsername, null, null);
    }
}
