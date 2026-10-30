// filepath: /backend/src/main/java/com/example/app/modules/admin/service/AdminAuditService.java
package com.example.app.modules.admin.service;

import com.example.app.modules.admin.entity.AdminAuditLog;
import com.example.app.modules.admin.repository.AdminAuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Mencatat aksi admin dalam transaksi yang sama dengan perubahannya (gagal = tidak tercatat, dan sebaliknya). */
@Service
public class AdminAuditService {

    public static final String TARGET_USER = "USER";
    public static final String TARGET_ADMIN = "ADMIN";
    public static final String TARGET_INVITATION = "INVITATION";
    public static final String TARGET_SETTINGS = "SETTINGS";

    private final AdminAuditLogRepository repository;

    public AdminAuditService(AdminAuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(UUID adminId, String action, String targetType, Object targetId, Map<String, Object> detail,
                       String ip) {
        repository.save(new AdminAuditLog(adminId, action, targetType,
                targetId == null ? null : targetId.toString(), detail, ip));
    }

    /** Map yang boleh berisi null (Map.of tidak). Pasangan key, value, key, value, ... */
    public static Map<String, Object> detail(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            Object value = keyValues[i + 1];
            map.put(String.valueOf(keyValues[i]), value == null ? null : value.toString());
        }
        return map;
    }
}
