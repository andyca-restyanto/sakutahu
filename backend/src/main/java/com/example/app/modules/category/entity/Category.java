// filepath: /backend/src/main/java/com/example/app/modules/category/entity/Category.java
package com.example.app.modules.category.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Master kategori. Kategori bawaan di-seed oleh migrasi V3 (user_id = null). */
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40, updatable = false)
    private String code;

    @Column(nullable = false, length = 60)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CategoryKind kind;

    @Column(nullable = false, length = 7)
    private String color;

    @Column(nullable = false, length = 16)
    private String icon;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    protected Category() {}

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public TransactionType getType() { return type; }
    public CategoryKind getKind() { return kind; }
    public String getColor() { return color; }
    public String getIcon() { return icon; }
    public int getSortOrder() { return sortOrder; }
    public UUID getUserId() { return userId; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
}
