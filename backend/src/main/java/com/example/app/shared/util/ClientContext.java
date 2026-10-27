// filepath: /backend/src/main/java/com/example/app/shared/util/ClientContext.java
package com.example.app.shared.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

/** Info client (IP & user agent) untuk rate limiting dan pencatatan sesi. */
public record ClientContext(String ip, String userAgent) {

    public static ClientContext from(HttpServletRequest request) {
        // IP asli dari X-Forwarded-For sudah ditangani oleh server.forward-headers-strategy=framework
        String ua = request.getHeader(HttpHeaders.USER_AGENT);
        if (ua != null && ua.length() > 255) {
            ua = ua.substring(0, 255);
        }
        return new ClientContext(request.getRemoteAddr(), ua);
    }
}
