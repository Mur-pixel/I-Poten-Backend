package com.cygnus.ipoten.wordbook_term.repository;

import com.cygnus.ipoten.term.entity.Term;
import com.cygnus.ipoten.wordbook_term.entity.WordbookTerm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface WordbookTermRepository extends JpaRepository<WordbookTerm, Long> {

    @Query("""
        select max(wbt.createdAt)
        from WordbookTerm wbt
        where wbt.account.id = :accountId
    """)
    Optional<Instant> findLatestCreatedAtByAccountId(@Param("accountId") Long accountId);

    // 조회(목록) - 폴더 소유자 기준 페이지 조회
    @Query(
            value = """
            select wbt
            from WordbookTerm wbt
              join wbt.wordbook wb
              join wb.account acc
              join fetch wbt.term t
            where wb.id = :wordbookId
              and acc.id = :accountId
            """,
            countQuery = """
            select count(wbt)
            from WordbookTerm wbt
              join wbt.wordbook wb
              join wb.account acc
            where wb.id = :wordbookId
              and acc.id = :accountId
            """
    )
    Page<WordbookTerm> findPageByFolderAndOwnerFetch(Long wordbookId, Long accountId, Pageable pageable);

    @Query(
            value = """
                SELECT uwt.*
                FROM wordbook_term uwt
                JOIN term t
                  ON t.id = uwt.term_id
                JOIN wordbook wb
                  ON wb.id = uwt.wordbook_id
                LEFT JOIN learning_progress lp
                  ON lp.account_id = uwt.account_id
                 AND lp.term_id = uwt.term_id
                WHERE uwt.wordbook_id = :wordbookId
                  AND uwt.account_id = :accountId
                ORDER BY
                  CASE
                    WHEN COALESCE(lp.status, 'LEARNING') = 'DONE' THEN 1
                    ELSE 0
                  END ASC,
                  t.title ASC,
                  uwt.id ASC
                """,
            countQuery = """
                SELECT COUNT(*)
                FROM wordbook_term uwt
                WHERE uwt.wordbook_id = :wordbookId
                  AND uwt.account_id = :accountId
                """,
            nativeQuery = true
    )
    Page<WordbookTerm> findPageByFolderAndOwnerOrderByStatusAsc(
            @Param("wordbookId") Long wordbookId,
            @Param("accountId") Long accountId,
            Pageable pageable
    );

    @Query(
            value = """
                SELECT uwt.*
                FROM wordbook_term uwt
                JOIN term t
                  ON t.id = uwt.term_id
                JOIN wordbook wb
                  ON wb.id = uwt.wordbook_id
                LEFT JOIN learning_progress lp
                  ON lp.account_id = uwt.account_id
                 AND lp.term_id = uwt.term_id
                WHERE uwt.wordbook_id = :wordbookId
                  AND uwt.account_id = :accountId
                ORDER BY
                  CASE
                    WHEN COALESCE(lp.status, 'LEARNING') = 'DONE' THEN 1
                    ELSE 0
                  END DESC,
                  t.title ASC,
                  uwt.id ASC
                """,
            countQuery = """
                SELECT COUNT(*)
                FROM wordbook_term uwt
                WHERE uwt.wordbook_id = :wordbookId
                  AND uwt.account_id = :accountId
                """,
            nativeQuery = true
    )
    Page<WordbookTerm> findPageByFolderAndOwnerOrderByStatusDesc(
            @Param("wordbookId") Long wordbookId,
            @Param("accountId") Long accountId,
            Pageable pageable
    );

    // 이동 로직(FOLDER-ONLY, account 불일치 데이터에도 견고)
    // 대상 폴더에 같은 term 존재 여부 (account 조건 제외)
    boolean existsByWordbook_IdAndTerm_Id(Long wordbookId, Long termId);

    // 소스 폴더에 (folder+term) 존재 여부 및 식별자 획득
    Optional<WordbookTerm> findByWordbook_IdAndTerm_Id(Long wordbookId, Long termId);

    // 소스에서 삭제 (folder+term 기준)
    void deleteByWordbook_IdAndTerm_Id(Long wordbookId, Long termId);

    // 대상 폴더의 마지막 정렬 엔트리
    Optional<WordbookTerm> findTopByWordbook_IdOrderBySortOrderDesc(Long wordbookId);

    // 대상 폴더의 최대 sortOrder 값만 조회
    @Query("""
        select coalesce(max(wbt.sortOrder), 0)
        from WordbookTerm wbt
        where wbt.wordbook.id = :wordbookId
    """)
    Integer findMaxSortOrderByWordbook(@Param("wordbookId") Long wordbookId);

    /** (호환용) 기존 account-scoped 메서드들
     * - 기존 코드가 쓰는 곳이 있으면 유지
     * - '이동' 로직에서는 사용하지 말 것
     */
    Optional<WordbookTerm> findByIdAndAccount_Id(Long id, Long accountId);

    boolean existsByAccount_IdAndWordbook_IdAndTerm_Id(Long accountId, Long wordbookId, Long termId);

    Optional<WordbookTerm> findByAccount_IdAndWordbook_IdAndTerm_Id(Long accountId, Long wordbookId, Long termId);

    Optional<WordbookTerm> findTopByAccount_IdAndWordbook_IdOrderBySortOrderDesc(Long accountId, Long wordbookId);

    @Query("""
        select coalesce(max(wbt.sortOrder), 0)
        from WordbookTerm wbt
        where wbt.account.id = :accountId
          and wbt.wordbook.id  = :wordbookId
    """)
    Integer findMaxSortOrderByAccountAndFolder(@Param("accountId") Long accountId,
                                               @Param("wordbookId") Long wordbookId);

    @Query("""
        select count(wbt)
        from WordbookTerm wbt
        where wbt.wordbook.id = :wordbookId
          and wbt.account.id = :accountId
    """)
    long countByWordbookIdAndAccountId(@Param("wordbookId") Long wordbookId,
                                       @Param("accountId") Long accountId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
       update WordbookTerm wbt
          set wbt.wordbook = (
              select wb
              from Wordbook wb
              where wb.id = :targetWordbookId
                and wb.account.id = :accountId
          )
        where wbt.wordbook.id = :sourceWordbookId
          and wbt.account.id = :accountId
       """)
    int bulkUpdateMoveFolder(@Param("sourceWordbookId") Long sourceWordbookId,
                             @Param("targetWordbookId") Long targetWordbookId,
                             @Param("accountId") Long accountId);

    // PURGE: 해당 폴더 내 사용자 항목 자체 삭제
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        delete from WordbookTerm wbt
        where wbt.wordbook.id = :wordbookId
          and wbt.account.id = :accountId
    """)
    int deleteByWordbookIdAndAccountId(@Param("wordbookId") Long wordbookId,
                                       @Param("accountId") Long accountId);

    // BULK PURGE: 여러 폴더 한꺼번에
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        delete from WordbookTerm wbt
        where wbt.account.id = :accountId
          and wbt.wordbook.id in :wordbookIds
    """)
    int deleteByAccountIdAndWordbookIdIn(@Param("accountId") Long accountId,
                                         @Param("wordbookIds") Collection<Long> wordbookIds);

    // termId 일괄 조회
    @Query("""
        select distinct wbt.term.id
        from WordbookTerm wbt
        where wbt.wordbook.id = :wordbookId
          and wbt.account.id = :accountId
          and wbt.term.id is not null
        order by wbt.term.id asc
    """)
    List<Long> findDistinctTermIdsByWordbookAndAccountOrderByTermIdAsc(
            @Param("wordbookId") Long wordbookId,
            @Param("accountId") Long accountId
    );

    // normalized 폴더명으로 Term 목록
    @Query("""
        select wbt.term
        from WordbookTerm wbt
        join wbt.wordbook wb
        where wb.account.id = :accountId
          and wb.normalizedWordbookName = :normalized
        order by wbt.sortOrder asc, wbt.id asc
    """)
    List<Term> findTermsByAccountAndFolderNormalized(@Param("accountId") Long accountId,
                                                     @Param("normalized") String normalizedWordbookName);

    // normalized 폴더명으로 Term ID 목록
    @Query("""
        select wbt.term.id
        from WordbookTerm wbt
        join wbt.wordbook wb
        where wb.account.id = :accountId
          and wb.normalizedWordbookName = :normalized
        order by wbt.sortOrder asc, wbt.id asc
    """)
    List<Long> findTermIdsByAccountAndFolderNormalized(@Param("accountId") Long accountId,
                                                       @Param("normalized") String normalizedWordbookName);

    @Modifying
    int deleteByAccount_IdAndWordbook_IdAndTerm_Id(Long accountId, Long wordbookId, Long termId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    delete from WordbookTerm wbt
    where wbt.account.id = :accountId
      and wbt.wordbook.id = :wordbookId
      and wbt.term.id in :termIds
""")
    int deleteByAccountIdAndWordbookIdAndTermIdIn(
            @Param("accountId") Long accountId,
            @Param("wordbookId") Long wordbookId,
            @Param("termIds") List<Long> termIds
    );

    @Query("""
        select wbt.term
        from WordbookTerm wbt
        where wbt.account.id = :accountId
        order by wbt.sortOrder asc, wbt.id asc
    """)
    List<Term> findTermsByAccount(@Param("accountId") Long accountId);

    @Query("""
        select wbt.term
        from WordbookTerm wbt
        where wbt.account.id = :accountId
          and wbt.wordbook.id = :wordbookId
        order by wbt.sortOrder asc, wbt.id asc
    """)
    List<Term> findTermsByAccountAndFolderStrict(@Param("accountId") Long accountId,
                                                 @Param("wordbookId") Long wordbookId);

    @Query("""
    select distinct wt.term.id
    from WordbookTerm wt
    where wt.wordbook.id = :wordbookId
      and wt.term.id is not null
""")
    Set<Long> findDistinctTermIdsByWordbookId(@Param("wordbookId") Long wordbookId);

    @Query("""
        select min(wbt.wordbook.id)
        from WordbookTerm wbt
        where wbt.account.id = :accountId
          and wbt.term.id = :termId
    """)
    Long findMinWordbookIdByAccountIdAndTermId(
            @Param("accountId") Long accountId,
            @Param("termId") Long termId
    );
}
