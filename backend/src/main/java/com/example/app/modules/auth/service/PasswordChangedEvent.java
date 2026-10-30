// filepath: /backend/src/main/java/com/example/app/modules/auth/service/PasswordChangedEvent.java
package com.example.app.modules.auth.service;

import java.time.Instant;

/** Pemberitahuan keamanan "password kamu baru saja diubah". viaReset = lewat link lupa password. */
public record PasswordChangedEvent(String email, String fullName, String timezone, Instant changedAt, boolean viaReset) {}
