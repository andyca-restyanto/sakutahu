// filepath: /backend/src/main/java/com/example/app/modules/admin/service/AdminPasswordEmailEvents.java
package com.example.app.modules.admin.service;

import java.time.Duration;
import java.time.Instant;

/** Event email lupa password admin; dikirim setelah commit dan secara asinkron. */
public final class AdminPasswordEmailEvents {

    private AdminPasswordEmailEvents() {}

    /** sentByOwnerName != null jika link dikirim OWNER dari menu Admin. */
    public record ResetRequested(String email, String fullName, String rawToken, Duration validFor,
                                 String sentByOwnerName) {}

    /** Pemberitahuan ke admin yang bersangkutan saja. */
    public record PasswordReset(String email, String fullName, Instant changedAt) {}
}
