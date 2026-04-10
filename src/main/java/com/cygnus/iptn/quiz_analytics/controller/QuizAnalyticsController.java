package com.cygnus.iptn.quiz_analytics.controller;

import com.cygnus.iptn.common.annotation.LoginUser;
import com.cygnus.iptn.quiz_set.entity.enums.QuizSetType;
import com.cygnus.iptn.quiz_analytics.controller.response_form.QuizTrendResponseForm;
import com.cygnus.iptn.quiz_analytics.controller.response_form.QuizTotalSetsResponseForm;
import com.cygnus.iptn.quiz_analytics.service.QuizAnalyticsQueryService;
import com.cygnus.iptn.quiz_session.service.QuizSessionQueryService;
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

    private final QuizAnalyticsQueryService quizAnalyticsQueryService;
    private final QuizSessionQueryService quizSessionQueryService;

    @Operation(summary = "퀴즈 성과 추이(그래프용 데이터) 조회")
    @GetMapping("/me/quiz/metrics")
    public ResponseEntity<QuizTrendResponseForm> getTrend(
            @RequestParam String metric,
            @RequestParam(defaultValue = "30d") String span,
            @LoginUser Long accountId) {

        return ResponseEntity.ok(quizAnalyticsQueryService.getTrend(accountId, metric, span));
    }

    @Operation(summary = "내 퀴즈 활동 타임라인 조회")
    @GetMapping("/me/quiz/timeline")
    public ResponseEntity<?> getMyTimeline(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "type", required = false, defaultValue = "ALL") String type,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size,
            @LoginUser Long accountId) {

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

    @Operation(summary = "완료한 퀴즈 세트 총 개수 조회")
    @GetMapping("/me/quiz/metrics/total-sets")
    public ResponseEntity<QuizTotalSetsResponseForm> getTotalSets(@LoginUser Long accountId) {
        long total = quizAnalyticsQueryService.getTotalSets(accountId);
        return ResponseEntity.ok(new QuizTotalSetsResponseForm(total));
    }
}
