package com.cygnus.ipoten.quiz_analytics.controller;

import com.cygnus.ipoten.quiz_set.entity.enums.QuizSetType;
import com.cygnus.ipoten.quiz_analytics.controller.response_form.QuizTrendResponseForm;
import com.cygnus.ipoten.quiz_analytics.controller.response_form.QuizTotalSetsResponseForm;
import com.cygnus.ipoten.quiz_analytics.service.QuizAnalyticsQueryService;
import com.cygnus.ipoten.quiz_session.service.QuizSessionQueryService;
import com.cygnus.ipoten.redis_cache.RedisCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "QuizAnalytics", description = "퀴즈 결과 분석 관리 API")
public class QuizAnalyticsController {

    private final RedisCacheService redisCacheService;
    private final QuizAnalyticsQueryService quizAnalyticsQueryService;
    private final QuizSessionQueryService quizSessionQueryService;

    @Operation(
            summary = "퀴즈 성과 추이(그래프용 데이터) 조회",
            description = "정답률, 세트 수 등 지정한 metric에 대해 지정된 기간(span, 예: 30d/7d)에 따른 추이 데이터를 조회합니다."
    )
    @GetMapping("/me/quiz/metrics")
    public ResponseEntity<QuizTrendResponseForm> getTrend(
            @RequestParam String metric,
            @RequestParam(defaultValue = "30d") String span,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(quizAnalyticsQueryService.getTrend(accountId, metric, span));
    }

    @Operation(
            summary = "내 퀴즈 활동 타임라인 조회",
            description = "검색어(q), 파트 타입, 페이지 정보로 필터링하여 사용자의 퀴즈 세션 타임라인을 조회합니다."
    )
    @GetMapping("/me/quiz/timeline")
    public ResponseEntity<?> getMyTimeline(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "type", required = false, defaultValue = "ALL") String type,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        QuizSetType part = null;
        if (type != null && !type.isBlank() && !"ALL".equalsIgnoreCase(type)) {
            try {
                part = QuizSetType.valueOf(type.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }
        var result = quizSessionQueryService.getTimeline(accountId, q, part, page, size);
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "완료한 퀴즈 세트 총 개수 조회",
            description = "사용자가 지금까지 완료(제출)한 퀴즈 세트의 총 개수를 조회합니다."
    )
    @GetMapping("/me/quiz/metrics/total-sets")
    public ResponseEntity<QuizTotalSetsResponseForm> getTotalSets(
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        long total = quizAnalyticsQueryService.getTotalSets(accountId);
        return ResponseEntity.ok(new QuizTotalSetsResponseForm(total));
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
