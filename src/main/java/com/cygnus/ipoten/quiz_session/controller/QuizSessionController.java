package com.cygnus.ipoten.quiz_session.controller;

import com.cygnus.ipoten.common.annotation.LoginUser;
import com.cygnus.ipoten.quiz_session.controller.request_form.RenameQuizSessionTitleRequestForm;
import com.cygnus.ipoten.quiz_session.controller.response_form.CreateQuizSessionResponseForm;
import com.cygnus.ipoten.quiz_session.service.QuizSessionDeleteService;
import com.cygnus.ipoten.quiz_session.service.QuizSessionRenameService;
import com.cygnus.ipoten.quiz_session.service.QuizSessionRetryService;
import com.cygnus.ipoten.quiz_session.service.response.InitialsQuestionsResponse;
import com.cygnus.ipoten.quiz_session_scope.controller.request_form.StartQuizSessionUnifiedRequestForm;
import com.cygnus.ipoten.quiz_session_scope.service.QuizScopeService;
import com.cygnus.ipoten.quiz_session_scope.value_objects.ScopeCondition;
import com.cygnus.ipoten.quiz_session.controller.response_form.*;
import com.cygnus.ipoten.quiz_session.service.QuizSessionQueryService;
import com.cygnus.ipoten.quiz_session.service.response.StartQuizSessionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class QuizSessionController {

    private final QuizSessionQueryService quizSessionQueryService;
    private final QuizScopeService quizScopeService;
    private final QuizSessionRetryService quizSessionRetryService;
    private final QuizSessionDeleteService quizSessionDeleteService;
    private final QuizSessionRenameService quizSessionRenameService;

    @Operation(summary = "퀴즈 세션 통합 시작 엔드포인트")
    @PostMapping("/me/quiz/sessions/start")
    public ResponseEntity<?> startQuizUnified(
            @Valid @RequestBody StartQuizSessionUnifiedRequestForm requestForm,
            @LoginUser Long accountId) {

        try {
            String customTitle = requestForm.getCustomTitle();
            log.info("[unified] req source={}, setId={}, count={}, customTitle={}",
                    requestForm.getSource(), requestForm.getQuizSetId(), requestForm.getCount(), requestForm.getCustomTitle());

            ScopeCondition condition = requestForm.toScopeCondition(accountId);
            StartQuizSessionResponse started = quizScopeService.startScopedSession(accountId, condition, customTitle);
            return ResponseEntity.status(HttpStatus.CREATED).body(CreateQuizSessionResponseForm.from(started));

        } catch (IllegalArgumentException e) {
            log.warn("startQuizUnified bad request: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("startQuizUnified failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "퀴즈 세트 재도전")
    @PostMapping("/me/quiz/sessions/{sessionId}/retry")
    public ResponseEntity<CreateQuizSessionResponseForm> retryAll(
            @PathVariable Long sessionId,
            @LoginUser Long accountId) {

        StartQuizSessionResponse started = quizSessionRetryService.startRetryAll(sessionId, accountId);
        return ResponseEntity.status(HttpStatus.CREATED).body(CreateQuizSessionResponseForm.from(started));
    }

    @Operation(summary = "오답만 다시 풀기")
    @PostMapping("/me/quiz/sessions/{sessionId}/retry-wrong")
    public ResponseEntity<CreateQuizSessionResponseForm> retryWrongOnly(
            @Parameter(description = "기준이 될 기존 세션 ID", example = "1")
            @PathVariable Long sessionId,
            @LoginUser Long accountId) {

        try {
            StartQuizSessionResponse started = quizSessionRetryService.startRetryWrongOnly(sessionId, accountId);
            return ResponseEntity.status(HttpStatus.CREATED).body(CreateQuizSessionResponseForm.from(started));
        } catch (SecurityException e) {
            log.warn("세션 접근 거부", e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("오답세션 생성 유효성 오류", e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("오답세션 생성 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "내 퀴즈 세션 목록 조회")
    @GetMapping("/me/quiz/sessions")
    public ResponseEntity<SessionListResponseForm> listMySessions(
            @RequestParam(name = "limit", defaultValue = "20") int limit,
            @RequestParam(name = "status", required = false) String status,
            @LoginUser Long accountId) {

        if (limit <= 0 || limit > 100) limit = 20;
        var list = quizSessionQueryService.listMySessions(accountId, limit, status);
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "특정 퀴즈 세션 요약 조회")
    @GetMapping("/me/quiz/sessions/{sessionId}")
    public ResponseEntity<SessionSummaryResponseForm> getSessionSummary(
            @Parameter(description = "조회할 세션 ID", example = "1")
            @PathVariable Long sessionId,
            @LoginUser Long accountId) {

        try {
            var summary = quizSessionQueryService.getSummary(sessionId, accountId);
            return ResponseEntity.ok(summary);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @Operation(summary = "퀴즈 세션 진행 화면용 문항 목록 조회")
    @GetMapping("/me/quiz/sessions/{sessionId}/items")
    public ResponseEntity<SessionItemsPageResponseForm> getSessionItems(
            @Parameter(description = "세션 ID", example = "1")
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(name = "includeAnswers", defaultValue = "false") boolean includeAnswers,
            @LoginUser Long accountId) {

        limit = Math.max(1, Math.min(100, limit));
        var page = quizSessionQueryService.getSessionItems(sessionId, accountId, offset, limit, includeAnswers);
        return ResponseEntity.ok(page);
    }

    @Operation(summary = "퀴즈 세션 리뷰용 조회")
    @GetMapping("/me/quiz/sessions/{sessionId}/review")
    public ResponseEntity<SessionReviewResponseForm> getSessionReview(
            @Parameter(description = "리뷰를 조회할 세션 ID", example = "1")
            @PathVariable Long sessionId,
            @LoginUser Long accountId) {

        var review = quizSessionQueryService.getReview(sessionId, accountId);
        return ResponseEntity.ok(review);
    }

    @Operation(summary = "오늘의 초성퀴즈 문항 조회")
    @GetMapping("/me/quiz/sessions/{sessionId}/questions/initials")
    public ResponseEntity<?> getInitialQuestions(
            @Parameter(description = "오늘의 초성퀴즈 세션 ID", example = "1")
            @PathVariable Long sessionId,
            @LoginUser Long accountId) {

        InitialsQuestionsResponse body = quizSessionQueryService.getDailyInitialsQuestions(sessionId, accountId);
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "최근 N일 이내 오답만 다시 풀기")
    @PostMapping("/me/quiz/sessions/quick-retry")
    public ResponseEntity<?> quickRetry(
            @RequestParam(name = "days", required = false) Integer days,
            @LoginUser Long accountId) {

        try {
            StartQuizSessionResponse started =
                    (days == null)
                            ? quizSessionRetryService.startQuickRetry(accountId)
                            : quizSessionRetryService.startQuickRetry(accountId, days);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("sessionId", started.getSessionId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "code", "QUICK_RETRY_NOT_AVAILABLE",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("quickRetry failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "내 퀴즈 세션 삭제")
    @DeleteMapping("/me/quiz/sessions/{sessionId}")
    public ResponseEntity<?> deleteMySession(
            @PathVariable Long sessionId,
            @LoginUser Long accountId) {

        try {
            quizSessionDeleteService.deleteMySession(accountId, sessionId);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("deleteMySession failed sessionId={}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "퀴즈 세션 이름(제목) 수정")
    @PatchMapping("/me/quiz/sessions/{sessionId}/title")
    public ResponseEntity<?> renameSessionTitle(
            @PathVariable Long sessionId,
            @Valid @RequestBody RenameQuizSessionTitleRequestForm requestForm,
            @LoginUser Long accountId) {

        try {
            var updated = quizSessionRenameService.renameTitle(sessionId, accountId, requestForm.getTitle());
            return ResponseEntity.ok(RenameQuizSessionTitleResponseForm.from(updated));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("renameSessionTitle failed sessionId={}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
