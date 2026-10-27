// filepath: /backend/src/main/java/com/example/app/modules/auth/dto/AuthResponse.java
package com.example.app.modules.auth.dto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {}
