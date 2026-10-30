// filepath: /backend/src/main/java/com/example/app/modules/admin/dto/ReceiptScanUsage.java
package com.example.app.modules.admin.dto;

/** Statistik baca struk bulan berjalan (zona waktu Asia/Jakarta). month = "YYYY-MM". */
public record ReceiptScanUsage(
        String month,
        int totalScans,
        int successScans,
        int failedScans,
        int usersAtQuota
) {}
