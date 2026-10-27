// filepath: /backend/src/main/java/com/example/app/modules/auth/repository/UserRepository.java
package com.example.app.modules.auth.repository;

import com.example.app.modules.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    /** Email sudah dinormalisasi ke huruf kecil sebelum dipanggil. */
    Optional<User> findByEmail(String email);

    /** Token & data turunan ikut terhapus lewat ON DELETE CASCADE di database. */
    @Modifying
    @Query("delete from User u where u.emailVerifiedAt is null and u.createdAt < :before")
    int deleteUnverifiedCreatedBefore(@Param("before") Instant before);
}
