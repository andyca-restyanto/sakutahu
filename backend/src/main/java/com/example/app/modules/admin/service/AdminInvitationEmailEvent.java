// filepath: /backend/src/main/java/com/example/app/modules/admin/service/AdminInvitationEmailEvent.java
package com.example.app.modules.admin.service;

/** Dipublikasikan saat undangan dibuat/dikirim ulang; email dikirim setelah commit. */
public record AdminInvitationEmailEvent(String email, String fullName, String inviterName, String rawToken) {}
