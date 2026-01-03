package com.cygnus.ipoten.quiz_wrongnote.controller;

import com.cygnus.ipoten.quiz_session.controller.response_form.CreateQuizSessionResponseForm;
import com.cygnus.ipoten.quiz_session.service.QuizSessionRetryService;
import com.cygnus.ipoten.quiz_session.service.response.StartQuizSessionResponse;
import com.cygnus.ipoten.quiz_wrongnote.controller.request_form.WrongNoteResolvedUpdateRequestForm;
import com.cygnus.ipoten.quiz_wrongnote.service.QuizWrongNoteService;
import com.cygnus.ipoten.quiz_wrongnote.service.QuizWrongNoteServiceImpl;
import com.cygnus.ipoten.redis_cache.RedisCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.NoSuchElementException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class QuizWrongNoteController {

    private final RedisCacheService redisCacheService;
    private final QuizSessionRetryService quizSessionRetryService;
    private final QuizWrongNoteServiceImpl quizWrongNoteServiceImpl;
    private final QuizWrongNoteService quizWrongNoteService;

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

    @GetMapping("/me/quiz/reviews/wrong")
    public ResponseEntity<?> listWrongNotes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "true") boolean includeAnswers,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("인증 실패: 계정 식별 불가");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            var body = quizWrongNoteService.listWrongNotes(accountId, page, size, type, sessionId, from, to, includeAnswers);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.error("listWrongNotes failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/me/quiz/reviews/{wrongNoteId}")
    public ResponseEntity<?> updateWrongNoteResolved(
            @PathVariable Long wrongNoteId,
            @RequestBody WrongNoteResolvedUpdateRequestForm requestForm,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (requestForm == null || requestForm.getResolved() == null) {
            return ResponseEntity.badRequest().body("resolved 값이 필요합니다.");
        }

        try {
            quizWrongNoteService.updateResolved(accountId, wrongNoteId, requestForm.getResolved());
            return ResponseEntity.ok().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("updateWrongNoteResolved failed", e);
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
