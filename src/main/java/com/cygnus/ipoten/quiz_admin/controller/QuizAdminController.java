package com.cygnus.ipoten.quiz_admin.controller;

import com.cygnus.ipoten.quiz_admin.service.QuizAdminService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class QuizAdminController {

    private final QuizAdminService quizAdminService;

    /**
     * 내부(Admin) 호출용: 특정 계정과 연관된 quiz 도메인 데이터 정리.
     * - user_quiz_session, session_answer, user_wrong_note 삭제
     * - 더 이상 어떤 세션에서도 참조되지 않는 quiz_set / quiz_question / quiz_choice 도 함께 정리
     *
     * 주의: quiz_set에는 accountId 컬럼이 없으므로, 실제로 다른 계정이 공유해서 쓰고 있을 수 있다.
     *      따라서 "모든 세션에서 고아(orphan)가 된 세트"만 삭제 대상이다.
     */
    @Operation(
            summary = "[내부] 특정 계정의 퀴즈 데이터 일괄 삭제",
            description = "Admin/배치용. 해당 accountId와 연관된 퀴즈 세션/오답노트 및 고아 세트/문항/보기를 정리합니다."
    )
    @DeleteMapping("/internal/admin/accounts/{accountId}/quiz:erase")
    public ResponseEntity<?> eraseQuizByAccount(
            @Parameter(description = "정리 대상 계정 ID", example = "1")
            @PathVariable Long accountId) {
        var result = quizAdminService.eraseByAccountId(accountId);

        Map<String, Object> body = Map.of(
                "accountId", accountId,
                "deleted", Map.of(
                        "wrong_note",          result.getWrongNotes(),
                        "session_answer",      result.getSessionAnswers(),
                        "user_quiz_session",   result.getSessions(),
                        "orphan_quiz_choice",  result.getOrphanChoices(),
                        "orphan_quiz_question",result.getOrphanQuestions(),
                        "orphan_quiz_set",     result.getOrphanSets()
                )
        );

        log.info("[quiz:erase] {}", body);
        return ResponseEntity.ok(body);
    }
}
