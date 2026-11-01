// filepath: /backend/src/main/java/com/example/app/modules/admin/repository/AdminPasswordResetTokenRepository.java
package com.example.app.modules.admin.repository;

import com.example.app.modules.admin.entity.AdminPasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AdminPasswordResetTokenRepository extends JpaRepository<AdminPasswordResetToken, UUID> {

    Optional<AdminPasswordResetToken> findByTokenHash(String tokenHash);

    /** Link lama tidak berlaku lagi saat link baru diminta atau reset berhasil. */
    @Modifying
    @Query("update AdminPasswordResetToken t set t.usedAt = :now where t.adminId = :adminId and t.usedAt is null")
    int invalidateActive(@Param("adminId") UUID adminId, @Param("now") Instant now);

    @Modifying
    @Query("delete from AdminPasswordResetToken t where t.expiresAt < :now or t.usedAt is not null")
    int deleteStale(@Param("now") Instant now);
}
