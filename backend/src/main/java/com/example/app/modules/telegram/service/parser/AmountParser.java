// filepath: /backend/src/main/java/com/example/app/modules/telegram/service/parser/AmountParser.java
package com.example.app.modules.telegram.service.parser;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mencari nominal Rupiah di teks bebas berbahasa Indonesia.
 * Didukung: 25000 · 25.000 · 25,000 · Rp25.000 · Rp 25.000 · 25rb · 25 ribu · 25k · 1,5jt · 1.5jt · 2 juta · 25.000,50
 * Konvensi: titik = pemisah ribuan, koma = desimal; dengan akhiran (rb/jt/k) titik/koma 1–2 digit dianggap desimal.
 */
public final class AmountParser {

    public record Match(BigDecimal value, int start, int end) {}

    static final BigDecimal MAX = new BigDecimal("9999999999999.99");

    private static final Pattern AMOUNT = Pattern.compile(
            "(?iu)(?<![\\p{L}\\d.,])"
                    + "(rp\\.?\\s*)?"
                    + "(\\d{1,3}(?:[.,]\\d{3})+(?:,\\d{1,2})?|\\d+(?:[.,]\\d+)?)"
                    + "(?:\\s*(ribu|rb|k|juta|jt))?"
                    + "(?![\\p{L}\\d])");

    private static final Pattern THOUSANDS = Pattern.compile("\\d{1,3}(?:[.,]\\d{3})+");

    private AmountParser() {}

    /**
     * Mengembalikan nominal paling mungkin: yang memakai "Rp" atau akhiran (rb/jt/k) lebih diutamakan;
     * jika tidak ada, angka terbesar (agar "bensin 30000 2 liter" tidak terbaca Rp2).
     */
    public static Optional<Match> find(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        List<Candidate> candidates = new ArrayList<>();
        Matcher m = AMOUNT.matcher(text);
        while (m.find()) {
            BigDecimal value = toValue(m.group(2), m.group(3));
            if (value == null || value.signum() <= 0 || value.compareTo(MAX) > 0) {
                continue;
            }
            boolean explicit = m.group(1) != null || m.group(3) != null;
            candidates.add(new Candidate(new Match(value, m.start(), m.end()), explicit));
        }
        Optional<Candidate> explicit = candidates.stream().filter(Candidate::explicit).reduce((a, b) -> b);
        if (explicit.isPresent()) {
            return Optional.of(explicit.get().match());
        }
        return candidates.stream().map(Candidate::match).max(Comparator.comparing(Match::value));
    }

    static BigDecimal toValue(String number, String suffix) {
        BigDecimal multiplier = multiplier(suffix);
        String n = number;
        try {
            if (suffix != null) {
                n = THOUSANDS.matcher(n).matches() ? n.replaceAll("[.,]", "") : n.replace(',', '.');
            } else if (n.matches("\\d{1,3}(?:[.,]\\d{3})+(?:,\\d{1,2})?")) {
                if (n.matches(".*,\\d{1,2}$")) {
                    int comma = n.lastIndexOf(',');
                    n = n.substring(0, comma).replaceAll("[.,]", "") + "." + n.substring(comma + 1);
                } else {
                    n = n.replaceAll("[.,]", "");
                }
            } else {
                n = n.replace(',', '.');
            }
            return new BigDecimal(n).multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal multiplier(String suffix) {
        if (suffix == null) {
            return BigDecimal.ONE;
        }
        return switch (suffix.toLowerCase(Locale.ROOT)) {
            case "rb", "ribu", "k" -> BigDecimal.valueOf(1_000);
            case "jt", "juta" -> BigDecimal.valueOf(1_000_000);
            default -> BigDecimal.ONE;
        };
    }

    private record Candidate(Match match, boolean explicit) {}
}
