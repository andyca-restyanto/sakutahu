// filepath: /backend/src/main/java/com/example/app/modules/telegram/service/CategoryMatcher.java
package com.example.app.modules.telegram.service;

import com.example.app.modules.category.entity.Category;
import com.example.app.modules.category.entity.TransactionType;
import com.example.app.modules.category.service.CategoryService;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Menebak kategori dari catatan transaksi memakai tabel kategori yang sama dengan web.
 * Urutan: kata kunci pribadi user (hasil koreksi) → kata kunci bawaan (category_keywords).
 * Jika tidak ada kecocokan yang jelas, hasilnya kosong dan bot akan menanyakan kategori.
 */
@Component
public class CategoryMatcher {

    /** Kata umum yang tidak membawa makna kategori. */
    static final Set<String> STOPWORDS = Set.of(
            "beli", "bayar", "buat", "untuk", "utk", "di", "ke", "dari", "dan", "yang", "yg", "sama", "pakai", "pake",
            "tadi", "hari", "ini", "pagi", "siang", "sore", "malam", "rp", "isi", "top", "up", "topup", "the", "and",
            "for", "dengan", "dgn", "aja", "saja", "lagi", "juga", "x");

    /** Kata yang menandakan uang masuk walau kategorinya belum jelas. */
    static final Set<String> INCOME_HINTS = Set.of(
            "terima", "diterima", "dapat", "dapet", "masuk", "transferan", "ditransfer", "income", "pemasukan");

    private final NamedParameterJdbcTemplate jdbc;
    private final CategoryService categoryService;
    private volatile Map<String, Long> globalKeywords;

    public CategoryMatcher(NamedParameterJdbcTemplate jdbc, CategoryService categoryService) {
        this.jdbc = jdbc;
        this.categoryService = categoryService;
    }

    /**
     * @param type jenis yang sudah pasti (dari "+" atau /masuk), atau null jika belum ditentukan
     */
    @Transactional(readOnly = true)
    public Optional<Category> match(UUID userId, TransactionType type, String note) {
        List<String> tokens = tokenize(note);
        if (tokens.isEmpty()) {
            return Optional.empty();
        }
        Optional<Category> personal = best(tokens, userKeywords(userId, tokens), type);
        if (personal.isPresent()) {
            return personal;
        }
        return best(tokens, globalKeywords(), type);
    }

    public boolean hasIncomeHint(String note) {
        return tokenize(note).stream().anyMatch(INCOME_HINTS::contains);
    }

    /** Simpan koreksi user: kata-kata di catatan dipetakan ke kategori pilihan user. */
    @Transactional
    public void remember(UUID userId, String note, Long categoryId) {
        List<String> tokens = tokenize(note).stream()
                .filter(t -> !INCOME_HINTS.contains(t) && t.length() >= 3 && !t.chars().allMatch(Character::isDigit))
                .limit(3)
                .toList();
        for (String token : tokens) {
            jdbc.update("""
                    INSERT INTO user_category_keywords (user_id, keyword, category_id, updated_at)
                    VALUES (:userId, :keyword, :categoryId, now())
                    ON CONFLICT (user_id, keyword)
                    DO UPDATE SET category_id = EXCLUDED.category_id, updated_at = now()
                    """, new MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("keyword", token)
                    .addValue("categoryId", categoryId));
        }
    }

    static List<String> tokenize(String note) {
        if (note == null || note.isBlank()) {
            return List.of();
        }
        return Arrays.stream(note.toLowerCase(Locale.ROOT).split("[^\\p{L}\\d]+"))
                .filter(t -> t.length() >= 2 && t.length() <= 40 && !STOPWORDS.contains(t))
                .distinct()
                .toList();
    }

    /** Kategori dengan kecocokan terbanyak; jika seri, yang kata kuncinya muncul paling awal di catatan. */
    private Optional<Category> best(List<String> tokens, Map<String, Long> keywordToCategory, TransactionType type) {
        Map<Long, Integer> hits = new LinkedHashMap<>();
        for (String token : tokens) {
            Long categoryId = keywordToCategory.get(token);
            if (categoryId == null) {
                continue;
            }
            Optional<Category> category = categoryService.findActive(categoryId);
            if (category.isEmpty() || (type != null && category.get().getType() != type)) {
                continue;
            }
            hits.merge(categoryId, 1, Integer::sum);
        }
        return hits.entrySet().stream()
                .reduce((a, b) -> b.getValue() > a.getValue() ? b : a)
                .flatMap(e -> categoryService.findActive(e.getKey()));
    }

    private Map<String, Long> userKeywords(UUID userId, List<String> tokens) {
        Map<String, Long> result = new HashMap<>();
        jdbc.query("SELECT keyword, category_id FROM user_category_keywords WHERE user_id = :userId AND keyword IN (:tokens)",
                new MapSqlParameterSource().addValue("userId", userId).addValue("tokens", tokens),
                (RowCallbackHandler) rs -> {
                    result.put(rs.getString("keyword"), rs.getLong("category_id"));
                });
        return result;
    }

    /** Kata kunci bawaan hanya berubah lewat migrasi, jadi dimuat sekali. */
    private Map<String, Long> globalKeywords() {
        Map<String, Long> local = globalKeywords;
        if (local == null) {
            synchronized (this) {
                local = globalKeywords;
                if (local == null) {
                    Map<String, Long> loaded = new HashMap<>();
                    jdbc.query("SELECT keyword, category_id FROM category_keywords", new MapSqlParameterSource(),
                            (RowCallbackHandler) rs -> {
                                loaded.put(rs.getString("keyword"), rs.getLong("category_id"));
                            });
                    local = Map.copyOf(loaded);
                    globalKeywords = local;
                }
            }
        }
        return local;
    }
}
