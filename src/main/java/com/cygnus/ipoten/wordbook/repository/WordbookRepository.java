package com.cygnus.ipoten.wordbook.repository;

import com.cygnus.ipoten.wordbook.controller.response_form.MyWordbookListResponseForm;
import com.cygnus.ipoten.wordbook.entity.Wordbook;
import com.cygnus.ipoten.wordbook.repository.projection.WordbookCountRow;
import com.cygnus.ipoten.wordbook.repository.projection.WordbookStatsRow;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WordbookRepository extends JpaRepository<Wordbook, Long> {

    boolean existsByIdAndAccount_Id(Long wordbookId, Long accountId);

    @Query("""
        select new com.cygnus.ipoten.wordbook.repository.projection.WordbookCountRow(
            wb.id,
            wb.wordbookName,
            coalesce(count(distinct wbt.term.id), 0)
        )
        from Wordbook wb
        left join WordbookTerm wbt
               on wbt.wordbook.id = wb.id
        where wb.account.id = :accountId
        group by wb.id, wb.wordbookName
        order by wb.sortOrder asc, wb.id asc
    """)
    List<WordbookCountRow> findMyWordbookWithFavoriteCount(@Param("accountId") Long accountId);

    @Query("""
        select new com.cygnus.ipoten.wordbook.controller.response_form.MyWordbookListResponseForm$Item(
            wb.id,
            wb.wordbookName,
            count(distinct wbt.term.id)
        )
        from Wordbook wb
        left join WordbookTerm wbt
               on wbt.wordbook.id = wb.id
        where wb.account.id = :accountId
        group by wb.id, wb.wordbookName
        order by wb.sortOrder asc, wb.id asc
    """)
    List<MyWordbookListResponseForm.Item> findFolderSummaries(@Param("accountId") Long accountId);

    @Query("""
        select coalesce(max(wb.sortOrder), -1)
        from Wordbook wb
        where wb.account.id = :accountId
    """)
    int findMaxSortOrderByAccountId(@Param("accountId") Long accountId);

    boolean existsByAccount_IdAndNormalizedWordbookNameAndIdNot(Long accountId,
                                                                String normalizedWordbookName,
                                                                Long excludeId);

    List<Wordbook> findAllByAccount_Id(Long accountId);

    List<Wordbook> findAllByAccount_IdOrderBySortOrderAscIdAsc(Long accountId);

    Optional<Wordbook> findByIdAndAccount_Id(Long id, Long accountId);

    @Query("""
        select count(wb)
        from Wordbook wb
        where wb.account.id = :accountId
          and wb.id in :ids
    """)
    long countOwnedByIds(@Param("accountId") Long accountId,
                         @Param("ids") Collection<Long> ids);

    void deleteByAccount_IdAndIdIn(Long accountId, Collection<Long> ids);

    @Query(value = """
      SELECT
        wb.id                                   AS id,
        wb.wordbook_name                        AS wordbookName,
        COUNT(wbt.term_id)                      AS termCount,
        COALESCE(
          SUM(
            CASE WHEN lp.status = 'DONE' THEN 1 ELSE 0 END
          ),
          0
        )                                       AS learnedCount,
        COALESCE(
          GREATEST(
            COALESCE(MAX(wbt.updated_at), wb.updated_at),
            wb.updated_at
          ),
          wb.updated_at
        )                                       AS updatedAt,
        MAX(lp.last_studied_at)                 AS lastStudiedAt
      FROM wordbook_folder wb
      LEFT JOIN wordbook_term wbt
             ON wbt.wordbook_id = wb.id
      LEFT JOIN learning_progress lp
             ON lp.account_id = wb.account_id
            AND lp.term_id    = wbt.term_id
      WHERE wb.account_id = :accountId
      GROUP BY wb.id, wb.wordbook_name, wb.sort_order, wb.updated_at
      ORDER BY wb.sort_order ASC, wb.id ASC
      """, nativeQuery = true)
    List<WordbookStatsRow> findMyWordbookWithStats(@Param("accountId") Long accountId);

    Optional<Wordbook> findByAccountIdAndNormalizedWordbookName(Long accountId,
                                                                String normalizedWordbookName);
}
