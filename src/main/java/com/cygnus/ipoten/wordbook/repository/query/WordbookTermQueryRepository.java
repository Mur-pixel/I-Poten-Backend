package com.cygnus.ipoten.wordbook.repository.query;

import com.cygnus.ipoten.wordbook.entity.enums.WordbookTermSort;
import com.cygnus.ipoten.wordbook_learning.entity.enums.LearningStatus;
import com.cygnus.ipoten.wordbook.service.view.FolderTermRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class WordbookTermQueryRepository {

    private final EntityManager em;

    public static record PageResult<T>(List<T> items, long total) {}

    public PageResult<FolderTermRow> findFolderTerms(
            Long accountId, Long wordbookId, int page, int perPage, WordbookTermSort sort
    ) {
        String orderClause = switch (sort) {
            case TITLE_ASC  -> "t.title COLLATE utf8mb4_0900_ai_ci ASC, wbt.created_at DESC";
            case TITLE_DESC -> "t.title COLLATE utf8mb4_0900_ai_ci DESC, wbt.created_at DESC";
            case STATUS_ASC -> // LEARNING(기본/NULL) 먼저 -> DONE
                    "CASE WHEN COALESCE(lp.status, 'LEARNING')='DONE' THEN 1 ELSE 0 END ASC, " +
                            "t.title COLLATE utf8mb4_0900_ai_ci ASC, wbt.created_at DESC";
            case STATUS_DESC -> // DONE 먼저
                    "CASE WHEN COALESCE(lp.status, 'LEARNING')='DONE' THEN 0 ELSE 1 END ASC, " +
                            "t.title COLLATE utf8mb4_0900_ai_ci ASC, wbt.created_at DESC";
            case CREATED_AT_DESC -> "wbt.created_at DESC";
        };

        String baseSelect = """
            SELECT
              wbt.id                                 AS wordbook_term_id,
              t.id                                   AS term_id,
              t.title                                AS title,
              t.description                          AS description,
              wbt.created_at                         AS created_at,
              COALESCE(lp.status, 'LEARNING')       AS status
            FROM wordbook_term wbt
            JOIN wordbook_folder wb
              ON wb.id = wbt.wordbook_id
            JOIN term t
              ON t.id = wbt.term_id
            LEFT JOIN learning_progress lp
              ON lp.account_id = :accountId
             AND lp.term_id    = t.id
            WHERE wb.account_id = :accountId
              AND wb.id         = :wordbookId
            """;

        String dataSql  = baseSelect + " ORDER BY " + orderClause + " LIMIT :limit OFFSET :offset";
        String countSql = """
            SELECT COUNT(*)
            FROM wordbook_term wbt
            JOIN wordbook_folder wb
              ON wb.id = wbt.wordbook_id
            WHERE wb.account_id = :accountId
              AND wb.id         = :wordbookId
            """;

        Query dq = em.createNativeQuery(dataSql);
        dq.setParameter("accountId", accountId);
        dq.setParameter("wordbookId",  wordbookId);
        dq.setParameter("limit",     perPage);
        dq.setParameter("offset",    Math.max(0, page) * perPage);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = dq.getResultList();
        List<FolderTermRow> items = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            Long wordbookTermId = ((Number) r[0]).longValue();
            Long termId         = ((Number) r[1]).longValue();
            String title        = (String) r[2];
            String desc         = (String) r[3];
            java.sql.Timestamp ts = (java.sql.Timestamp) r[4];
            String statusStr    = (String) r[5];

            items.add(new FolderTermRow(
                    wordbookTermId,
                    termId,
                    title,
                    desc,
                    ts == null ? null : ts.toLocalDateTime(),
                    statusStr == null ? LearningStatus.LEARNING : LearningStatus.valueOf(statusStr)
            ));
        }

        Query cq = em.createNativeQuery(countSql);
        cq.setParameter("accountId", accountId);
        cq.setParameter("wordbookId",  wordbookId);
        long total = ((Number) cq.getSingleResult()).longValue();

        return new PageResult<>(items, total);
    }
}
