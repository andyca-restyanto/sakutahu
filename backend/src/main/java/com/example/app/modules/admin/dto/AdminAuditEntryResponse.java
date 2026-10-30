// filepath: /backend/src/main/java/com/example/app/modules/admin/dto/AdminAuditEntryResponse.java
package com.example.app.modules.admin.dto;

import java.time.Instant;
import java.util.Map;

public record AdminAuditEntryResponse(
        Long id,
        String action,
        String adminName,
        Map<String, Object> detail,
        Instant createdAt
) {}
