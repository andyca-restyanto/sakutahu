// filepath: /backend/src/main/java/com/example/app/modules/admin/repository/AdminRefreshTokenRepository.java
package com.example.app.modules.admin.repository;

import com.example.app.modules.admin.entity.AdminRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AdminRefreshTokenRepository extends JpaRepository<AdminRefreshToken, UUID> {

    Optional<AdminRefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update AdminRefreshToken t set t.revokedAt = :now where t.adminId = :adminId and t.revokedAt is null")
    int revokeAllActive(@Param("adminId") UUID adminId, @Param("now") Instant now);

    @Modifying
    @Query("delete from AdminRefreshToken t where t.expiresAt < :now or (t.revokedAt is not null and t.revokedAt < :revokedBefore)")
    int deleteStale(@Param("now") Instant now, @Param("revokedBefore") Instant revokedBefore);
}
