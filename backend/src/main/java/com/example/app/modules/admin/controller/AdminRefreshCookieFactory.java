// filepath: /backend/src/main/java/com/example/app/modules/admin/controller/AdminRefreshCookieFactory.java
package com.example.app.modules.admin.controller;

import com.example.app.shared.config.AdminProperties;
import com.example.app.shared.config.AppProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import java.time.Duration;

/** Cookie refresh admin: nama & path berbeda dari cookie user, jadi kedua sesi tidak saling tertukar. */
@Component
public class AdminRefreshCookieFactory {

    private final AdminProperties adminProps;
    private final AppProperties appProps;

    public AdminRefreshCookieFactory(AdminProperties adminProps, AppProperties appProps) {
        this.adminProps = adminProps;
        this.appProps = appProps;
    }

    public ResponseCookie create(String refreshToken) {
        return base(refreshToken, adminProps.refreshTokenTtl());
    }

    public ResponseCookie clear() {
        return base("", Duration.ZERO);
    }

    public String read(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, adminProps.refreshCookieName());
        return cookie != null ? cookie.getValue() : null;
    }

    private ResponseCookie base(String value, Duration maxAge) {
        return ResponseCookie.from(adminProps.refreshCookieName(), value)
                .httpOnly(true)
                .secure(appProps.cookie().secure())
                .sameSite("Strict")
                .path(adminProps.refreshCookiePath())
                .maxAge(maxAge)
                .build();
    }
}
