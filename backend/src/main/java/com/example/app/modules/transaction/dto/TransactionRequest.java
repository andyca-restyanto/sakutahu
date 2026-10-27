// filepath: /backend/src/main/java/com/example/app/modules/transaction/dto/TransactionRequest.java
package com.example.app.modules.transaction.dto;

import com.example.app.modules.category.entity.TransactionType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Body untuk POST (buat) dan PUT (ubah) transaksi. */
public record TransactionRequest(
        @NotNull(message = "Jenis transaksi wajib dipilih")
        TransactionType type,

        @NotNull(message = "Kategori wajib dipilih")
        Long categoryId,

        @NotNull(message = "Nominal wajib diisi")
        @DecimalMin(value = "0.01", message = "Nominal harus lebih dari 0")
        @DecimalMax(value = "9999999999999.99", message = "Nominal terlalu besar")
        @Digits(integer = 13, fraction = 2, message = "Nominal maksimal 2 angka di belakang koma")
        BigDecimal amount,

        /* Tidak boleh lebih dari hari ini menurut zona waktu user (dicek di service). */
        @NotNull(message = "Tanggal wajib diisi")
        LocalDate occurredOn,

        @Size(max = 255, message = "Catatan maksimal 255 karakter")
        String note
) {}
