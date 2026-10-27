// filepath: /backend/src/main/java/com/example/app/modules/telegram/service/parser/MessageParser.java
package com.example.app.modules.telegram.service.parser;

import com.example.app.modules.category.entity.TransactionType;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Mengubah pesan Telegram menjadi entri transaksi. Tidak menyentuh database.
 * Jenis transaksi eksplisit: awalan "+" atau /masuk → pemasukan; "-" atau /keluar → pengeluaran.
 * Tanpa awalan, jenis ditentukan belakangan oleh CategoryMatcher (default pengeluaran).
 */
public final class MessageParser {

    public sealed interface Result permits Entry, MissingAmount, DateNotAllowed {}

    /** explicitType null = belum ditentukan user. note bisa null. */
    public record Entry(TransactionType explicitType, BigDecimal amount, String note) implements Result {}

    public record MissingAmount() implements Result {}

    /** Pesan menyebut tanggal selain hari ini; pencatatan via Telegram hanya untuk hari berjalan. */
    public record DateNotAllowed() implements Result {}

    private static final Pattern DATE_WORDS = Pattern.compile(
            "(?iu)(?<!\\p{L})(kemarin|kemaren|kmrn|kmarin|besok|bsk|lusa|semalam|tgl|tanggal|minggu\\s+lalu|bulan\\s+lalu|minggu\\s+depan)(?!\\p{L})");

    private static final Pattern DATE_NUMERIC = Pattern.compile("(?<!\\d)\\d{1,2}[/-]\\d{1,2}(?:[/-]\\d{2,4})?(?!\\d)");

    private MessageParser() {}

    public static Result parse(String rawText) {
        String text = rawText == null ? "" : rawText.trim();
        TransactionType explicitType = null;

        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.startsWith("/masuk")) {
            explicitType = TransactionType.INCOME;
            text = stripCommand(text);
        } else if (lower.startsWith("/keluar")) {
            explicitType = TransactionType.EXPENSE;
            text = stripCommand(text);
        } else if (text.startsWith("+")) {
            explicitType = TransactionType.INCOME;
            text = text.substring(1).trim();
        } else if (text.startsWith("-")) {
            explicitType = TransactionType.EXPENSE;
            text = text.substring(1).trim();
        }

        // Cek tanggal sebelum nominal: "12/10" tidak boleh terbaca sebagai Rp12
        if (DATE_WORDS.matcher(text).find() || DATE_NUMERIC.matcher(text).find()) {
            return new DateNotAllowed();
        }

        Optional<AmountParser.Match> amount = AmountParser.find(text);
        if (amount.isEmpty()) {
            return new MissingAmount();
        }
        AmountParser.Match m = amount.get();
        String note = cleanNote(text.substring(0, m.start()) + " " + text.substring(m.end()));
        return new Entry(explicitType, m.value(), note);
    }

    private static String stripCommand(String text) {
        int space = text.indexOf(' ');
        return space < 0 ? "" : text.substring(space + 1).trim();
    }

    static String cleanNote(String s) {
        String cleaned = s.replaceAll("\\s+", " ")
                .replaceAll("^[\\s\\-:;,.=+]+", "")
                .replaceAll("[\\s\\-:;,.=+]+$", "")
                .trim();
        if (cleaned.isEmpty()) {
            return null;
        }
        return cleaned.length() > 255 ? cleaned.substring(0, 255) : cleaned;
    }
}
