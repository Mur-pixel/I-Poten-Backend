package com.cygnus.ipoten.term.entity;

import com.cygnus.ipoten.term_category.entity.TermCategory;
import com.cygnus.ipoten.term_topic_tag.entity.TopicTag;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "term")
public class Term {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(nullable = false)
    private String title;

    @Setter
    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "category_id", 
            nullable = true,
            foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT)) // FK 생성 안 함
    private TermCategory termCategory;

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "term_topic_tag",
            joinColumns = @JoinColumn(name = "term_id"),
            inverseJoinColumns = @JoinColumn(name = "topic_tag_id"),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_term_topic_tag",
                    columnNames = {"term_id", "topic_tag_id"}
            )
    )
    private Set<TopicTag> topicTags = new HashSet<>();

    public Term(String title, String description, TermCategory termCategory) {
        this.title = title;
        this.description = description;
        this.termCategory = termCategory;
    }
}
