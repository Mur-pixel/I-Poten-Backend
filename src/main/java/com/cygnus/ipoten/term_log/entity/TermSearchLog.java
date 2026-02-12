package com.cygnus.ipoten.term_log.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "term_search_log",
        indexes = {
                @Index(name = "idx_tsl_created_at", columnList = "created_at"),
                @Index(name = "idx_tsl_actor_created_at", columnList = "actor_key, created_at"),
                @Index(name = "idx_tsl_actor_query_created_at", columnList = "actor_key, query_norm, created_at"),
                @Index(name = "idx_tsl_query_created_at", columnList = "query_norm, created_at"),
                @Index(name = "idx_tsl_is_zero_created_at", columnList = "is_zero, created_at")
        })
public class TermSearchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "actor_key", nullable = false, length = 80)
    private String actorKey;

    @Column(name ="query_raw", nullable = false, length = 255)
    private String queryRaw;

    @Column(name ="query_norm", nullable = false, length = 255)
    private String queryNorm;

    @Column(name = "result_count", nullable = false)
    private int resultCount;

    @Column(name = "is_zero", nullable = false)
    private boolean isZero;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "selected_category_id")
    private Long selectedCategoryId;

    @Column(name = "sort_key", length = 20)
    private String sortKey;

    @Column(name = "include_tags", nullable = false)
    private boolean includeTags;

    private TermSearchLog(
            String actorKey, String queryRaw, String queryNorm, int resultCount, boolean isZero, Integer latencyMs, Long selectedCategoryId, String sortKey, boolean includeTags
    ) {
        this.actorKey = actorKey;
        this.queryRaw = queryRaw;
        this.queryNorm = queryNorm;
        this.resultCount = resultCount;
        this.isZero = isZero;
        this.latencyMs = latencyMs;
        this.selectedCategoryId = selectedCategoryId;
        this.sortKey = sortKey;
        this.includeTags = includeTags;
    }

    public static TermSearchLog create(
            String actorKey, String queryRaw, String queryNorm, int resultCount, boolean isZero, Integer latencyMs, Long selectedCategoryId, String sortKey, boolean includeTags
    ) {
        return new TermSearchLog(actorKey, queryRaw, queryNorm, resultCount, isZero, latencyMs, selectedCategoryId, sortKey, includeTags);
    }
}
