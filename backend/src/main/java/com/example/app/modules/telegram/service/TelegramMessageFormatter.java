// filepath: /backend/src/main/java/com/example/app/modules/telegram/service/TelegramMessageFormatter.java
package com.example.app.modules.telegram.service;

import com.example.app.modules.category.dto.CategoryResponse;
import com.example.app.modules.category.entity.CategoryKind;
import com.example.app.modules.category.entity.TransactionType;
import com.example.app.modules.telegram.dto.TelegramWebhookReply.Button;
import com.example.app.modules.telegram.dto.TelegramWebhookReply.InlineKeyboard;
import com.example.app.modules.transaction.dto.TransactionResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Semua teks & tombol yang dikirim bot. Teks biasa (tanpa Markdown) agar tidak perlu escaping. */
public final class TelegramMessageFormatter {

    private static final Locale ID = Locale.of("id", "ID");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", ID);

    static final String HELP = """
            📝 Cara mencatat di SakuTahu

            • Pengeluaran: kopi susu 25rb
            • Pemasukan: +gaji 5jt  atau  /masuk 5jt gaji
            • Nominal bisa: 25000 · 25.000 · 25rb · 25k · 1,5jt · Rp25.000
            • 📸 Foto struk: kirim fotonya, total dibaca otomatis (fitur VIP / kuota)

            Kategori ditebak otomatis. Kalau belum yakin, bot akan menanyakannya.
            Semua catatan dari Telegram berlaku untuk hari ini.

            Perintah:
            /hariini – ringkasan hari ini
            /kategori – daftar kategori
            /batal – hapus catatan Telegram terakhir hari ini
            /bantuan – tampilkan pesan ini""";

    static final String NO_AMOUNT = "Nominalnya belum ada 🤔\nContoh: kopi susu 25rb  atau  +gaji 5jt\nKetik /bantuan untuk format lainnya.";
    static final String TODAY_ONLY = "Pencatatan lewat Telegram hanya untuk hari ini 📅\n"
            + "Kirim ulang tanpa tanggal (mis. kopi 25rb), atau catat transaksi tanggal lain lewat web SakuTahu.";
    static final String TOO_MANY = "Terlalu banyak pesan dalam waktu singkat. Coba lagi sebentar lagi ya.";
    static final String DRAFT_EXPIRED = "Pilihan ini sudah kedaluwarsa ⌛ Kirim ulang pesannya untuk mencatat.";
    static final String EDIT_EXPIRED = "Transaksi ini sudah lewat 10 menit, jadi tidak bisa diubah dari Telegram. Ubah lewat web SakuTahu.";
    static final String NOT_FOUND = "Transaksi tidak ditemukan atau sudah dihapus.";
    static final String DRAFT_CANCELLED = "Dibatalkan, tidak ada yang dicatat.";
    static final String NOTHING_TO_UNDO = "Belum ada catatan dari Telegram hari ini.";
    static final String TEXT_ONLY = "Kirim pesan teks ya, contoh: kopi 25rb";

    private TelegramMessageFormatter() {}

    public static String rupiah(BigDecimal amount) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(ID);
        boolean hasCents = amount.stripTrailingZeros().scale() > 0;
        DecimalFormat format = new DecimalFormat(hasCents ? "#,##0.00" : "#,##0", symbols);
        return "Rp" + format.format(amount.setScale(hasCents ? 2 : 0, RoundingMode.HALF_UP));
    }

    static String typeLabel(TransactionType type) {
        return type == TransactionType.INCOME ? "Pemasukan" : "Pengeluaran";
    }

    public static String confirmation(TransactionResponse tx, BigDecimal todayTotal) {
        String head = tx.receiptScanId() != null ? "✅ Tercatat dari struk 🧾 — " : "✅ Tercatat — ";
        StringBuilder sb = new StringBuilder(head).append(typeLabel(tx.type())).append('\n')
                .append(rupiah(tx.amount())).append(" · ").append(tx.category().icon()).append(' ').append(tx.category().name())
                .append('\n');
        if (tx.note() != null) {
            sb.append('"').append(tx.note()).append("\" · ");
        }
        sb.append(DATE.format(tx.occurredOn()));
        if (todayTotal != null) {
            String label = tx.type() == TransactionType.INCOME ? "Pemasukan"
                    : tx.category().kind() == CategoryKind.SAVING ? "Ditabung" : "Pengeluaran";
            sb.append('\n').append(label).append(" hari ini: ").append(rupiah(todayTotal));
        }
        return sb.toString();
    }

    public static InlineKeyboard confirmationButtons(TransactionResponse tx) {
        return new InlineKeyboard(List.of(List.of(
                new Button("✏️ Ubah kategori", "e:" + tx.id()),
                new Button("↩️ Batalkan", "x:" + tx.id()))));
    }

    static String cancelled(TransactionResponse tx) {
        return "↩️ Dibatalkan: " + typeLabel(tx.type()) + " " + rupiah(tx.amount()) + " · " + tx.category().name();
    }

    static String askCategory(TransactionType type, BigDecimal amount, String note) {
        return "Masuk kategori apa? 👇\n" + typeLabel(type) + " " + rupiah(amount)
                + (note != null ? " · \"" + note + "\"" : "")
                + "\n(pilihan berlaku 15 menit)";
    }

    static String chooseNewCategory(TransactionResponse tx) {
        return "Pilih kategori baru untuk " + typeLabel(tx.type()).toLowerCase(ID) + " " + rupiah(tx.amount())
                + (tx.note() != null ? " · \"" + tx.note() + "\"" : "") + " 👇";
    }

    /** Tombol kategori 2 kolom; callbackPrefix mis. "c:12:" atau "s:<txId>:". */
    static InlineKeyboard categoryKeyboard(List<CategoryResponse> categories, String callbackPrefix, Button extra) {
        List<List<Button>> rows = new ArrayList<>();
        List<Button> row = new ArrayList<>();
        for (CategoryResponse c : categories) {
            row.add(new Button(c.icon() + " " + c.name(), callbackPrefix + c.id()));
            if (row.size() == 2) {
                rows.add(row);
                row = new ArrayList<>();
            }
        }
        if (!row.isEmpty()) {
            rows.add(row);
        }
        if (extra != null) {
            rows.add(List.of(extra));
        }
        return new InlineKeyboard(rows);
    }

    static String categoryList(List<CategoryResponse> expense, List<CategoryResponse> income) {
        StringBuilder sb = new StringBuilder("📂 Kategori Pengeluaran\n");
        expense.forEach(c -> sb.append(c.icon()).append(' ').append(c.name()).append('\n'));
        sb.append("\n📂 Kategori Pemasukan\n");
        income.forEach(c -> sb.append(c.icon()).append(' ').append(c.name()).append('\n'));
        sb.append("\nTips: tulis nama barangnya, mis. \"parkir 5rb\", kategori akan ditebak otomatis.");
        return sb.toString().trim();
    }

    static String todaySummary(LocalDate today, Map<CategoryKind, BigDecimal> totals) {
        BigDecimal income = totals.get(CategoryKind.INCOME);
        BigDecimal spending = totals.get(CategoryKind.SPENDING);
        BigDecimal saving = totals.get(CategoryKind.SAVING);
        return "📊 Ringkasan " + DATE.format(today) + "\n"
                + "Pemasukan: " + rupiah(income) + "\n"
                + "Pengeluaran: " + rupiah(spending) + "\n"
                + "Ditabung: " + rupiah(saving) + "\n"
                + "Sisa: " + (income.subtract(spending).subtract(saving).signum() < 0 ? "-" : "")
                + rupiah(income.subtract(spending).subtract(saving).abs());
    }
}
