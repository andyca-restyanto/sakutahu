// filepath: /backend/src/main/java/com/example/app/modules/category/entity/CategoryKind.java
package com.example.app.modules.category.entity;

/**
 * Sifat kategori untuk perhitungan dashboard.
 * SPENDING = uang habis dipakai, SAVING = uang dipindah ke tabungan/investasi (keluar tapi bukan konsumsi),
 * INCOME = uang masuk.
 */
public enum CategoryKind {
    SPENDING,
    SAVING,
    INCOME
}
