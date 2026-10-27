// filepath: /backend/src/main/java/com/example/app/modules/auth/service/AuthService.java
package com.example.app.modules.auth.service;

import com.example.app.modules.auth.dto.LoginRequest;
import com.example.app.modules.auth.dto.MessageResponse;
import com.example.app.modules.auth.dto.RegisterRequest;
import com.example.app.modules.auth.dto.RegisterResponse;
import com.example.app.modules.auth.dto.ResendVerificationRequest;
import com.example.app.modules.auth.dto.UserResponse;
import com.example.app.modules.auth.dto.VerifyEmailRequest;
import com.example.app.shared.util.ClientContext;

import java.util.UUID;

public interface AuthService {

    RegisterResponse register(RegisterRequest request, ClientContext ctx);

    AuthResult verifyEmail(VerifyEmailRequest request, ClientContext ctx);

    MessageResponse resendVerification(ResendVerificationRequest request, ClientContext ctx);

    AuthResult login(LoginRequest request, ClientContext ctx);

    AuthResult refresh(String rawRefreshToken, ClientContext ctx);

    void logout(String rawRefreshToken);

    UserResponse me(UUID userId);
}
