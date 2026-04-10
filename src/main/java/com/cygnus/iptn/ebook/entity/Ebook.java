package com.cygnus.iptn.ebook.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "ebook",
        indexes = {
                @Index(name = "idx_ebook_created_at", columnList = "created_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ebook_file_key", columnNames = "file_key")
        }
)
public class Ebook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    /** storage-dir 기준 상대 경로 ex) "fe/react-intro.pdf" */
    @Column(name = "file_key", nullable = false, length = 255)
    private String fileKey;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    private Ebook(String title, String fileKey, boolean isPublic) {
        this.title = title;
        this.fileKey = fileKey;
        this.isPublic = isPublic;
    }

    public static Ebook create(String title, String fileKey, boolean isPublic) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("title required");
        if (fileKey == null || fileKey.isBlank()) throw new IllegalArgumentException("fileKey required");
        return new Ebook(title.trim(), fileKey.trim(), isPublic);
    }
}
