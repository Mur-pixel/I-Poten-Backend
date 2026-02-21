package com.cygnus.ipoten.quiz_daily.controller;

import com.cygnus.ipoten.quiz_daily.controller.request_form.CheckDailyQuestionRequestForm;
import com.cygnus.ipoten.quiz_daily.controller.response_form.CheckDailyQuestionResponseForm;
import com.cygnus.ipoten.quiz_daily.service.DailyQuizPlayService;
import com.cygnus.ipoten.redis_cache.RedisCacheService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class DailyQuizPlayController {

    private final RedisCacheService redisCacheService;
    private final DailyQuizPlayService dailyQuizPlayService;

    @Operation(
            summary = "데일리 문항 즉시 채점",
            description = "데일리 퀴즈 진행 중 문항 단위로 정오, 정답, 해설, 다음 문항 ID를 반환합니다."
    )
    @PostMapping("/me/quiz/daily/sessions/{sessionId}/questions/{questionId}/check")
    public ResponseEntity<?> checkQuestion(
            @PathVariable Long sessionId,
            @PathVariable Long questionId,
            @Valid @RequestBody CheckDailyQuestionRequestForm requestForm,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            CheckDailyQuestionResponseForm responseForm =
                    dailyQuizPlayService.checkQuestion(sessionId, questionId, accountId, requestForm);
            return ResponseEntity.ok(responseForm);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            Throwable root = e;
            while (root.getCause() != null) root = root.getCause();

            log.error("[daily check] sessionId={} questionId={} payload={} rootType={} rootMsg={}",
                    sessionId, questionId, requestForm,
                    root.getClass().getName(),
                    root.getMessage(),
                    e
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", "서버 내부 오류가 발생했습니다.",
                            "debug", root.getClass().getSimpleName() + ": " + String.valueOf(root.getMessage())
                    ));
        }
    }

    private Long resolveAccountId(String userToken) {
        if (userToken == null || userToken.isBlank()) return null;
        return redisCacheService.getValueByKey(userToken, Long.class);
    }
}
