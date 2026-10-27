// filepath: /backend/src/main/java/com/example/app/modules/category/service/CategoryService.java
package com.example.app.modules.category.service;

import com.example.app.modules.category.dto.CategoryResponse;
import com.example.app.modules.category.dto.CategorySummaryResponse;
import com.example.app.modules.category.entity.Category;
import com.example.app.modules.category.entity.TransactionType;
import com.example.app.modules.category.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Kategori bawaan hanya berubah lewat migrasi, jadi dimuat sekali lalu disimpan di memori.
 * Restart aplikasi setelah migrasi kategori baru.
 */
@Service
public class CategoryService {

    private final CategoryRepository repository;
    private volatile Map<Long, Category> cache;

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }

    public List<CategoryResponse> list(TransactionType type) {
        return all().values().stream()
                .filter(c -> type == null || c.getType() == type)
                .map(CategoryService::toResponse)
                .toList();
    }

    public Optional<Category> findActive(Long id) {
        return Optional.ofNullable(all().get(id));
    }

    /** Untuk kategori yang sudah dinonaktifkan tapi masih dipakai transaksi lama. */
    public CategorySummaryResponse summaryOf(Long id) {
        Category c = all().get(id);
        if (c == null) {
            c = repository.findById(id).orElseThrow();
        }
        return toSummary(c);
    }

    public static CategorySummaryResponse toSummary(Category c) {
        return new CategorySummaryResponse(c.getId(), c.getCode(), c.getName(), c.getKind(), c.getColor(), c.getIcon());
    }

    private static CategoryResponse toResponse(Category c) {
        return new CategoryResponse(c.getId(), c.getCode(), c.getName(), c.getType(), c.getKind(),
                c.getColor(), c.getIcon(), c.getSortOrder());
    }

    private Map<Long, Category> all() {
        Map<Long, Category> local = cache;
        if (local == null) {
            synchronized (this) {
                local = cache;
                if (local == null) {
                    local = repository.findByUserIdIsNullAndActiveTrueOrderByTypeAscSortOrderAsc().stream()
                            .collect(Collectors.toMap(Category::getId, Function.identity(),
                                    (a, b) -> a, LinkedHashMap::new));
                    cache = local;
                }
            }
        }
        return local;
    }
}
