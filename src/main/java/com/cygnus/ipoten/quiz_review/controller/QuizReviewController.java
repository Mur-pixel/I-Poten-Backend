package com.cygnus.ipoten.quiz_review.controller;

import com.cygnus.ipoten.quiz_session.controller.response_form.CreateQuizSessionResponseForm;
import com.cygnus.ipoten.quiz_session.controller.response_form.SessionReviewResponseForm;
import com.cygnus.ipoten.quiz_session.service.QuizSessionQueryService;
import com.cygnus.ipoten.quiz_session.service.QuizSessionRetryService;
import com.cygnus.ipoten.quiz_session.service.response.StartQuizSessionResponse;
import com.cygnus.ipoten.redis_cache.RedisCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class QuizReviewController {

    private final RedisCacheService redisCacheService;
    private final QuizSessionQueryService quizSessionQueryService;

    @Operation(
            summary = "퀴즈 세션 리뷰용 조회",
            description = "각 문항별 정답, 해설, 사용자의 선택 내역을 포함한 세션 리뷰 정보를 조회합니다."
    )
    @GetMapping("/me/quiz/sessions/{sessionId}/review")
    public ResponseEntity<SessionReviewResponseForm> getSessionReview(
            @Parameter(description = "리뷰를 조회할 세션 ID", example = "1")
            @PathVariable Long sessionId,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var review = quizSessionQueryService.getReview(sessionId, accountId);
        return ResponseEntity.ok(review);
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
