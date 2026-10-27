// filepath: /backend/src/main/java/com/example/app/modules/auth/controller/AuthController.java
package com.example.app.modules.auth.controller;

import com.example.app.modules.auth.dto.AuthResponse;
import com.example.app.modules.auth.dto.LoginRequest;
import com.example.app.modules.auth.dto.MessageResponse;
import com.example.app.modules.auth.dto.RegisterRequest;
import com.example.app.modules.auth.dto.RegisterResponse;
import com.example.app.modules.auth.dto.ResendVerificationRequest;
import com.example.app.modules.auth.dto.UserResponse;
import com.example.app.modules.auth.dto.VerifyEmailRequest;
import com.example.app.modules.auth.service.AuthResult;
import com.example.app.modules.auth.service.AuthService;
import com.example.app.shared.security.UserPrincipal;
import com.example.app.shared.util.ClientContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshCookieFactory cookies;

    public AuthController(AuthService authService, RefreshCookieFactory cookies) {
        this.authService = authService;
        this.cookies = cookies;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request,
                                                     HttpServletRequest http) {
        RegisterResponse body = authService.register(request, ClientContext.from(http));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<AuthResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request,
                                                    HttpServletRequest http) {
        return withCookie(authService.verifyEmail(request, ClientContext.from(http)));
    }

    @PostMapping("/resend-verification")
    public MessageResponse resendVerification(@Valid @RequestBody ResendVerificationRequest request,
                                              HttpServletRequest http) {
        return authService.resendVerification(request, ClientContext.from(http));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return withCookie(authService.login(request, ClientContext.from(http)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest http) {
        return withCookie(authService.refresh(cookies.read(http), ClientContext.from(http)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest http) {
        authService.logout(cookies.read(http));
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookies.clear().toString())
                .build();
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return authService.me(principal.id());
    }

    private ResponseEntity<AuthResponse> withCookie(AuthResult result) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.create(result.refreshToken()).toString())
                .body(result.response());
    }
}
