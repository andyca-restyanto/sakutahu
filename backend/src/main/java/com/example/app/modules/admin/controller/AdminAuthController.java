// filepath: /backend/src/main/java/com/example/app/modules/admin/controller/AdminAuthController.java
package com.example.app.modules.admin.controller;

import com.example.app.modules.admin.dto.AdminAuthResponse;
import com.example.app.modules.admin.dto.AdminLoginRequest;
import com.example.app.modules.admin.dto.AdminResponse;
import com.example.app.modules.admin.dto.ChangePasswordRequest;
import com.example.app.modules.admin.service.AdminAuthResult;
import com.example.app.modules.admin.service.AdminAuthService;
import com.example.app.shared.security.AdminPrincipal;
import com.example.app.shared.util.ClientContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/auth")
public class AdminAuthController {

    private final AdminAuthService authService;
    private final AdminRefreshCookieFactory cookies;

    public AdminAuthController(AdminAuthService authService, AdminRefreshCookieFactory cookies) {
        this.authService = authService;
        this.cookies = cookies;
    }

    @PostMapping("/login")
    public ResponseEntity<AdminAuthResponse> login(@Valid @RequestBody AdminLoginRequest request,
                                                   HttpServletRequest http) {
        return withCookie(authService.login(request, ClientContext.from(http)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AdminAuthResponse> refresh(HttpServletRequest http) {
        return withCookie(authService.refresh(cookies.read(http), ClientContext.from(http)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest http) {
        authService.logout(cookies.read(http));
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookies.clear().toString()).build();
    }

    @GetMapping("/me")
    public AdminResponse me(@AuthenticationPrincipal AdminPrincipal admin) {
        return authService.me(admin.id());
    }

    @PostMapping("/change-password")
    public ResponseEntity<AdminAuthResponse> changePassword(@AuthenticationPrincipal AdminPrincipal admin,
                                                            @Valid @RequestBody ChangePasswordRequest request,
                                                            HttpServletRequest http) {
        return withCookie(authService.changePassword(admin.id(), request, ClientContext.from(http)));
    }

    private ResponseEntity<AdminAuthResponse> withCookie(AdminAuthResult result) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.create(result.refreshToken()).toString())
                .body(result.response());
    }
}
