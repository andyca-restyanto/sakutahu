// filepath: /backend/src/main/java/com/example/app/modules/receipt/service/ReceiptMessages.java
package com.example.app.modules.receipt.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Teks bot untuk fitur struk (bahasa Indonesia, teks biasa tanpa Markdown). */
final class ReceiptMessages {

    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.of("id", "ID"));

    static final String READING = "🧾 Sedang membaca struk…";
    static final String VIP_ONLY = "Fitur baca struk khusus VIP ⭐\nKamu tetap bisa mencatat dengan mengetik, mis. belanja 125rb";
    static final String NOT_CONFIGURED = "Fitur baca struk belum aktif di server. Catat dengan mengetik dulu ya, mis. belanja 125rb";
    static final String UNSUPPORTED_FILE = "Kirim struk sebagai foto/gambar (JPG, PNG, atau WebP) ya. PDF belum didukung.";
    static final String ALREADY_PROCESSING = "Struk ini masih diproses, tunggu sebentar ya ⏳";
    static final String NOT_A_RECEIPT = "Foto ini sepertinya bukan struk belanja 🤔\nCoba foto ulang struknya dengan jelas, atau ketik manual, mis. belanja 125rb";
    static final String READ_FAILED = "Maaf, struk gagal dibaca 😔\nCoba kirim ulang fotonya, atau ketik manual, mis. belanja 125rb";
    static final String SERVICE_UNAVAILABLE = "Fitur baca struk sedang gangguan di server 🛠️ (kuota kamu tidak terpakai).\nCatat dengan mengetik dulu ya, mis. belanja 125rb";
    static final String SERVICE_BUSY = "Layanan baca struk sedang sibuk ⏳ (kuota kamu tidak terpakai).\nCoba kirim ulang fotonya beberapa menit lagi.";
    static final String BAD_IMAGE = "Gambar ini tidak bisa diproses 😔\nCoba kirim sebagai foto biasa (bukan file), atau ketik manual, mis. belanja 125rb";
    static final String TIMED_OUT = "Maaf, pembacaan struk terlalu lama dan dibatalkan. Coba kirim ulang fotonya.";
    static final String AMOUNT_SKIPPED = "Oke, struk ini tidak dicatat.";
    static final String TOO_MANY = "Terlalu banyak struk dalam waktu singkat. Coba lagi sebentar lagi ya.";

    private ReceiptMessages() {}

    static String quotaExceeded(int used, int quota) {
        return "Kuota baca struk bulan ini habis (" + used + "/" + quota + ") 📸\n"
                + "Kuota direset tanggal 1. Kamu tetap bisa mencatat dengan mengetik, mis. belanja 125rb";
    }

    static String tooLarge(long maxMb) {
        return "Fotonya terlalu besar. Maksimal " + maxMb + " MB ya.";
    }

    static String needsAmount(String merchant) {
        return "Total di struk" + (merchant != null ? " " + merchant : "") + " tidak terbaca jelas 🤔\n"
                + "Ketik nominalnya saja, mis. 125rb (berlaku 15 menit).";
    }

    static String receiptDateNote(LocalDate receiptDate, LocalDate today) {
        if (receiptDate == null || receiptDate.equals(today)) {
            return "";
        }
        return "\nℹ️ Tanggal di struk: " + SHORT.format(receiptDate) + ", dicatat untuk hari ini";
    }

    static String quotaNote(Integer quota, int usedAfter) {
        if (quota == null) {
            return "";
        }
        return "\n📸 Kuota struk bulan ini: " + usedAfter + "/" + quota;
    }
}
