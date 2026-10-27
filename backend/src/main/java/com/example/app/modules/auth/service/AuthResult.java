// filepath: /backend/src/main/java/com/example/app/modules/auth/service/AuthResult.java
package com.example.app.modules.auth.service;

import com.example.app.modules.auth.dto.AuthResponse;

/** Hasil internal login: body response + refresh token mentah untuk di-set ke cookie. */
public record AuthResult(AuthResponse response, String refreshToken) {}
