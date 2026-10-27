// filepath: /backend/src/main/java/com/example/app/modules/dashboard/dto/Granularity.java
package com.example.app.modules.dashboard.dto;

/** Ukuran bucket grafik tren. DAY untuk rentang ≤ 31 hari, MONTH untuk lebih panjang. */
public enum Granularity {
    DAY,
    MONTH
}
