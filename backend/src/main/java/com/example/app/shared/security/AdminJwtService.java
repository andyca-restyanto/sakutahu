// filepath: /backend/src/main/java/com/example/app/shared/security/AdminJwtService.java
package com.example.app.shared.security;

import com.example.app.shared.config.AdminProperties;
import com.example.app.shared.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Access token khusus admin: secret berbeda dari token user + audience "sakutahu-admin".
 * Token user tidak lolos di sini dan token admin tidak lolos di JwtService user.
 */
@Service
public class AdminJwtService {

    private static final String ISSUER = "sakutahu";
    static final String AUDIENCE = "sakutahu-admin";

    private final SecretKey key;
    private final Duration accessTokenTtl;

    public AdminJwtService(AdminProperties adminProps, AppProperties appProps) {
        String secret = adminProps.jwtSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.admin.jwt-secret (ADMIN_JWT_SECRET) minimal 32 byte");
        }
        if (secret.equals(appProps.jwt().secret())) {
            throw new IllegalStateException("ADMIN_JWT_SECRET harus berbeda dari JWT_SECRET");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtl = adminProps.accessTokenTtl();
    }

    public String generateAccessToken(UUID adminId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .subject(adminId.toString())
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .signWith(key)
                .compact();
    }

    /** @throws JwtException jika token tidak valid, kedaluwarsa, atau bukan token admin */
    public UUID parseAdminId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(ISSUER)
                .requireAudience(AUDIENCE)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return UUID.fromString(claims.getSubject());
    }

    public long accessTokenTtlSeconds() {
        return accessTokenTtl.toSeconds();
    }
}
