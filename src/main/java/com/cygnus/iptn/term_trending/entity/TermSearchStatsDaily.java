package com.cygnus.iptn.term_trending.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "term_search_stats_daily",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tssd_date_term", columnNames = {"date", "term_id"})
        },
        indexes = {
                @Index(name = "idx_tssd_date", columnList = "date"),
                @Index(name = "idx_tssd_term", columnList = "term_id")
        }
)
public class TermSearchStatsDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "term_id", nullable = false)
    private Long termId;

    @Column(name = "search_count", nullable = false)
    private long searchCount;

    @Column(name = "last_searched_at")
    private LocalDateTime lastSearchedAt;
}
