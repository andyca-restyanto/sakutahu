// filepath: /backend/src/main/java/com/example/app/modules/admin/repository/AdminAuditLogRepository.java
package com.example.app.modules.admin.repository;

import com.example.app.modules.admin.entity.AdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {

    List<AdminAuditLog> findTop20ByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, String targetId);
}
