// filepath: /backend/src/main/java/com/example/app/shared/security/UserPrincipal.java
package com.example.app.shared.security;

import java.util.UUID;

/** Identitas user yang sedang login, diambil dari access token. */
public record UserPrincipal(UUID id, String email) {}
