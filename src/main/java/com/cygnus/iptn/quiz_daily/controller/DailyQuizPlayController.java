package com.cygnus.iptn.quiz_daily.controller;

import com.cygnus.iptn.common.annotation.LoginUser;
import com.cygnus.iptn.quiz_daily.controller.request_form.CheckDailyQuestionRequestForm;
import com.cygnus.iptn.quiz_daily.controller.response_form.CheckDailyQuestionResponseForm;
import com.cygnus.iptn.quiz_daily.service.DailyQuizPlayService;
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

    private final DailyQuizPlayService dailyQuizPlayService;

    @Operation(summary = "데일리 문항 즉시 채점")
    @PostMapping("/me/quiz/daily/sessions/{sessionId}/questions/{questionId}/check")
    public ResponseEntity<?> checkQuestion(
            @PathVariable Long sessionId,
            @PathVariable Long questionId,
            @Valid @RequestBody CheckDailyQuestionRequestForm requestForm,
            @LoginUser Long accountId) {

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
                    e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", "서버 내부 오류가 발생했습니다.",
                            "debug", root.getClass().getSimpleName() + ": " + String.valueOf(root.getMessage())
                    ));
        }
    }
}
