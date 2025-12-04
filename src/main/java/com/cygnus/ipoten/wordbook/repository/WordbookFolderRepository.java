package com.cygnus.ipoten.wordbook.repository;

import com.cygnus.ipoten.wordbook.controller.response_form.MyFolderListResponseForm;
import com.cygnus.ipoten.wordbook.entity.WordbookFolder;
import com.cygnus.ipoten.wordbook.repository.projection.FolderCountRow;
import com.cygnus.ipoten.wordbook.repository.projection.FolderStatsRow;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WordbookFolderRepository extends JpaRepository<WordbookFolder, Long> {
    boolean existsByIdAndAccount_Id(Long folderId, Long accountId);

    @Query("""
    select new com.cygnus.ipoten.wordbook.repository.projection.FolderCountRow(
        f.id,
        f.folderName,
        coalesce(count(distinct uwt.term.id), 0)
    )
    from WordbookFolder f
    left join WordbookTerm uwt
           on uwt.folder.id = f.id
    where f.account.id = :accountId
    group by f.id, f.folderName
    order by f.sortOrder asc, f.id asc
    """)
    List<FolderCountRow> findMyFoldersWithFavoriteCount(@Param("accountId") Long accountId);

    @Query("""
    select new com.cygnus.ipoten.wordbook.controller.response_form.MyFolderListResponseForm$Item(
        f.id,
        f.folderName,
        count(distinct uwt.term.id)
    )
    from WordbookFolder f
    left join WordbookTerm uwt
           on uwt.folder.id = f.id
    where f.account.id = :accountId
    group by f.id, f.folderName
    order by f.sortOrder asc, f.id asc
    """)
    List<MyFolderListResponseForm.Item> findFolderSummaries(@Param("accountId") Long accountId);

    @Query("select coalesce(max(f.sortOrder), -1) from WordbookFolder f where f.account.id = :accountId")
    int findMaxSortOrderByAccountId(@Param("accountId") Long accountId);

    boolean existsByAccount_IdAndNormalizedFolderName(Long accountId, String normalizedFolderName);
    List<WordbookFolder> findAllByAccount_Id(Long accountId);
    List<WordbookFolder> findAllByAccount_IdOrderBySortOrderAscIdAsc(Long accountId);

    boolean existsByAccount_IdAndNormalizedFolderNameAndIdNot(Long accountId, String folderName, Long excludeFolderId);
    Optional<WordbookFolder> findByIdAndAccount_Id(Long id, Long accountId);
    @Query("select count(f) from WordbookFolder f where f.account.id = :accountId and f.id in :ids")
    long countOwnedByIds(@Param("accountId") Long accountId, @Param("ids") Collection<Long> ids);
    void deleteByAccount_IdAndIdIn(Long accountId, Collection<Long> ids);

    @Query(value = """
      SELECT
        f.id                                  AS id,
        f.folder_name                         AS folderName,
        COUNT(uwt.term_id)                    AS termCount,
        COALESCE(SUM(CASE WHEN utp.status = 'DONE' THEN 1 ELSE 0 END), 0) AS learnedCount,
        COALESCE(
          GREATEST(
            COALESCE(MAX(uwt.updated_at), f.updated_at),
            f.updated_at
          ),
          f.updated_at
        )                                     AS updatedAt,
        MAX(utp.last_studied_at)              AS lastStudiedAt
      FROM wordbook_folder f
      LEFT JOIN wordbook_term uwt
             ON uwt.folder_id = f.id
      LEFT JOIN learning_progress lp
             ON lp.account_id = f.account_id
            AND lp.term_id    = uwt.term_id
      WHERE f.account_id = :accountId
      GROUP BY f.id, f.folder_name, f.sort_order, f.updated_at
      ORDER BY f.sort_order ASC, f.id ASC
      """, nativeQuery = true)
    List<FolderStatsRow> findMyFoldersWithStats(@Param("accountId") Long accountId);
    Optional<WordbookFolder> findByAccountIdAndNormalizedFolderName(Long accountId, String normalizedFolderName);
}
