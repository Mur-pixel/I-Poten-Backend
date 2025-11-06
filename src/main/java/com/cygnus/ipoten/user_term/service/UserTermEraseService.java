package com.cygnus.ipoten.user_term.service;

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
public class UserTermEraseService {

    @PersistenceContext
    private EntityManager em;

    /**
     * 삭제 결과 요약 DTO.
     * - folders:        실제 삭제된 user_wordbook_folder 개수
     * - wordbookTerms:  실제 삭제된 user_wordbook_term 개수
     * - progresses:     실제 삭제된 user_term_progress 개수
     * - recentTerms:    실제 삭제된 user_recent_term 개수
     */
    @Value
    public static class Result {
        long folders;
        long wordbookTerms;
        long progresses;
        long recentTerms;
    }

    /**
     * 계정 단위로 user_term 도메인 데이터만 삭제한다.
     *  1) 최근본(user_recent_term) / 진행(user_term_progress) 을 계정 기준으로 직접 삭제
     *  2) 폴더 삭제 전에 user_wordbook_term 을 '폴더 조인'으로 직접 삭제 (FK 제약 오류 방지)
     *  3) 마지막으로 user_wordbook_folder 삭제
     */
    @Transactional
    public Result eraseByAccountId(Long accountId) {
        // 운영 모니터링/점검용 사전 집계. 결과 본문에는 사용하지 않지만 실제 삭제 수와 비교해보기 위해 남겨둠
        long foldersBefore = count("SELECT COUNT(*) FROM user_wordbook_folder WHERE account_id=:id", accountId);
        long wbtBefore = count("""
            SELECT COUNT(*)
              FROM user_wordbook_term t
              JOIN user_wordbook_folder f ON f.id = t.folder_id
             WHERE f.account_id = :id
        """, accountId);
        long utpBefore = count("SELECT COUNT(*) FROM user_term_progress WHERE account_id=:id", accountId);
        long urtBefore = count("SELECT COUNT(*) FROM user_recent_term   WHERE account_id=:id", accountId);

        // 1) 최근 본 용어 / 암기 진행 상태: 계정 기준 직접 삭제
        int delUrt = execute("DELETE FROM user_recent_term   WHERE account_id=:id", accountId);
        int delUtp = execute("DELETE FROM user_term_progress WHERE account_id=:id", accountId);

        // 2) 폴더를 지우기 전에, 폴더에 매달린 항목(user_wordbook_term)을 먼저 제거
        //    (현재 스키마는 CASCADE 가 없어 FK 제약 위반 방지용으로 선삭제가 필요)
        int delUwt = execute("""
            DELETE t FROM user_wordbook_term t
            JOIN user_wordbook_folder f ON f.id = t.folder_id
            WHERE f.account_id = :id
        """, accountId);

        // 3) 폴더 삭제 (이 시점에는 해당 계정의 폴더에 매달린 항목이 더 이상 없음)
        int delFolders = execute("DELETE FROM user_wordbook_folder WHERE account_id=:id", accountId);

        log.info(
                "[user-term:erase] accountId={} deleted: folders={}, uwt={}, utp={}, urt={} (before: folders={}, uwt={}, utp={}, urt={})",
                accountId, delFolders, delUwt, delUtp, delUrt,
                foldersBefore, wbtBefore, utpBefore, urtBefore
        );

        // 실제 삭제된 개수만 결과로 반환
        return new Result(delFolders, delUwt, delUtp, delUrt);
    }

    // ---------------------------------------------------------
    // 아래 두 메서드는 공통 유틸:
    //   - count : 네이티브 쿼리의 COUNT(*) 결과를 long 으로 얻는다
    //   - execute: 네이티브 DML(DELETE 등)을 수행하고 영향받은 행 수를 반환
    // 주의) 네이티브 쿼리는 DB 방언/스키마에 의존하므로 변경 시 반드시 점검 필요.
    // ---------------------------------------------------------

    private long count(String sql, Long accountId) {
        Object x = em.createNativeQuery(sql)
                .setParameter("id", accountId)
                .getSingleResult();
        return ((Number) x).longValue();
    }

    private int execute(String sql, Long accountId) {
        return em.createNativeQuery(sql)
                .setParameter("id", accountId)
                .executeUpdate();
    }
}