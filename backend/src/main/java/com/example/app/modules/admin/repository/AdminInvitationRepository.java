// filepath: /backend/src/main/java/com/example/app/modules/admin/repository/AdminInvitationRepository.java
package com.example.app.modules.admin.repository;

import com.example.app.modules.admin.entity.AdminInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdminInvitationRepository extends JpaRepository<AdminInvitation, UUID> {

    Optional<AdminInvitation> findByTokenHash(String tokenHash);

    @Query("select i from AdminInvitation i where i.acceptedAt is null and i.revokedAt is null order by i.createdAt desc")
    List<AdminInvitation> findPending();

    @Query("select i from AdminInvitation i where i.email = :email and i.acceptedAt is null and i.revokedAt is null")
    Optional<AdminInvitation> findPendingByEmail(@Param("email") String email);

    /** Undangan yang tidak dipakai dibersihkan 30 hari setelah kedaluwarsa. */
    @Modifying
    @Query("delete from AdminInvitation i where i.acceptedAt is null and i.expiresAt < :before")
    int deleteExpiredBefore(@Param("before") Instant before);
}
