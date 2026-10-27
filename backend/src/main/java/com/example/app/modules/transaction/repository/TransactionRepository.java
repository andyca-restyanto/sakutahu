// filepath: /backend/src/main/java/com/example/app/modules/transaction/repository/TransactionRepository.java
package com.example.app.modules.transaction.repository;

import com.example.app.modules.transaction.entity.Transaction;
import com.example.app.modules.transaction.entity.TransactionSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    /** Selalu cari berdasarkan id + pemilik, agar transaksi user lain tidak pernah terbaca. */
    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    Optional<Transaction> findByTelegramChatIdAndTelegramMessageId(Long chatId, Long messageId);

    /** Transaksi Telegram terbaru milik user pada tanggal tertentu (untuk /batal). */
    Optional<Transaction> findFirstByUserIdAndSourceAndOccurredOnOrderByCreatedAtDesc(
            UUID userId, TransactionSource source, LocalDate occurredOn);
}
