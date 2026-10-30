// filepath: /backend/src/main/java/com/example/app/shared/security/AdminAccountLookup.java
package com.example.app.shared.security;

import java.util.Optional;
import java.util.UUID;

/**
 * Diimplementasikan modul admin. Security cukup tahu kontrak ini sehingga shared/ tidak bergantung pada modul.
 * Mengembalikan empty jika admin tidak ada atau sudah DISABLED.
 */
public interface AdminAccountLookup {

    Optional<AdminPrincipal> findActive(UUID adminId);
}
