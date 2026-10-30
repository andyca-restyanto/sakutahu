// filepath: /backend/src/main/java/com/example/app/modules/admin/service/AdminAccountLookupService.java
package com.example.app.modules.admin.service;

import com.example.app.modules.admin.repository.AdminUserRepository;
import com.example.app.shared.security.AdminAccountLookup;
import com.example.app.shared.security.AdminPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/** Dipakai AdminJwtAuthFilter di setiap request admin. */
@Service
public class AdminAccountLookupService implements AdminAccountLookup {

    private final AdminUserRepository repository;

    public AdminAccountLookupService(AdminUserRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AdminPrincipal> findActive(UUID adminId) {
        return repository.findById(adminId)
                .filter(a -> a.isActive())
                .map(a -> new AdminPrincipal(a.getId(), a.getEmail(), a.isOwner(), a.isMustChangePassword()));
    }
}
