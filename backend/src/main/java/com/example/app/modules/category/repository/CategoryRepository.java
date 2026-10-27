// filepath: /backend/src/main/java/com/example/app/modules/category/repository/CategoryRepository.java
package com.example.app.modules.category.repository;

import com.example.app.modules.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** Kategori bawaan sistem yang aktif, urut sesuai tampilan. */
    List<Category> findByUserIdIsNullAndActiveTrueOrderByTypeAscSortOrderAsc();
}
