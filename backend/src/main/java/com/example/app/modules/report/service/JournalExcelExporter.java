// filepath: /backend/src/main/java/com/example/app/modules/report/service/JournalExcelExporter.java
package com.example.app.modules.report.service;

import com.example.app.modules.category.entity.CategoryKind;
import com.example.app.modules.category.entity.TransactionType;
import com.example.app.modules.report.dto.CategoryRecapResponse;
import com.example.app.modules.report.dto.JournalEntryResponse;
import com.example.app.modules.report.dto.JournalSummaryResponse;
import com.example.app.modules.transaction.entity.TransactionSource;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Menulis laporan jurnal ke .xlsx dengan Apache POI SXSSF (streaming: hanya 200 baris di memori).
 * Sheet 1 "Jurnal" (buku kas) dan sheet 2 "Ringkasan" (total & rekap per kategori).
 * Angka ditulis sebagai number (bisa dijumlah/difilter), tanggal sebagai tanggal Excel,
 * teks selalu sebagai sel teks (tidak pernah formula) agar aman dari formula injection.
 */
@Component
public class JournalExcelExporter {

    static final String SHEET_JOURNAL = "Jurnal";
    static final String SHEET_SUMMARY = "Ringkasan";
    static final String[] HEADERS = {"No", "Tanggal", "Keterangan", "Kategori", "Jenis", "Sumber",
            "Masuk (Rp)", "Keluar (Rp)", "Saldo (Rp)"};
    private static final int[] WIDTHS = {6, 12, 40, 26, 14, 16, 16, 16, 18};
    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id-ID"));
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("d MMMM yyyy, HH.mm", Locale.forLanguageTag("id-ID"));

    /** Sumber baris jurnal; dipanggil sekali dan memberi baris berurutan ke consumer. */
    @FunctionalInterface
    public interface EntrySource {
        void forEach(Consumer<JournalEntryResponse> consumer);
    }

    private final Clock clock;

    public JournalExcelExporter(Clock clock) {
        this.clock = clock;
    }

    public void write(OutputStream out, JournalReportService.PreparedExport export, JournalSummaryResponse summary,
                      List<CategoryRecapResponse> recap, EntrySource entries) throws IOException {
        SXSSFWorkbook wb = new SXSSFWorkbook(200);
        wb.setCompressTempFiles(true);
        try {
            Styles s = new Styles(wb);
            writeJournal(wb, s, export, summary, recap, entries);
            writeSummary(wb, s, export, summary, recap);
            wb.write(out);
        } finally {
            wb.close();
            wb.dispose();
        }
    }

    // ---------- Sheet Jurnal ----------

    private void writeJournal(SXSSFWorkbook wb, Styles s, JournalReportService.PreparedExport export,
                              JournalSummaryResponse summary, List<CategoryRecapResponse> recap, EntrySource entries) {
        SXSSFSheet sheet = wb.createSheet(SHEET_JOURNAL);
        for (int i = 0; i < WIDTHS.length; i++) {
            sheet.setColumnWidth(i, WIDTHS[i] * 256);
        }
        JournalFilter f = export.filter();
        boolean filtered = f.filtered();

        int r = header(sheet, s, "Jurnal Keuangan (Buku Kas)", export);
        text(s, sheet.createRow(r++), 0, "Filter: " + filterLabel(f, recap), s.meta);
        r++;

        int headerRow = r;
        Row h = sheet.createRow(r++);
        for (int i = 0; i < HEADERS.length; i++) {
            if (filtered && i == 8) {
                break;
            }
            text(s, h, i, HEADERS[i], s.header);
        }
        sheet.createFreezePane(0, headerRow + 1);

        if (!filtered) {
            Row open = sheet.createRow(r++);
            date(open, 1, f.from(), s.dateBold);
            text(s, open, 2, "Saldo awal", s.bold);
            money(open, 8, summary.openingBalance(), s.moneyBold);
        }

        int[] rowIndex = {r};
        int[] no = {0};
        entries.forEach(e -> {
            Row row = sheet.createRow(rowIndex[0]++);
            number(row, 0, ++no[0], s.intStyle);
            date(row, 1, e.occurredOn(), s.date);
            text(s, row, 2, e.description(), s.text);
            text(s, row, 3, e.category().name(), s.text);
            text(s, row, 4, kindLabel(e.category().kind()), s.text);
            text(s, row, 5, sourceLabel(e.source(), e.receiptScanId() != null), s.text);
            if (e.type() == TransactionType.INCOME) {
                money(row, 6, e.income(), s.money);
            } else {
                money(row, 7, e.expense(), s.money);
            }
            if (!filtered) {
                money(row, 8, e.balance(), s.money);
            }
        });
        int lastDataRow = rowIndex[0] - 1;
        int totalRow = rowIndex[0];

        Row total = sheet.createRow(totalRow);
        text(s, total, 2, filtered ? "Total" : "Total periode / saldo akhir", s.totalLabel);
        money(total, 6, summary.totalIncome(), s.moneyTotal);
        money(total, 7, summary.totalExpense().add(summary.totalSaving()), s.moneyTotal);
        if (!filtered) {
            money(total, 8, summary.closingBalance(), s.moneyTotal);
        }
        if (summary.entryCount() == 0) {
            text(s, sheet.createRow(totalRow + 2), 0, "Tidak ada transaksi pada periode ini.", s.meta);
        } else if (filtered) {
            text(s, sheet.createRow(totalRow + 2), 0,
                    "Kolom saldo tidak ditampilkan karena laporan difilter.", s.meta);
        }
        if (lastDataRow > headerRow) {
            sheet.setAutoFilter(new CellRangeAddress(headerRow, lastDataRow, 0, filtered ? 7 : 8));
        }
    }

    // ---------- Sheet Ringkasan ----------

    private void writeSummary(SXSSFWorkbook wb, Styles s, JournalReportService.PreparedExport export,
                              JournalSummaryResponse summary, List<CategoryRecapResponse> recap) {
        SXSSFSheet sheet = wb.createSheet(SHEET_SUMMARY);
        int[] widths = {34, 16, 18, 18, 14};
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
        }
        int r = header(sheet, s, "Ringkasan Keuangan", export);
        text(s, sheet.createRow(r++), 0, "Filter: " + filterLabel(export.filter(), recap), s.meta);
        r++;

        if (summary.openingBalance() != null) {
            r = summaryLine(sheet, s, r, "Saldo awal", summary.openingBalance(), false);
        }
        r = summaryLine(sheet, s, r, "Total pemasukan", summary.totalIncome(), false);
        r = summaryLine(sheet, s, r, "Total pengeluaran", summary.totalExpense(), false);
        r = summaryLine(sheet, s, r, "Total tabungan & investasi", summary.totalSaving(), false);
        if (summary.closingBalance() != null) {
            r = summaryLine(sheet, s, r, "Saldo akhir", summary.closingBalance(), true);
        }
        Row count = sheet.createRow(r++);
        text(s, count, 0, "Jumlah transaksi", s.text);
        number(count, 1, summary.entryCount(), s.intStyle);
        r++;

        Row h = sheet.createRow(r++);
        String[] headers = {"Kategori", "Jenis", "Jumlah transaksi", "Total (Rp)", "Porsi"};
        for (int i = 0; i < headers.length; i++) {
            text(s, h, i, headers[i], s.header);
        }
        for (CategoryKind kind : List.of(CategoryKind.INCOME, CategoryKind.SPENDING, CategoryKind.SAVING)) {
            for (CategoryRecapResponse c : recap) {
                if (c.kind() != kind) {
                    continue;
                }
                Row row = sheet.createRow(r++);
                text(s, row, 0, c.icon() + " " + c.name(), s.text);
                text(s, row, 1, kindLabel(c.kind()), s.text);
                number(row, 2, c.count(), s.intStyle);
                money(row, 3, c.total(), s.money);
                Cell share = row.createCell(4);
                share.setCellValue(c.share());
                share.setCellStyle(s.percent);
            }
        }
        if (recap.isEmpty()) {
            text(s, sheet.createRow(r), 0, "Tidak ada transaksi pada periode ini.", s.meta);
        }
        text(s, sheet.createRow(r + 2), 0, "Porsi = persentase dari total jenis yang sama (pemasukan / pengeluaran / tabungan).", s.meta);
    }

    private int summaryLine(Sheet sheet, Styles s, int r, String label, BigDecimal value, boolean bold) {
        Row row = sheet.createRow(r);
        text(s, row, 0, label, bold ? s.bold : s.text);
        money(row, 1, value, bold ? s.moneyBold : s.money);
        return r + 1;
    }

    /** Kop laporan; mengembalikan index baris berikutnya. */
    private int header(Sheet sheet, Styles s, String title, JournalReportService.PreparedExport export) {
        JournalFilter f = export.filter();
        ZoneId zone = zoneOf(export.timezone());
        text(s, sheet.createRow(0), 0, title + " · SakuTahu", s.title);
        text(s, sheet.createRow(1), 0, "Nama: " + export.fullName(), s.meta);
        text(s, sheet.createRow(2), 0, "Periode: " + LONG_DATE.format(f.from()) + " – " + LONG_DATE.format(f.to()), s.meta);
        text(s, sheet.createRow(3), 0, "Diunduh: " + STAMP.format(clock.instant().atZone(zone)) + " (" + zone.getId() + ")", s.meta);
        return 4;
    }

    // ---------- label ----------

    static String kindLabel(CategoryKind kind) {
        return switch (kind) {
            case INCOME -> "Pemasukan";
            case SPENDING -> "Pengeluaran";
            case SAVING -> "Tabungan";
        };
    }

    static String sourceLabel(TransactionSource source, boolean receipt) {
        if (receipt) {
            return "Telegram · Struk";
        }
        return source == TransactionSource.TELEGRAM ? "Telegram" : "Web";
    }

    static String filterLabel(JournalFilter f, List<CategoryRecapResponse> recap) {
        if (!f.filtered()) {
            return "Semua transaksi";
        }
        StringBuilder sb = new StringBuilder();
        if (f.type() != null) {
            sb.append("Jenis ").append(f.type() == TransactionType.INCOME ? "pemasukan" : "pengeluaran");
        }
        if (f.categoryId() != null) {
            String name = recap.stream().filter(c -> c.categoryId().equals(f.categoryId()))
                    .map(CategoryRecapResponse::name).findFirst().orElse("kategori terpilih");
            if (!sb.isEmpty()) {
                sb.append(" · ");
            }
            sb.append("Kategori ").append(name);
        }
        return sb.toString();
    }

    private static ZoneId zoneOf(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            return ZoneId.of("Asia/Jakarta");
        }
    }

    // ---------- sel ----------

    /** Selalu sel teks. Teks yang diawali = + - @ diberi quote prefix agar Excel tidak pernah menganggapnya formula. */
    static void text(Styles s, Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        String v = value == null ? "" : value;
        cell.setCellValue(v);
        cell.setCellStyle(!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0 ? s.quoted(style) : style);
    }

    private static void money(Row row, int col, BigDecimal value, CellStyle style) {
        if (value == null) {
            return;
        }
        Cell cell = row.createCell(col);
        cell.setCellValue(value.doubleValue());
        cell.setCellStyle(style);
    }

    private static void number(Row row, int col, long value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static void date(Row row, int col, LocalDate value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    /** Style dibuat sekali per workbook (batas Excel ±64 ribu style). */
    static final class Styles {
        private final SXSSFWorkbook wb;
        /** Varian quote-prefix per style dasar, dibuat sekali saja. */
        private final java.util.Map<CellStyle, CellStyle> quotedStyles = new java.util.HashMap<>();
        final CellStyle title, meta, header, text, bold, totalLabel, date, dateBold, money, moneyBold, moneyTotal,
                intStyle, percent;

        CellStyle quoted(CellStyle base) {
            return quotedStyles.computeIfAbsent(base, b -> {
                CellStyle q = wb.createCellStyle();
                q.cloneStyleFrom(b);
                q.setQuotePrefixed(true);
                return q;
            });
        }

        Styles(SXSSFWorkbook wb) {
            this.wb = wb;
            DataFormat fmt = wb.createDataFormat();
            Font boldFont = wb.createFont();
            boldFont.setBold(true);
            Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            Font metaFont = wb.createFont();
            metaFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());

            title = wb.createCellStyle();
            title.setFont(titleFont);
            meta = wb.createCellStyle();
            meta.setFont(metaFont);
            text = wb.createCellStyle();
            bold = wb.createCellStyle();
            bold.setFont(boldFont);

            header = wb.createCellStyle();
            header.setFont(boldFont);
            header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setBorderBottom(BorderStyle.THIN);

            totalLabel = wb.createCellStyle();
            totalLabel.setFont(boldFont);
            totalLabel.setBorderTop(BorderStyle.THIN);

            date = wb.createCellStyle();
            date.setDataFormat(fmt.getFormat("dd/mm/yyyy"));
            date.setAlignment(HorizontalAlignment.LEFT);
            dateBold = wb.createCellStyle();
            dateBold.cloneStyleFrom(date);
            dateBold.setFont(boldFont);

            short moneyFmt = fmt.getFormat("#,##0;[Red]-#,##0");
            money = wb.createCellStyle();
            money.setDataFormat(moneyFmt);
            moneyBold = wb.createCellStyle();
            moneyBold.setDataFormat(moneyFmt);
            moneyBold.setFont(boldFont);
            moneyTotal = wb.createCellStyle();
            moneyTotal.cloneStyleFrom(moneyBold);
            moneyTotal.setBorderTop(BorderStyle.THIN);

            intStyle = wb.createCellStyle();
            intStyle.setDataFormat(fmt.getFormat("0"));
            percent = wb.createCellStyle();
            percent.setDataFormat(fmt.getFormat("0.0%"));
        }
    }
}
