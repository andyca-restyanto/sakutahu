// filepath: /backend/src/main/java/com/example/app/modules/admin/service/AdminAccountService.java
package com.example.app.modules.admin.service;

import com.example.app.modules.admin.dto.AdminResponse;
import com.example.app.modules.admin.entity.AdminStatus;
import com.example.app.modules.admin.entity.AdminUser;
import com.example.app.modules.admin.repository.AdminUserRepository;
import com.example.app.shared.exception.ApiException;
import com.example.app.shared.exception.ErrorCode;
import com.example.app.shared.util.ClientContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Daftar admin & aktif/nonaktif. Endpoint-nya hanya untuk OWNER (diatur di SecurityConfig). */
@Service
public class AdminAccountService {

    private final AdminUserRepository repository;
    private final AdminSessionService sessionService;
    private final AdminAuditService auditService;

    public AdminAccountService(AdminUserRepository repository, AdminSessionService sessionService,
                               AdminAuditService auditService) {
        this.repository = repository;
        this.sessionService = sessionService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<AdminResponse> list() {
        return repository.findAllByOrderByCreatedAtAsc().stream().map(AdminMapper::toResponse).toList();
    }

    @Transactional
    public AdminResponse changeStatus(UUID ownerId, UUID adminId, AdminStatus status, ClientContext ctx) {
        AdminUser target = repository.findById(adminId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Admin tidak ditemukan"));
        // Mencakup "tidak bisa menonaktifkan diri sendiri", karena hanya OWNER yang bisa memanggil ini
        if (target.isOwner()) {
            throw new ApiException(ErrorCode.ADMIN_OWNER_PROTECTED);
        }
        AdminStatus before = target.getStatus();
        if (before != status) {
            target.changeStatus(status);
            if (status == AdminStatus.DISABLED) {
                sessionService.revokeAll(adminId);
            }
            auditService.record(ownerId, "ADMIN_STATUS_CHANGED", AdminAuditService.TARGET_ADMIN, adminId,
                    AdminAuditService.detail("email", target.getEmail(), "from", before, "to", status), ctx.ip());
        }
        return AdminMapper.toResponse(target);
    }
}
