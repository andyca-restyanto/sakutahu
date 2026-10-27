// filepath: /backend/src/main/java/com/example/app/shared/security/RestAuthHandlers.java
package com.example.app.shared.security;

import com.example.app.shared.exception.ErrorCode;
import com.example.app.shared.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Mengembalikan error JSON standar untuk 401/403 dari Spring Security. */
@Component
public class RestAuthHandlers {

    private final ObjectMapper objectMapper;

    public RestAuthHandlers(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AuthenticationEntryPoint entryPoint() {
        return (request, response, ex) -> {
            boolean invalidToken = Boolean.TRUE.equals(request.getAttribute(JwtAuthFilter.INVALID_TOKEN_ATTR));
            write(response, invalidToken ? ErrorCode.TOKEN_EXPIRED : ErrorCode.UNAUTHORIZED);
        };
    }

    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) -> write(response, ErrorCode.FORBIDDEN);
    }

    private void write(HttpServletResponse response, ErrorCode code) throws IOException {
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), ErrorResponse.of(code));
    }
}
