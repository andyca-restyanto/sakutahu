// filepath: /backend/src/main/java/com/example/app/modules/admin/repository/AdminUserRepository.java
package com.example.app.modules.admin.repository;

import com.example.app.modules.admin.entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdminUserRepository extends JpaRepository<AdminUser, UUID> {

    /** Email sudah dinormalisasi ke huruf kecil. */
    Optional<AdminUser> findByEmail(String email);

    boolean existsByEmail(String email);

    List<AdminUser> findAllByOrderByCreatedAtAsc();
}
