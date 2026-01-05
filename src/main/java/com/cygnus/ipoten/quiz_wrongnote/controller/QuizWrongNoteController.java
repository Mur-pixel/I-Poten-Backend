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
    private final QuizWrongNoteService quizWrongNoteService;

    @Operation(
            summary = "내 오답노트 목록 조회",
            description = "오답노트(리뷰) 중 오답 항목을 페이지네이션으로 조회합니다. " +
                    "type/sessionId/from~to 필터를 통해 범위를 좁힐 수 있고, includeAnswers=true면 정답/해설 등 민감 정보를 함께 내려줄 수 있습니다."
    )
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

    @Operation(
            summary = "오답노트 해결 여부 변경",
            description = "오답노트 항목을 '해결 완료(resolved=true)' 또는 '미해결(resolved=false)'로 변경합니다. "
    )
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
