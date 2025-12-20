package com.cygnus.ipoten.term_topic_tag.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Locale;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "key")
@Table(
        name = "topic_tag",
        uniqueConstraints = @UniqueConstraint(name = "uk_topic_tag_key", columnNames = "tag_key"),
        indexes = @Index(name = "idx_topic_tag_key", columnList = "tag_key")
)
public class TopicTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tag_key", nullable = false, length = 50)
    private String key;

    private TopicTag(String key) {
        this.key = normalizeKey(key);
    }

    public static TopicTag create(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("TopicTag.key는 필수입니다.");
        }
        return new TopicTag(key);
    }

    private static String normalizeKey(String key) {
        return key.trim().toLowerCase(Locale.ROOT);
    }
}
