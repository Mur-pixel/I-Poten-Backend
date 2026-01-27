package com.cygnus.ipoten.quiz_session.controller;

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
import com.cygnus.ipoten.redis_cache.RedisCacheService;
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

    private final RedisCacheService redisCacheService;
    private final QuizSessionQueryService quizSessionQueryService;
    private final QuizScopeService quizScopeService;
    private final QuizSessionRetryService quizSessionRetryService;
    private final QuizSessionDeleteService quizSessionDeleteService;
    private final QuizSessionRenameService quizSessionRenameService;

    @Operation(
            summary = "퀴즈 세션 통합 시작 엔드포인트",
            description = "source 값(wordbook/term_category/set/job)에 따라 단어장/카테고리/세트/직무 기반으로 퀴즈 세션을 시작합니다."
    )
    @PostMapping("/me/quiz/sessions/start")
    public ResponseEntity<?> startQuizUnified(
            @Valid @RequestBody StartQuizSessionUnifiedRequestForm requestForm,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            String customTitle = requestForm.getCustomTitle();
            log.info("[unified] req source={}, setId={}, count={}, customTitle={}",
                    requestForm.getSource(), requestForm.getQuizSetId(), requestForm.getCount(), requestForm.getCustomTitle());

            ScopeCondition condition = requestForm.toScopeCondition(accountId);
            log.info("[unified] source={}, setId={}, count={}, type={}, level={}, seedMode={}, fixedSeed={}",
                    requestForm.getSource(), requestForm.getQuizSetId(), requestForm.getCount(),
                    requestForm.getType(), requestForm.getLevel(),
                    requestForm.getSeedMode(), requestForm.getFixedSeed());

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

    @Operation(
            summary = "퀴즈 세트 재도전",
            description = "기존 퀴즈 세션을 다시 풀되, 문항과 보기 순서는 새로 셔플됩니다."
    )
    @PostMapping("/me/quiz/sessions/{sessionId}/retry")
    public ResponseEntity<CreateQuizSessionResponseForm> retryAll(
            @PathVariable Long sessionId,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("인증 실패: 계정 식별 불가");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        StartQuizSessionResponse started = quizSessionRetryService.startRetryAll(sessionId, accountId);
        return ResponseEntity.status(HttpStatus.CREATED).body(CreateQuizSessionResponseForm.from(started));
    }

    @Operation(
            summary = "오답만 다시 풀기",
            description = "기존 세션의 오답만 모아서 새로운 오답 전용 세션을 생성합니다."
    )
    @PostMapping("/me/quiz/sessions/{sessionId}/retry-wrong")
    public ResponseEntity<CreateQuizSessionResponseForm> retryWrongOnly(
            @Parameter(description = "기준이 될 기존 세션 ID", example = "1")
            @PathVariable Long sessionId,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("인증 실패: 계정 식별 불가");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            StartQuizSessionResponse started = quizSessionRetryService.startRetryWrongOnly(sessionId, accountId);
            return ResponseEntity.status(HttpStatus.CREATED).body(CreateQuizSessionResponseForm.from(started));
        } catch(SecurityException e) {
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

    @Operation(
            summary = "내 퀴즈 세션 목록 조회",
            description = "가장 최근에 진행한 퀴즈 세션들을 상태(IN_PROGRESS/SUBMITTED) 필터와 함께 조회합니다."
    )
    @GetMapping("/me/quiz/sessions")
    public ResponseEntity<SessionListResponseForm> listMySessions(
            @RequestParam(name = "limit", defaultValue = "20") int limit,
            @RequestParam(name = "status", required = false) String status, // SUBMITTED/IN_PROGRESS/null
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (limit <= 0 || limit > 100) limit = 20;
        var list = quizSessionQueryService.listMySessions(accountId, limit, status);
        return ResponseEntity.ok(list);
    }

    @Operation(
            summary = "특정 퀴즈 세션 요약 조회",
            description = "정답/오답 개수, 점수 등 해당 세션의 요약 정보를 조회합니다."
    )
    @GetMapping("/me/quiz/sessions/{sessionId}")
    public ResponseEntity<SessionSummaryResponseForm> getSessionSummary(
            @Parameter(description = "조회할 세션 ID", example = "1")
            @PathVariable Long sessionId,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("인증 실패: 계정 식별 불가");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            var summary = quizSessionQueryService.getSummary(sessionId, accountId);
            return ResponseEntity.ok(summary);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @Operation(
            summary = "퀴즈 세션 진행 화면용 문항 목록 조회",
            description = "특정 세션에 포함된 문항 목록을 페이지네이션(offset/limit) 형태로 조회합니다. 필요 시 정답 포함 여부를 제어할 수 있습니다."
    )
    @GetMapping("/me/quiz/sessions/{sessionId}/items")
    public ResponseEntity<SessionItemsPageResponseForm> getSessionItems(
            @Parameter(description = "세션 ID", example = "1")
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(name = "includeAnswers", defaultValue = "false") boolean includeAnswers,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("인증 실패: 계정 식별 불가");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        limit = Math.max(1, Math.min(100, limit));

        var page = quizSessionQueryService.getSessionItems(sessionId, accountId, offset, limit, includeAnswers);
        return ResponseEntity.ok(page);
    }

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

    @Operation(
            summary = "오늘의 초성퀴즈 문항 조회",
            description = "DAILY/INITIALS 타입 세션에 대해, 세션 스냅샷 기준 3개의 초성 퀴즈 문항을 조회합니다."
    )
    @GetMapping("/me/quiz/sessions/{sessionId}/questions/initials")
    public ResponseEntity<?> getInitialQuestions(
            @Parameter(description = "오늘의 초성퀴즈 세션 ID", example = "1")
            @PathVariable Long sessionId,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        InitialsQuestionsResponse body = quizSessionQueryService.getDailyInitialsQuestions(sessionId, accountId);
        return ResponseEntity.ok(body);
    }

    @Operation(
            summary = "최근 N일 이내 오답만 다시 풀기",
            description = "퀴즈 타임라인 페이지에서 최근 7일 혹은 30일 이내 오답만 빠르게 다시 푸는 경우 선택합니다. "
    )
    @PostMapping("/me/quiz/sessions/quick-retry")
    public ResponseEntity<?> quickRetry(
            @RequestParam(name = "days", required = false) Integer days,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            StartQuizSessionResponse started =
                    (days == null)
                            ? quizSessionRetryService.startQuickRetry(accountId) // 기존 AUTO(7->30 폴백) 유지
                            : quizSessionRetryService.startQuickRetry(accountId, days); // 사용자가 7일 혹은 30일을 선택한 경우
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

    @Operation(
            summary = "내 퀴즈 세션 삭제",
            description = "사용자가 본인 퀴즈 세션을 삭제(소프트 삭제)하여 타임라인/목록에서 보이지 않게 합니다. 오답노트는 삭제하지 않습니다."
    )
    @DeleteMapping("/me/quiz/sessions/{sessionId}")
    public ResponseEntity<?> deleteMySession(
            @PathVariable Long sessionId,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

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

    @Operation(
            summary = "퀴즈 세션 이름(제목) 수정",
            description = "사용자가 본인 퀴즈 세션의 제목을 수정합니다."
    )
    @PatchMapping("/me/quiz/sessions/{sessionId}/title")
    public ResponseEntity<?> renameSessionTitle(
            @PathVariable Long sessionId,
            @Valid @RequestBody RenameQuizSessionTitleRequestForm requestForm,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if  (accountId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

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