// filepath: /backend/src/main/java/com/example/app/modules/receipt/repository/ReceiptScanRepository.java
package com.example.app.modules.receipt.repository;

import com.example.app.modules.receipt.entity.ReceiptScan;
import com.example.app.modules.receipt.entity.ReceiptScanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReceiptScanRepository extends JpaRepository<ReceiptScan, UUID> {

    Optional<ReceiptScan> findByUserIdAndFileUniqueId(UUID userId, String fileUniqueId);

    Optional<ReceiptScan> findByIdAndUserId(UUID id, UUID userId);

    /** Struk terbaru milik user yang menunggu nominal diketik. */
    Optional<ReceiptScan> findFirstByUserIdAndStatusAndProcessedAtAfterOrderByProcessedAtDesc(
            UUID userId, ReceiptScanStatus status, Instant after);

    List<ReceiptScan> findByStatusInAndCreatedAtBefore(Collection<ReceiptScanStatus> statuses, Instant before);

    List<ReceiptScan> findByStatusAndProcessedAtBefore(ReceiptScanStatus status, Instant before);
}
