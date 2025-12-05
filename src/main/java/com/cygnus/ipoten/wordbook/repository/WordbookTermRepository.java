package com.cygnus.ipoten.wordbook.repository;

import com.cygnus.ipoten.term.entity.Term;
import com.cygnus.ipoten.wordbook.entity.WordbookTerm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WordbookTermRepository extends JpaRepository<WordbookTerm, Long> {

    //조회(목록) - 폴더 소유자 기준 페이지 조회
    @Query(
            value = """
            select uwt
            from WordbookTerm uwt
              join uwt.folder f
              join f.account a
              join fetch uwt.term t
            where f.id = :folderId
              and a.id = :accountId
            """,
            countQuery = """
            select count(uwt)
            from WordbookTerm uwt
              join uwt.folder f
              join f.account a
            where f.id = :folderId
              and a.id = :accountId
            """
    )
    Page<WordbookTerm> findPageByFolderAndOwnerFetch(
            @Param("folderId") Long folderId,
            @Param("accountId") Long accountId,
            Pageable pageable
    );

    //이동 로직(FOLDER-ONLY, account 불일치 데이터에도 견고)
    // 대상 폴더에 같은 term 존재 여부 (account 조건 제외)
    boolean existsByFolder_IdAndTerm_Id(Long folderId, Long termId);

    // 소스 폴더에 (folder+term) 존재 여부 및 식별자 획득
    Optional<WordbookTerm> findByFolder_IdAndTerm_Id(Long folderId, Long termId);

    // 소스에서 삭제 (folder+term 기준)
    void deleteByFolder_IdAndTerm_Id(Long folderId, Long termId);

    // 대상 폴더의 마지막 정렬 엔트리
    Optional<WordbookTerm> findTopByFolder_IdOrderBySortOrderDesc(Long folderId);

    // 대상 폴더의 최대 sortOrder 값만 조회
    @Query("""
        select coalesce(max(uwt.sortOrder), 0)
        from WordbookTerm uwt
        where uwt.folder.id = :folderId
    """)
    Integer findMaxSortOrderByFolder(@Param("folderId") Long folderId);

    /** (호환용) 기존 account-scoped 메서드들
     * - 기존 코드가 쓰는 곳이 있으면 유지
     * - '이동' 로직에서는 사용하지 말 것
     */
    Optional<WordbookTerm> findByIdAndAccount_Id(Long id, Long accountId);

    boolean existsByAccount_IdAndFolder_IdAndTerm_Id(Long accountId, Long folderId, Long termId);

    Optional<WordbookTerm> findByAccount_IdAndFolder_IdAndTerm_Id(Long accountId, Long folderId, Long termId);

    Optional<WordbookTerm> findTopByAccount_IdAndFolder_IdOrderBySortOrderDesc(Long accountId, Long folderId);

    @Query("""
        select coalesce(max(uwt.sortOrder), 0)
        from WordbookTerm uwt
        where uwt.account.id = :accountId
          and uwt.folder.id  = :folderId
    """)
    Integer findMaxSortOrderByAccountAndFolder(@Param("accountId") Long accountId,
                                               @Param("folderId") Long folderId);

    @Query("select count(t) from WordbookTerm t where t.folder.id = :folderId and t.account.id = :accountId")
    long countByFolderIdAndAccountId(@Param("folderId") Long folderId, @Param("accountId") Long accountId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
       update WordbookTerm t
          set t.folder = (
              select f
              from WordbookFolder f
              where f.id = :targetFolderId
                and f.account.id = :accountId
          )
        where t.folder.id = :sourceFolderId
          and t.account.id = :accountId
       """)
    int bulkUpdateMoveFolder(@Param("sourceFolderId") Long sourceFolderId,
                             @Param("targetFolderId") Long targetFolderId,
                             @Param("accountId") Long accountId);

    // PURGE: 해당 폴더 내 사용자 항목 자체 삭제
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from WordbookTerm t where t.folder.id = :folderId and t.account.id = :accountId")
    int deleteByFolderIdAndAccountId(@Param("folderId") Long folderId, @Param("accountId") Long accountId);

    // BULK PURGE: 여러 폴더 한꺼번에
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from WordbookTerm t where t.account.id = :accountId and t.folder.id in :folderIds")
    int deleteByAccountIdAndFolderIdIn(@Param("accountId") Long accountId, @Param("folderIds") Collection<Long> folderIds);

    // termId 일괄 조회
    @Query("""
        select distinct uwt.term.id
        from WordbookTerm uwt
        where uwt.folder.id = :folderId
        and uwt.account.id = :accountId
        and uwt.term.id is not null
        order by uwt.term.id asc
    """)
    List<Long> findDistinctTermIdsByFolderAndAccountOrderByTermIdAsc(
            @Param("folderId") Long folderId,
            @Param("accountId") Long accountId
    );

    // normalized 폴더명으로 Term 목록
    @Query("""
        select t.term
        from WordbookTerm t
        join t.folder f
        where f.account.id = :accountId
          and f.normalizedFolderName = :normalized
        order by t.sortOrder asc, t.id asc
    """)
    List<Term> findTermsByAccountAndFolderNormalized(@Param("accountId") Long accountId,
                                                     @Param("normalized") String normalizedFolderName);

    // normalized 폴더명으로 Term ID 목록
    @Query("""
        select t.term.id
        from WordbookTerm t
        join t.folder f
        where f.account.id = :accountId
          and f.normalizedFolderName = :normalized
        order by t.sortOrder asc, t.id asc
    """)
    List<Long> findTermIdsByAccountAndFolderNormalized(@Param("accountId") Long accountId,
                                                       @Param("normalized") String normalizedFolderName);

    @Modifying
    int deleteByAccount_IdAndFolder_IdAndTerm_Id(Long accountId, Long folderId, Long termId);

    @Query("""
    select t.term
    from WordbookTerm t
    where t.account.id = :accountId
    order by t.sortOrder asc, t.id asc
    """)
    List<Term> findTermsByAccount(@Param("accountId") Long accountId);

    @Query("""
    select t.term
    from WordbookTerm t
    where t.account.id = :accountId
      and t.folder.id = :folderId
    order by t.sortOrder asc, t.id asc
    """)
    List<Term> findTermsByAccountAndFolderStrict(@Param("accountId") Long accountId,
                                                 @Param("folderId") Long folderId);
}
