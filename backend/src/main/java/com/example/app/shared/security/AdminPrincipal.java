// filepath: /backend/src/main/java/com/example/app/shared/security/AdminPrincipal.java
package com.example.app.shared.security;

import java.util.UUID;

/** Admin yang sedang login. Dibaca ulang dari database di setiap request (status & role selalu terbaru). */
public record AdminPrincipal(UUID id, String email, boolean owner, boolean mustChangePassword) {}
