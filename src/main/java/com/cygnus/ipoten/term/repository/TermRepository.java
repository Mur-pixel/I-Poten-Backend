package com.cygnus.ipoten.term.repository;

import com.cygnus.ipoten.term.entity.Term;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface TermRepository extends JpaRepository<Term, Long>, JpaSpecificationExecutor<Term> {

    boolean existsByTermCategory_IdAndTitle(Long categoryId, String title);
    Optional<Term> findByTermCategoryIdAndTitle(Long categoryId, String title);

    @EntityGraph(attributePaths = "termCategory")
    Page<Term> findAll(Pageable pageable);

    /* ---------------- 기본 LIKE / 연관도 검색 ---------------- */

    @Query("""
        SELECT DISTINCT t FROM Term t
        LEFT JOIN com.cygnus.ipoten.term.entity.TermTag tt ON tt.term = t
        LEFT JOIN com.cygnus.ipoten.term.entity.Tag tag ON tag = tt.tag
        WHERE LOWER(t.title)       LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(t.description) LIKE LOWER(CONCAT('%', :q, '%'))
           OR (:includeTags = TRUE AND LOWER(tag.name) LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    Page<Term> searchLike(@Param("q") String q,
                          @Param("includeTags") boolean includeTags,
                          Pageable pageable);

    @Query(value = """
        SELECT 
            t.*,
            (
              CASE
                WHEN LOWER(t.title) = LOWER(:q)                      THEN 100
                WHEN LOWER(t.title) LIKE CONCAT(LOWER(:q), '%')      THEN 80
                WHEN LOWER(t.title) LIKE CONCAT('%', LOWER(:q), '%') THEN 60
                ELSE 0
              END
            )
            + (CASE WHEN LOWER(t.description) LIKE CONCAT('%', LOWER(:q), '%') THEN 10 ELSE 0 END)
            + (CASE WHEN :includeTags = TRUE AND EXISTS (
                    SELECT 1
                      FROM term_tag tt
                      JOIN tag tg ON tg.id = tt.tag_id
                     WHERE tt.term_id = t.id
                       AND LOWER(tg.name) LIKE CONCAT('%', LOWER(:q), '%')
                 ) THEN 5 ELSE 0 END) AS score
        FROM term t
        WHERE
               LOWER(t.title)       LIKE CONCAT('%', LOWER(:q), '%')
            OR LOWER(t.description) LIKE CONCAT('%', LOWER(:q), '%')
            OR (:includeTags = TRUE AND EXISTS (
                  SELECT 1 FROM term_tag tt
                  JOIN tag tg ON tg.id = tt.tag_id
                 WHERE tt.term_id = t.id
                   AND LOWER(tg.name) LIKE CONCAT('%', LOWER(:q), '%')
               ))
        ORDER BY score DESC, t.id DESC, t.title ASC
        """,
            countQuery = """
        SELECT COUNT(*)
          FROM term t
         WHERE
               LOWER(t.title)       LIKE CONCAT('%', LOWER(:q), '%')
            OR LOWER(t.description) LIKE CONCAT('%', LOWER(:q), '%')
            OR (:includeTags = TRUE AND EXISTS (
                  SELECT 1 FROM term_tag tt
                  JOIN tag tg ON tg.id = tt.tag_id
                 WHERE tt.term_id = t.id
                   AND LOWER(tg.name) LIKE CONCAT('%', LOWER(:q), '%')
               ))
        """,
            nativeQuery = true)
    Page<Term> searchByRelevance(@Param("q") String q,
                                 @Param("includeTags") boolean includeTags,
                                 Pageable pageable);

    /* 태그 기반 조회 */
    @Query(
        value = """
          SELECT DISTINCT t
            FROM Term t
            JOIN FETCH t.termCategory c
            JOIN com.cygnus.ipoten.term.entity.TermTag tt ON tt.term = t
            JOIN tt.tag tg
           WHERE LOWER(TRIM(BOTH FROM tg.name)) = LOWER(TRIM(BOTH FROM :tag))
        """,
        countQuery = """
          SELECT COUNT(DISTINCT t)
            FROM Term t
            JOIN com.cygnus.ipoten.term.entity.TermTag tt ON tt.term = t
            JOIN tt.tag tg
           WHERE LOWER(TRIM(BOTH FROM tg.name)) = LOWER(TRIM(BOTH FROM :tag))
        """
    )
    Page<Term> findByTagNameIgnoreCase(@Param("tag") String tag, Pageable pageable);

    /* ---------------- 접두(prefix) 검색 (카테고리 미포함) ---------------- */

    @Query(
            value = """
        SELECT *
          FROM term t
         WHERE LEFT(REGEXP_REPLACE(t.title, '^[[:space:][:punct:]]+', ''), 1) >= :fromCh
           AND LEFT(REGEXP_REPLACE(t.title, '^[[:space:][:punct:]]+', ''), 1) <  :toCh
         ORDER BY t.title ASC
        """,
            countQuery = """
        SELECT COUNT(*)
          FROM term t
         WHERE LEFT(REGEXP_REPLACE(t.title, '^[[:space:][:punct:]]+', ''), 1) >= :fromCh
           AND LEFT(REGEXP_REPLACE(t.title, '^[[:space:][:punct:]]+', ''), 1) <  :toCh
        """,
            nativeQuery = true
    )
    Page<Term> findByHangulInitialRange(@Param("fromCh") String fromCh,
                                        @Param("toCh")   String toCh,
                                        Pageable pageable);

    @Query(
            value = """
        SELECT *
          FROM term t
         WHERE LOWER(LEFT(t.title, 1)) = LOWER(:ch)
         ORDER BY t.title ASC
        """,
            countQuery = """
        SELECT COUNT(*)
          FROM term t
         WHERE LOWER(LEFT(t.title, 1)) = LOWER(:ch)
        """,
            nativeQuery = true
    )
    Page<Term> findByFirstAlpha(@Param("ch") String ch, Pageable pageable);

    @Query(
            value = """
        SELECT *
          FROM term t
         WHERE LEFT(t.title, 1) = :sym
         ORDER BY t.title ASC
        """,
            countQuery = """
        SELECT COUNT(*)
          FROM term t
         WHERE LEFT(t.title, 1) = :sym
        """,
            nativeQuery = true
    )
    Page<Term> findByFirstSymbol(@Param("sym") String sym, Pageable pageable);

    /* ---------------- 카테고리 필터 버전 ---------------- */

    @Query("SELECT t FROM Term t WHERE t.termCategory.id IN :catIds")
    Page<Term> findByTermCategoryIdIn(@Param("catIds") Collection<Long> catIds, Pageable pageable);

    @Query("""
        SELECT t FROM Term t
        WHERE t.title >= :start AND t.title < :end
          AND t.termCategory.id IN :catIds
        """)
    Page<Term> findByHangulInitialRangeInCategories(@Param("start") String start,
                                                    @Param("end") String end,
                                                    @Param("catIds") Collection<Long> catIds,
                                                    Pageable pageable);

    @Query("""
        SELECT t FROM Term t
        WHERE UPPER(SUBSTRING(t.title,1,1)) = UPPER(:alpha)
          AND t.termCategory.id IN :catIds
        """)
    Page<Term> findByFirstAlphaInCategories(@Param("alpha") String alpha,
                                            @Param("catIds") Collection<Long> catIds,
                                            Pageable pageable);

    @Query("""
        SELECT t FROM Term t
        WHERE SUBSTRING(t.title,1,1) = :symbol
          AND t.termCategory.id IN :catIds
        """)
    Page<Term> findByFirstSymbolInCategories(@Param("symbol") String symbol,
                                             @Param("catIds") Collection<Long> catIds,
                                             Pageable pageable);

    @Query("""
        SELECT t FROM Term t
        WHERE t.termCategory.id IN :catIds AND (
              LOWER(t.title)       LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(t.description) LIKE LOWER(CONCAT('%', :q, '%'))
           OR (:includeTags = TRUE AND EXISTS (
                SELECT 1 FROM com.cygnus.ipoten.term.entity.TermTag tt
                 WHERE tt.term = t
                   AND LOWER(tt.tag.name) LIKE LOWER(CONCAT('%', :q, '%'))
           ))
        )
        """)
    Page<Term> searchLikeInCategories(@Param("q") String q,
                                      @Param("includeTags") boolean includeTags,
                                      @Param("catIds") Collection<Long> catIds,
                                      Pageable pageable);

    /* 연관도(가중치) + 카테고리 필터 (native) */
    @Query(value = """
    SELECT 
        t.*,
        (
          CASE
            WHEN LOWER(t.title) = LOWER(:q)                      THEN 100
            WHEN LOWER(t.title) LIKE CONCAT(LOWER(:q), '%')      THEN 80
            WHEN LOWER(t.title) LIKE CONCAT('%', LOWER(:q), '%') THEN 60
            ELSE 0
          END
        )
        + (CASE WHEN LOWER(t.description) LIKE CONCAT('%', LOWER(:q), '%') THEN 10 ELSE 0 END)
        + (CASE WHEN :includeTags = TRUE AND EXISTS (
                SELECT 1
                  FROM term_tag tt
                  JOIN tag tg ON tg.id = tt.tag_id
                 WHERE tt.term_id = t.id
                   AND LOWER(tg.name) LIKE CONCAT('%', LOWER(:q), '%')
           ) THEN 5 ELSE 0 END) AS score
    FROM term t
    WHERE (
           LOWER(t.title)       LIKE CONCAT('%', LOWER(:q), '%')
        OR LOWER(t.description) LIKE CONCAT('%', LOWER(:q), '%')
        OR (:includeTags = TRUE AND EXISTS (
              SELECT 1 FROM term_tag tt
              JOIN tag tg ON tg.id = tt.tag_id
             WHERE tt.term_id = t.id
               AND LOWER(tg.name) LIKE CONCAT('%', LOWER(:q), '%')
           ))
    )
    AND t.termCategory_id IN (:catIds)
    ORDER BY score DESC, t.id DESC, t.title ASC
    """,
                countQuery = """
    SELECT COUNT(*)
    FROM term t
    WHERE (
           LOWER(t.title)       LIKE CONCAT('%', LOWER(:q), '%')
        OR LOWER(t.description) LIKE CONCAT('%', LOWER(:q), '%')
        OR (:includeTags = TRUE AND EXISTS (
              SELECT 1 FROM term_tag tt
              JOIN tag tg ON tg.id = tt.tag_id
             WHERE tt.term_id = t.id
               AND LOWER(tg.name) LIKE CONCAT('%', LOWER(:q), '%')
           ))
    )
    AND t.termCategory_id IN (:catIds)
    """,
                nativeQuery = true)
    Page<Term> searchByRelevanceInCategories(@Param("q") String q,
                                             @Param("includeTags") boolean includeTags,
                                             @Param("catIds") Collection<Long> catIds,
                                             Pageable pageable);

    Optional<Term> findFirstByTitleAndTermCategory_Id(String title, Long categoryId);
}
