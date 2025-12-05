package com.cygnus.ipoten.quiz_admin.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizAdminService {

    @PersistenceContext
    private EntityManager em;

    /**
     * 삭제 결과 요약
     * - 각 단계에서 "실제로 삭제된 행 수"를 담아 운영/로깅에 사용.
     */
    @Value
    public static class Result {
        long wrongNotes;        // user_wrong_note (해당 계정)
        long sessionAnswers;    // session_answer (해당 계정의 세션을 통해 삭제)
        long sessions;          // user_quiz_session (해당 계정)
        long orphanChoices;     // quiz_choice (고아 세트 정리 과정에서 삭제된 개수)
        long orphanQuestions;   // quiz_question (고아 세트 정리 과정에서 삭제된 개수)
        long orphanSets;        // quiz_set (더 이상 어떤 세션에서도 참조되지 않는 세트)
    }

    /**
     * [핵심 규칙]
     * - "계정 데이터"만 지운다: user_quiz_session, session_answer(해당 세션들), user_wrong_note(해당 계정).
     * - quiz_set / quiz_question / quiz_choice는 '공유(재사용) 가능'한 구조일 수 있으므로
     *   → 먼저 "이 계정이 사용하던 세트 id"를 수집하고,
     *   → 이 계정의 세션들을 모두 지운 뒤,
     *   → 남은 세션이 아무도 참조하지 않는 '고아 세트'만 안전하게 정리(세트/문항/보기)한다.
     *
     * 삭제 순서:
     *   1) user_wrong_note (계정 기준으로 바로 삭제)
     *   2) session_answer (해당 계정의 user_quiz_session을 JOIN해서 삭제)
     *   3) user_quiz_session (계정 기준 삭제)
     *   4) '고아 세트' 정리:
     *      - 이 계정의 세션들이 참조하던 quiz_set 들 중
     *        더 이상 어떤 세션도 참조하지 않는 id만 골라
     *        quiz_choice → quiz_question → quiz_set 순서로 삭제
     */
    @Transactional
    public Result eraseByAccountId(Long accountId) {
        // (A) 이 계정의 세션들이 '참조하던' 세트 id 목록을 먼저 확보해 둔다.
        //     → 나중에 고아 세트 판단에 사용
        List<Long> candidateSetIds = listLongs("""
            SELECT DISTINCT quiz_set_id
              FROM user_quiz_session
             WHERE account_id = :id
        """, accountId);

        // (B) user_wrong_note : 계정 기준 바로 삭제
        int delWrong = execute("DELETE FROM user_wrong_note WHERE account_id = :id", accountId);

        // (C) session_answer : 이 계정의 세션을 통해 매핑되는 답안을 먼저 지움
        int delSa = execute("""
            DELETE a
              FROM session_answer a
              JOIN user_quiz_session s ON s.id = a.session_id
             WHERE s.account_id = :id
        """, accountId);

        // (D) user_quiz_session : 계정 기준 세션 삭제
        int delSessions = execute("DELETE FROM user_quiz_session WHERE account_id = :id", accountId);

        // (E) 고아 세트 정리
        //     - candidateSetIds 중에서 "아무 세션도 참조하지 않는" 세트만 추린다.
        List<Long> orphanSetIds = (candidateSetIds.isEmpty())
                ? List.of()
                : listLongs(buildIn("""
                    SELECT qs.id
                      FROM quiz_set qs
                     WHERE qs.id IN (%s)
                       AND NOT EXISTS (
                             SELECT 1
                               FROM user_quiz_session s
                              WHERE s.quiz_set_id = qs.id
                           )
                """, candidateSetIds.size()), candidateSetIds);

        long delChoices = 0;
        long delQuestions = 0;
        long delSets = 0;

        if (!orphanSetIds.isEmpty()) {
            // 순서 중요: choice → question → set
            delChoices = executeIn("""
                DELETE c
                  FROM quiz_choice c
                  JOIN quiz_question q ON q.id = c.quiz_question_id
                 WHERE q.quiz_set_id IN (%s)
            """, orphanSetIds);

            delQuestions = executeIn("""
                DELETE FROM quiz_question
                 WHERE quiz_set_id IN (%s)
            """, orphanSetIds);

            delSets = executeIn("""
                DELETE FROM quiz_set
                 WHERE id IN (%s)
            """, orphanSetIds);
        }

        log.info("[quiz:erase] accountId={} delWrong={}, delSA={}, delSessions={}, orphan: sets={}, questions={}, choices={}",
                accountId, delWrong, delSa, delSessions, delSets, delQuestions, delChoices);

        return new Result(delWrong, delSa, delSessions, delChoices, delQuestions, delSets);
    }

    /* ===================== 내부 유틸 메서드 (Native Query 헬퍼) ===================== */

    // 단일 파라미터(:id)로 실행하는 DELETE/UPDATE
    private int execute(String sql, Long accountId) {
        return em.createNativeQuery(sql)
                .setParameter("id", accountId)
                .executeUpdate();
    }

    // IN 절이 있는 DELETE/UPDATE (ids를 바인딩)
    private int executeIn(String sqlWithPlaceholders, List<Long> ids) {
        String sql = buildIn(sqlWithPlaceholders, ids.size());
        var q = em.createNativeQuery(sql);
        for (int i = 0; i < ids.size(); i++) {
            q.setParameter(i + 1, ids.get(i)); // ?1, ?2, ...
        }
        return q.executeUpdate();
    }

    // SELECT ... WHERE id IN (?,?,...) 형태로 결과를 Long 리스트로 받기
    private List<Long> listLongs(String sqlWithNamed, Long accountId) {
        @SuppressWarnings("unchecked")
        List<Object> rows = em.createNativeQuery(sqlWithNamed)
                .setParameter("id", accountId)
                .getResultList();
        return mapToLongs(rows);
    }

    // SELECT ... WHERE id IN (?,?,...) 형태 - 파라미터 배열 기반
    private List<Long> listLongs(String sqlWithPlaceholders, List<Long> ids) {
        String sql = buildIn(sqlWithPlaceholders, ids.size());
        var q = em.createNativeQuery(sql);
        for (int i = 0; i < ids.size(); i++) q.setParameter(i + 1, ids.get(i));
        @SuppressWarnings("unchecked")
        List<Object> rows = q.getResultList();
        return mapToLongs(rows);
    }

    // (?, ?, ?, ...) 자리를 size 기준으로 만들어주는 헬퍼
    private String buildIn(String template, int size) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            if (i > 0) sb.append(',');
            sb.append('?').append(i + 1);
        }
        return template.formatted(sb);
    }

    private List<Long> mapToLongs(List<Object> rows) {
        List<Long> out = new ArrayList<>(rows.size());
        for (Object r : rows) {
            if (r == null) continue;
            if (r instanceof Number n) out.add(n.longValue());
            else if (r instanceof BigInteger bi) out.add(bi.longValue());
            else out.add(Long.valueOf(r.toString()));
        }
        return out;
    }
}
