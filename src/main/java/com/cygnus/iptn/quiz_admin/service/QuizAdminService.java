package com.cygnus.iptn.quiz_admin.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * QuizAdminService
 *
 * [목적]
 * - 회원 탈퇴/관리자 정리 시, "Quiz 도메인에서 계정이 생성한/남긴 데이터"를 안전하게 삭제한다.
 *
 * [전략]
 * - 세션/답안/오답노트는 개인정보/사용자 데이터이므로 Hard Delete로 즉시 정리한다.
 * - 문제/보기/세트(quiz_set/quiz_question/quiz_choice)는 "콘텐츠/품질/운영 자산" 성격이므로 여기서 건드리지 않는다.
 *   (콘텐츠 정리는 별도 배치/관리 툴에서 Soft Delete or Archive 정책으로 분리 권장)
 *
 * [주의]
 * - QuizSession은 parent_session_id(셀프 FK)를 갖는다.
 *   → 부모를 삭제하기 전에, 부모를 참조하는 자식 세션을 먼저 삭제해야 FK 에러를 피할 수 있다.
 * - QuizSessionAnswer / QuizWrongNote는 QuizSession과 연결되므로,
 *   → 답안/오답노트를 먼저 지우고 세션을 지우는 순서가 안전하다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuizAdminService {

    @PersistenceContext
    private EntityManager em;

    /**
     * 삭제 결과 요약 (운영 로그/모니터링/리포트용)
     *
     * - wrongNotes: quiz_wrong_note에서 삭제된 행 수
     * - sessionAnswers: session_answer에서 삭제된 행 수 (계정의 세션을 통해 삭제)
     * - sessions: quiz_session에서 삭제된 행 수 (자식+부모 포함 합산)
     *
     * orphan*는 "콘텐츠 정리" 기능을 분리했기 때문에 항상 0으로 유지한다.
     * (추후 배치/관리 API로 이동시키고, 그때 Result를 확장해도 됨)
     */
    @Value
    public static class Result {
        long wrongNotes;
        long sessionAnswers;
        long sessions;

        long orphanChoices;
        long orphanQuestions;
        long orphanSets;
    }

    /**
     * [회원탈퇴/관리자] 계정 기준 퀴즈 데이터 일괄 삭제 (Hard Delete)
     *
     * [삭제 범위]
     * - quiz_wrong_note: account_id 기준 삭제
     * - session_answer: quiz_session(account_id) JOIN 후 삭제
     * - quiz_session: account_id 기준 삭제 (단, parent_session_id 셀프 FK 때문에 "자식 → 부모" 순서 보장)
     *
     * [삭제 순서(중요)]
     *  1) quiz_wrong_note (계정 기준)
     *  2) session_answer (계정의 세션 기준)
     *  3) quiz_session(자식) : "부모가 accountId인 세션"을 참조하는 자식 세션 먼저 삭제
     *  4) quiz_session(부모) : account_id 기준 삭제
     *
     * [콘텐츠(품질/운영) 데이터 처리]
     * - quiz_set / quiz_question / quiz_choice는 여기서 삭제하지 않는다.
     *   → 문제 품질 관리/감사 로그/재학습/운영 분석을 위해 "별도 배치/관리"로 분리한다.
     */
    @Transactional
    public Result eraseByAccountId(Long accountId) {
        if (accountId == null) {
            throw new IllegalArgumentException("accountId는 필수입니다.");
        }

        // (1) 오답노트: account_id 기준으로 바로 삭제 (개인 데이터)
        int delWrong = execute(
                "DELETE FROM quiz_wrong_note WHERE account_id = :id",
                accountId
        );

        // (2) 세션 답안: account의 quiz_session을 JOIN해서 삭제
        // - quiz_session을 먼저 지우면 session_answer FK 때문에 실패할 수 있으므로 답안을 먼저 삭제한다.
        int delSa = execute("""
                DELETE a
                  FROM session_answer a
                  JOIN quiz_session s ON s.id = a.session_id
                 WHERE s.account_id = :id
                """, accountId);

        // (3) 자식 세션 먼저 삭제 (셀프 FK 안전장치)
        // - parent_session_id가 "부모 세션"을 참조하므로,
        //   부모(=accountId 소유 세션)를 삭제하기 전에, 그 부모를 참조하는 자식 세션을 선제 삭제한다.
        // - 자식 세션의 account_id가 항상 동일하다는 보장이 있어도, 데이터 오염/레거시 상황을 대비해 JOIN 방식이 더 안전하다.
        int delChild = execute("""
                DELETE c
                  FROM quiz_session c
                  JOIN quiz_session p ON p.id = c.parent_session_id
                 WHERE p.account_id = :id
                """, accountId);

        // (4) 부모 세션 삭제 (account_id 기준)
        int delSessions = execute("""
                DELETE FROM quiz_session
                 WHERE account_id = :id
                """, accountId);

        // (5) 콘텐츠 정리(quiz_set/quiz_question/quiz_choice)는 여기서 하지 않는다.
        // - orphan 정리는 "품질 로그/운영 정책"이랑 맞물리므로 별도 배치/관리 API로 분리하는 게 안전
        long orphanChoices = 0;
        long orphanQuestions = 0;
        long orphanSets = 0;
        long totalSessions = (long) delChild + (long) delSessions;
        return new Result(delWrong, delSa, totalSessions, orphanChoices, orphanQuestions, orphanSets);
    }

    /* ===================== 내부 유틸 (Native Query 헬퍼) ===================== */

    /**
     * 단일 파라미터(:id) 기반 Native DELETE/UPDATE 실행 헬퍼
     *
     * - accountId를 :id로 바인딩한다.
     * - 반환값은 "영향받은 행 수" (MySQL 기준)
     */
    private int execute(String sql, Long accountId) {
        return em.createNativeQuery(sql)
                .setParameter("id", accountId)
                .executeUpdate();
    }
}
