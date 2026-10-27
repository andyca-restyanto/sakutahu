// filepath: /backend/src/main/java/com/example/app/modules/receipt/service/ReceiptScanQueuedEvent.java
package com.example.app.modules.receipt.service;

import java.util.UUID;

/** Dipublikasikan saat struk diterima; diproses asinkron SETELAH transaksi database commit. */
public record ReceiptScanQueuedEvent(UUID scanId) {}
