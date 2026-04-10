package com.cygnus.iptn.recommendation.entity;

import com.cygnus.iptn.recommendation.entity.enums.JobKey;
import com.cygnus.iptn.term.entity.Term;
import com.cygnus.iptn.term_category.entity.TermCategory;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "job_recommended_term",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_jrt_job_rank", columnNames = {"job_key", "rank_no"}),
                @UniqueConstraint(name = "uk_jrt_job_term", columnNames = {"job_key", "term_id"})
        },
        indexes = {
                @Index(name = "idx_jrt_job_rank", columnList = "job_key, rank_no"),
                @Index(name = "idx_jrt_term", columnList = "term_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class JobRecommendedTerm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 추천 직무 키 */
    @Enumerated(EnumType.STRING)
    @Column(name = "job_key", nullable = false, length = 32)
    private JobKey jobKey;

    /** 추천에 포함된 실제 용어 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    /** 추천에 포함된 실제 용어 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private TermCategory termCategory;

    /** 직무 내에서의 노출 순서 (1~100) */
    @Column(name = "rank_no", nullable = false)
    private int rankNo;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    private JobRecommendedTerm(JobKey jobKey, Term term, TermCategory termCategory, int rankNo) {
        if (jobKey == null) throw new IllegalArgumentException("jobKey는 필수입니다.");
        if (term == null) throw new IllegalArgumentException("term은 필수입니다.");
        if (termCategory == null) throw new IllegalArgumentException("category는 필수입니다.");
        if (rankNo <= 0 || rankNo > 100) throw new IllegalArgumentException("rankNo는 1~100이어야 합니다.");
        this.jobKey = jobKey;
        this.term = term;
        this.termCategory = termCategory;
        this.rankNo = rankNo;
    }

    public static JobRecommendedTerm create(JobKey jobKey, Term term, TermCategory termCategory, int rankNo) {
        return new JobRecommendedTerm(jobKey, term, termCategory, rankNo);
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
