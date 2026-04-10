package com.cygnus.iptn.term_trending.repository;

import com.cygnus.iptn.term_trending.entity.TermSearchStatsDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface TermSearchStatsDailyRepository extends JpaRepository<TermSearchStatsDaily, Long> {

    @Query(value = """
        SELECT
            tssd.term_id AS termId,
            t.title AS title,
            SUM(tssd.search_count) AS searchCount,
            MAX(tssd.last_searched_at) AS lastSearchedAt
        FROM term_search_stats_daily tssd
        JOIN term t ON t.id = tssd.term_id
        WHERE tssd.date >= :fromDate AND tssd.date <= :toDate
        GROUP BY tssd.term_id, t.title
        ORDER BY SUM(tssd.search_count) DESC, MAX(tssd.last_searched_at) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<TrendingRow> findTrending(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("limit") int limit
    );

    interface TrendingRow {
        Long getTermId();
        String getTitle();
        long getSearchCount();
        LocalDateTime getLastSearchedAt();
    }

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
    INSERT INTO term_search_stats_daily (date, term_id, search_count, last_searched_at)
    VALUES (:date, :termId, 1, :now)
    ON DUPLICATE KEY UPDATE
      search_count = search_count + 1,
      last_searched_at = GREATEST(last_searched_at, VALUES(last_searched_at))
    """, nativeQuery = true)
    void upsert(@Param("date") LocalDate date,
                @Param("now")  LocalDateTime now,
                @Param("termId") Long termId);
}
