package com.cygnus.ipoten.user_term.repository.query;

import com.cygnus.ipoten.user_term.entity.enums.FolderTermSort;
import com.cygnus.ipoten.user_term.entity.enums.MemorizationStatus;
import com.cygnus.ipoten.user_term.service.view.FolderTermRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserWordbookTermQueryRepository {

    private final EntityManager em;

    public static record PageResult<T>(List<T> items, long total) {}

    public PageResult<FolderTermRow> findFolderTerms(
            Long accountId, Long folderId, int page, int perPage, FolderTermSort sort
    ) {
        String orderClause = switch (sort) {
            case TITLE_ASC  -> "t.title COLLATE utf8mb4_0900_ai_ci ASC, uwt.created_at DESC";
            case TITLE_DESC -> "t.title COLLATE utf8mb4_0900_ai_ci DESC, uwt.created_at DESC";
            case STATUS_ASC -> // LEARNING(기본/NULL) 먼저 -> MEMORIZED
                    "CASE WHEN COALESCE(utp.status, 'LEARNING')='MEMORIZED' THEN 1 ELSE 0 END ASC, " +
                            "t.title COLLATE utf8mb4_0900_ai_ci ASC, uwt.created_at DESC";
            case STATUS_DESC -> // MEMORIZED 먼저
                    "CASE WHEN COALESCE(utp.status, 'LEARNING')='MEMORIZED' THEN 0 ELSE 1 END ASC, " +
                            "t.title COLLATE utf8mb4_0900_ai_ci ASC, uwt.created_at DESC";
            case CREATED_AT_DESC -> "uwt.created_at DESC";
        };

        String baseSelect = """
            SELECT
              uwt.id                                   AS uwt_id,
              t.id                                     AS term_id,
              t.title                                  AS title,
              t.description                            AS description,
              uwt.created_at                           AS created_at,
              COALESCE(utp.status, 'LEARNING')         AS status
            FROM user_wordbook_term uwt
            JOIN user_wordbook_folder f    ON f.id = uwt.folder_id
            JOIN term t                    ON t.id = uwt.term_id
            LEFT JOIN user_term_progress utp
                 ON utp.account_id = :accountId
                AND utp.term_id    = t.id
            WHERE f.account_id = :accountId
              AND f.id         = :folderId
            """;

        String dataSql  = baseSelect + " ORDER BY " + orderClause + " LIMIT :limit OFFSET :offset";
        String countSql = """
            SELECT COUNT(*)
            FROM user_wordbook_term uwt
            JOIN user_wordbook_folder f ON f.id = uwt.folder_id
            WHERE f.account_id = :accountId
              AND f.id         = :folderId
            """;

        Query dq = em.createNativeQuery(dataSql);
        dq.setParameter("accountId", accountId);
        dq.setParameter("folderId",  folderId);
        dq.setParameter("limit",     perPage);
        dq.setParameter("offset",    Math.max(0, page) * perPage);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = dq.getResultList();
        List<FolderTermRow> items = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            Long uwtId   = ((Number) r[0]).longValue();
            Long termId  = ((Number) r[1]).longValue();
            String title = (String) r[2];
            String desc  = (String) r[3];
            java.sql.Timestamp ts = (java.sql.Timestamp) r[4];
            String statusStr = (String) r[5];

            items.add(new FolderTermRow(
                    uwtId,
                    termId,
                    title,
                    desc,
                    ts == null ? null : ts.toLocalDateTime(),
                    statusStr == null ? MemorizationStatus.LEARNING : MemorizationStatus.valueOf(statusStr)
            ));
        }

        Query cq = em.createNativeQuery(countSql);
        cq.setParameter("accountId", accountId);
        cq.setParameter("folderId",  folderId);
        long total = ((Number) cq.getSingleResult()).longValue();

        return new PageResult<>(items, total);
    }
}
