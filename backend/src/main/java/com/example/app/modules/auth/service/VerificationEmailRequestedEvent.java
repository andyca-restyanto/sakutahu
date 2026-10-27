// filepath: /backend/src/main/java/com/example/app/modules/auth/service/VerificationEmailRequestedEvent.java
package com.example.app.modules.auth.service;

/** Dipublikasikan saat token verifikasi baru dibuat; email dikirim setelah transaksi commit. */
public record VerificationEmailRequestedEvent(String email, String fullName, String rawToken) {}
