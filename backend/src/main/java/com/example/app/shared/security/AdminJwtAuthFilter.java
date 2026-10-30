// filepath: /backend/src/main/java/com/example/app/shared/security/AdminJwtAuthFilter.java
package com.example.app.shared.security;

import com.example.app.shared.exception.ErrorCode;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Autentikasi request /api/v1/admin/**. Status admin dicek ke database di setiap request,
 * jadi admin yang dinonaktifkan langsung kehilangan akses tanpa menunggu token kedaluwarsa.
 */
@Component
public class AdminJwtAuthFilter extends OncePerRequestFilter {

    /** Selama wajib ganti password, hanya endpoint ini yang boleh dipakai. */
    static final Set<String> ALLOWED_WHILE_PASSWORD_CHANGE = Set.of(
            "/api/v1/admin/auth/me",
            "/api/v1/admin/auth/change-password",
            "/api/v1/admin/auth/logout",
            "/api/v1/admin/auth/refresh");

    private final AdminJwtService jwtService;
    private final AdminAccountLookup lookup;
    private final RestAuthHandlers handlers;

    public AdminJwtAuthFilter(AdminJwtService jwtService, AdminAccountLookup lookup, RestAuthHandlers handlers) {
        this.jwtService = jwtService;
        this.lookup = lookup;
        this.handlers = handlers;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            Optional<AdminPrincipal> principal = Optional.empty();
            try {
                UUID adminId = jwtService.parseAdminId(header.substring(7));
                principal = lookup.findActive(adminId);
            } catch (JwtException | IllegalArgumentException ex) {
                // token rusak / kedaluwarsa / bukan token admin
            }
            if (principal.isEmpty()) {
                request.setAttribute(JwtAuthFilter.INVALID_TOKEN_ATTR, Boolean.TRUE);
                SecurityContextHolder.clearContext();
            } else {
                AdminPrincipal admin = principal.get();
                if (admin.mustChangePassword() && !ALLOWED_WHILE_PASSWORD_CHANGE.contains(request.getRequestURI())) {
                    handlers.write(response, ErrorCode.PASSWORD_CHANGE_REQUIRED);
                    return;
                }
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                if (admin.owner()) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_OWNER"));
                }
                var auth = new UsernamePasswordAuthenticationToken(admin, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }
}
