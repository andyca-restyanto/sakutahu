// filepath: /backend/src/main/java/com/example/app/shared/security/JwtAuthFilter.java
package com.example.app.shared.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    /** Penanda untuk entry point: token ada tapi tidak valid/kedaluwarsa. */
    public static final String INVALID_TOKEN_ATTR = "sakutahu.invalidToken";
    /** Penanda untuk entry point: token valid tapi akun diblokir admin. */
    public static final String BLOCKED_ATTR = "sakutahu.blocked";

    private final JwtService jwtService;
    private final BlockedUserChecker blockedUserChecker;

    public JwtAuthFilter(JwtService jwtService, BlockedUserChecker blockedUserChecker) {
        this.jwtService = jwtService;
        this.blockedUserChecker = blockedUserChecker;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                UserPrincipal principal = jwtService.parse(token);
                if (blockedUserChecker.isBlocked(principal.id())) {
                    request.setAttribute(BLOCKED_ATTR, Boolean.TRUE);
                    SecurityContextHolder.clearContext();
                    chain.doFilter(request, response);
                    return;
                }
                var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException | IllegalArgumentException ex) {
                request.setAttribute(INVALID_TOKEN_ATTR, Boolean.TRUE);
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
