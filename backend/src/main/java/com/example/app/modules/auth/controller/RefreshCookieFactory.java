// filepath: /backend/src/main/java/com/example/app/modules/auth/controller/RefreshCookieFactory.java
package com.example.app.modules.auth.controller;

import com.example.app.shared.config.AppProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import java.time.Duration;

/** Refresh token hanya hidup di cookie HttpOnly, path dibatasi ke /api/v1/auth. */
@Component
public class RefreshCookieFactory {

    private final AppProperties props;

    public RefreshCookieFactory(AppProperties props) {
        this.props = props;
    }

    public ResponseCookie create(String refreshToken) {
        return base(refreshToken, props.jwt().refreshTokenTtl());
    }

    public ResponseCookie clear() {
        return base("", Duration.ZERO);
    }

    public String read(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, props.cookie().refreshCookieName());
        return cookie != null ? cookie.getValue() : null;
    }

    private ResponseCookie base(String value, Duration maxAge) {
        return ResponseCookie.from(props.cookie().refreshCookieName(), value)
                .httpOnly(true)
                .secure(props.cookie().secure())
                .sameSite("Strict")
                .path(props.cookie().refreshCookiePath())
                .maxAge(maxAge)
                .build();
    }
}
