package com.cygnus.ipoten.wordbook_pdf.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WordbookPdfEraseService {

    @PersistenceContext
    private EntityManager em;

    /**
     * 삭제 결과 요약 객체
     */
    @Value
    public static class Result {
        long ebooks;  // 삭제된 ebook 행 수
    }

    /**
     * 계정 기준으로 ebook 도메인 데이터를 삭제한다.
     *
     * 삭제 순서:
     *  ebook WHERE account_id=:id 삭제
     */
    @Transactional
    public Result eraseByAccountId(Long accountId) {
        // 계정 기준 일괄 삭제
        int delEbooks = execute("DELETE FROM ebook WHERE account_id = :id", accountId);

        log.info("[ebook:erase] deleted: ebooks={}", delEbooks);
        return new Result(delEbooks);
    }

    /** 단일 파라미터 바인딩으로 실행하는 네이티브 DML 헬퍼 */
    private int execute(String sql, Long accountId) {
        return em.createNativeQuery(sql)
                .setParameter("id", accountId)
                .executeUpdate();
    }
}
