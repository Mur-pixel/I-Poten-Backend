package com.cygnus.ipoten.wordbook_admin.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Wordbook 도메인 계정 데이터 정리 서비스.
 *
 * 목표:
 * - 회원탈퇴/운영 배치에서 accountId 하나로 "개인 데이터"만 깔끔히 정리
 * - 공유 데이터(term 등)는 건드리지 않음
 *
 * 정리 순서(안전/제약 고려):
 *  1) learning_progress  : account_id 직접 삭제
 *  2) wordbook_pdf       : account_id 직접 삭제 (FK/개인 산출물)
 *  3) wordbook_term      : wordbook 조인 삭제(혹시 cascade 미적용 환경 대비)
 *  4) wordbook           : account_id 삭제 (wordbook_term이 FK cascade면 여기서 자동 삭제될 수도 있음)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WordbookAdminEraseService {

    @PersistenceContext
    private EntityManager em;

    /**
     * 삭제 결과 요약 DTO
     * - 운영 로그/모니터링/리포팅용으로 "실제 삭제된 행 수"를 반환합니다.
     */
    @Value
    public static class Result {
        long learningProgresses;
        long wordbookPdfs;
        long wordbookTerms;
        long wordbooks;
    }

    @Transactional
    public Result eraseByAccountId(Long accountId) {
        // ===== (선택) 운영 점검용 사전 카운트 =====
        long lpBefore = count("SELECT COUNT(*) FROM learning_progress WHERE account_id = :id", accountId);
        long pdfBefore = count("SELECT COUNT(*) FROM wordbook_pdf WHERE account_id = :id", accountId);
        long wbtBefore = count("""
            SELECT COUNT(*)
              FROM wordbook_term t
              JOIN wordbook w ON w.id = t.wordbook_id
             WHERE w.account_id = :id
        """, accountId);
        long wbBefore = count("SELECT COUNT(*) FROM wordbook WHERE account_id = :id", accountId);

        // ===== 1) 학습 진행: account 기준 =====
        int delLp = execute("DELETE FROM learning_progress WHERE account_id = :id", accountId);

        // ===== 2) PDF 산출물: account 기준 =====
        int delPdf = execute("DELETE FROM wordbook_pdf WHERE account_id = :id", accountId);

        // ===== 3) 단어장-용어 매핑: wordbook 조인으로 선삭제(환경별 cascade 차이 대비) =====
        int delWbt = execute("""
            DELETE t
              FROM wordbook_term t
              JOIN wordbook w ON w.id = t.wordbook_id
             WHERE w.account_id = :id
        """, accountId);

        // ===== 4) 단어장: account 기준 =====
        int delWb = execute("DELETE FROM wordbook WHERE account_id = :id", accountId);

        log.info(
                "[wordbook:erase] accountId={} deleted(lp={}, pdf={}, wbt={}, wb={}) before(lp={}, pdf={}, wbt={}, wb={})",
                accountId, delLp, delPdf, delWbt, delWb,
                lpBefore, pdfBefore, wbtBefore, wbBefore
        );

        return new Result(delLp, delPdf, delWbt, delWb);
    }

    /* ===================== Native Query Util ===================== */

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