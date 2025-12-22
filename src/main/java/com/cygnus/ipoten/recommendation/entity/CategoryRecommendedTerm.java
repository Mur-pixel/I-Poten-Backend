package com.cygnus.ipoten.recommendation.entity;

import com.cygnus.ipoten.term.entity.Term;
import com.cygnus.ipoten.term_category.entity.TermCategory;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
        name = "category_recommended_term",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_track_rank", columnNames = {"term_category_id", "rank_no"}),
                @UniqueConstraint(name = "uk_track_term", columnNames = {"term_category_id", "term_id"})
        },
        indexes = {
                @Index(name = "idx_crt_track", columnList = "term_category_id"),
                @Index(name = "idx_crt_term", columnList = "term_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CategoryRecommendedTerm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 추천 카테고리 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_category_id", nullable = false)
    private TermCategory termCategory;

    /** 직무 추천에 포함된 실제 용어 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    /** 직무 내에서의 노출 순서 (1~100) */
    @Column(name = "rank_no", nullable = false)
    private int rankNo;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    private CategoryRecommendedTerm(TermCategory termCategory, Term term, int rankNo) {
        if (termCategory == null) throw new IllegalArgumentException("termCategory는 필수입니다.");
        if (term == null) throw new IllegalArgumentException("term은 필수입니다.");
        if (rankNo <= 0 || rankNo > 100) throw new IllegalArgumentException("rankNo는 1~100이어야 합니다.");
        this.termCategory = termCategory;
        this.term = term;
        this.rankNo = rankNo;
    }

    public static CategoryRecommendedTerm create(TermCategory trackCategory, Term term, int rankNo) {
        return new CategoryRecommendedTerm(trackCategory, term, rankNo);
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
