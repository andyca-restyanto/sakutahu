// filepath: /backend/src/main/java/com/example/app/modules/auth/service/PasswordResetRequestedEvent.java
package com.example.app.modules.auth.service;

import java.time.Duration;

/** Email link reset dikirim setelah commit (token pasti tersimpan) dan secara asinkron. */
public record PasswordResetRequestedEvent(String email, String fullName, String rawToken, Duration validFor) {}
