// filepath: /backend/src/main/java/com/example/app/modules/auth/dto/RegisterResponse.java
package com.example.app.modules.auth.dto;

public record RegisterResponse(
        String email,
        boolean verificationRequired,
        String message
) {}
