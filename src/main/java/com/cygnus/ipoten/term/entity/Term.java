package com.cygnus.ipoten.term.entity;

import com.cygnus.ipoten.term_category.entity.TermCategory;
import jakarta.persistence.*;
import lombok.*;

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

    public Term(String title, String description, TermCategory termCategory) {
        this.title = title;
        this.description = description;
        this.termCategory = termCategory;
    }
}
