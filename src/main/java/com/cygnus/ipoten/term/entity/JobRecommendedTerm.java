package com.cygnus.ipoten.term.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "job_recommended_term",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_job_rank", columnNames = {"job_key", "rank_no"}),
                @UniqueConstraint(name = "uk_job_term", columnNames = {"job_key", "term_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class JobRecommendedTerm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FRONTEND, BACKEND ... */
    @Column(name = "job_key", nullable = false, length = 50)
    private String jobKey;

    /** 직무 추천에 포함된 실제 용어 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    /** 이 추천 용어가 속한 카테고리 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /** 직무 내에서의 노출 순서 (1~100) */
    @Column(name = "rank_no", nullable = false)
    private int rankNo;
}
