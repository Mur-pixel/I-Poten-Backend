package com.cygnus.ipoten.quiz_label.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Locale;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "key")
@Table(
        name = "quiz_label",
        uniqueConstraints = @UniqueConstraint(name = "uk_quiz_label_key", columnNames = "label_key"),
        indexes = @Index(name = "idx_quiz_label_key", columnList = "label_key")
)
public class QuizLabel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 정규화된 키(필터링/중복 방지용): react, vue */
    @Column(name = "label_key", nullable = false, length = 50)
    private String key;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    private QuizLabel(String key, String name) {
        this.key = normalizeKey(key);
    }

    public static QuizLabel create(String key, String name) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("QuizLabel.key는 필수입니다.");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("QuizLabel.name은 필수입니다.");
        return new QuizLabel(key, name);
    }

    private static String normalizeKey(String key) {
        return key.trim().toLowerCase(Locale.ROOT);
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}