package com.cygnus.ipoten.quiz_session_answer.controller;

import com.cygnus.ipoten.quiz_session_answer.controller.request_form.SubmitQuizSessionRequestForm;
import com.cygnus.ipoten.quiz_session_answer.service.QuizSessionAnswerService;
import com.cygnus.ipoten.quiz_session.service.QuizSessionQueryService;
import com.cygnus.ipoten.redis_cache.RedisCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


/**
 * QuizSessionAnswerController
 *  - 사용자의 "답안 제출", "채점", "오답만 다시 풀기" 요청 처리
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class QuizSessionAnswerController {

    private final RedisCacheService redisCacheService;
    private final QuizSessionAnswerService quizSessionAnswerService;
    private final QuizSessionQueryService quizSessionQueryService;

    @Operation(
            summary = "퀴즈 세션 제출",
            description = "사용자가 푼 퀴즈 세션을 제출하고 점수 및 통계를 계산합니다. 이미 제출된 세션이면 요약 정보를 반환합니다."
    )
    @PostMapping("/me/quiz/sessions/{sessionId}/submit")
    public ResponseEntity<?> submitQuizSession(
            @Parameter(description = "제출할 세션 ID", example = "1")
            @PathVariable Long sessionId,
            @Valid @RequestBody SubmitQuizSessionRequestForm requestForm,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("인증 실패: 계정 식별 불가");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            var response = quizSessionAnswerService.submitSession(sessionId, accountId, requestForm);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            log.warn("세션 접근 거부", e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            String message = String.valueOf(e.getMessage());
            if (message.contains("이미 제출된 세션")) {
                var summary = quizSessionQueryService.getSummary(sessionId, accountId);
                return ResponseEntity.ok(summary);
            }
            throw e;
        } catch (Exception e) {
            log.error("세션 제출 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 공통: 쿠키에서 userToken을 읽어 Redis에서 accountId를 조회한다.
     * - 토큰이 없거나 공백이면 null
     * - Redis에 존재하지 않거나 TTL 만료된 경우도 null
     */
    private Long resolveAccountId(String userToken) {
        if (userToken == null || userToken.isBlank()) {
            return null;
        }
        return redisCacheService.getValueByKey(userToken, Long.class); // TTL 만료/무효면 null
    }
}
