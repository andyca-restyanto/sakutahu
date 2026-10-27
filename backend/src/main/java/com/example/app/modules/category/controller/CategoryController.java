// filepath: /backend/src/main/java/com/example/app/modules/category/controller/CategoryController.java
package com.example.app.modules.category.controller;

import com.example.app.modules.category.dto.CategoryResponse;
import com.example.app.modules.category.entity.TransactionType;
import com.example.app.modules.category.service.CategoryService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> list(@RequestParam(required = false) TransactionType type) {
        return ResponseEntity.ok()
                // Jangan di-cache browser: kategori baru dari migrasi harus langsung terlihat.
                // Frontend sudah menyimpan kategori di store selama satu sesi halaman.
                .cacheControl(CacheControl.noStore())
                .body(categoryService.list(type));
    }
}
