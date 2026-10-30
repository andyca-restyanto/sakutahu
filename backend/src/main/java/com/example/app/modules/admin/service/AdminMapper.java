// filepath: /backend/src/main/java/com/example/app/modules/admin/service/AdminMapper.java
package com.example.app.modules.admin.service;

import com.example.app.modules.admin.dto.AdminResponse;
import com.example.app.modules.admin.entity.AdminUser;

final class AdminMapper {

    private AdminMapper() {}

    static AdminResponse toResponse(AdminUser a) {
        return new AdminResponse(a.getId(), a.getFullName(), a.getEmail(), a.getRole(), a.getStatus(),
                a.isMustChangePassword(), a.getLastLoginAt(), a.getCreatedAt());
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
