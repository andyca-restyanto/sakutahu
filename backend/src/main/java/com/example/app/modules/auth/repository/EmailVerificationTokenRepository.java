// filepath: /backend/src/main/java/com/example/app/modules/auth/repository/EmailVerificationTokenRepository.java
package com.example.app.modules.auth.repository;

import com.example.app.modules.auth.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    /** Menonaktifkan semua token yang masih aktif (dipakai saat token baru dibuat / setelah verifikasi). */
    @Modifying
    @Query("update EmailVerificationToken t set t.usedAt = :now where t.userId = :userId and t.usedAt is null")
    int invalidateActiveForUser(@Param("userId") UUID userId, @Param("now") Instant now);

    @Modifying
    @Query("delete from EmailVerificationToken t where t.expiresAt < :now or t.usedAt is not null")
    int deleteStale(@Param("now") Instant now);
}
