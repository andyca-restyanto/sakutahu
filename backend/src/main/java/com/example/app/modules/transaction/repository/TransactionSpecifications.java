// filepath: /backend/src/main/java/com/example/app/modules/transaction/repository/TransactionSpecifications.java
package com.example.app.modules.transaction.repository;

import com.example.app.modules.category.entity.TransactionType;
import com.example.app.modules.transaction.entity.Transaction;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

/** Filter opsional untuk daftar transaksi; filter yang null diabaikan. */
public final class TransactionSpecifications {

    private TransactionSpecifications() {}

    public static Specification<Transaction> filter(UUID userId, TransactionType type, Long categoryId,
                                                    LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            var predicate = cb.equal(root.get("userId"), userId);
            if (type != null) {
                predicate = cb.and(predicate, cb.equal(root.get("type"), type));
            }
            if (categoryId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("categoryId"), categoryId));
            }
            if (from != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.<LocalDate>get("occurredOn"), from));
            }
            if (to != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.<LocalDate>get("occurredOn"), to));
            }
            return predicate;
        };
    }
}
