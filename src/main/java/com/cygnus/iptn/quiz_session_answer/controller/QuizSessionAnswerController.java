package com.cygnus.iptn.quiz_session_answer.controller;

import com.cygnus.iptn.common.annotation.LoginUser;
import com.cygnus.iptn.quiz_session_answer.controller.request_form.SubmitQuizSessionRequestForm;
import com.cygnus.iptn.quiz_session_answer.service.QuizSessionAnswerService;
import com.cygnus.iptn.quiz_session.service.QuizSessionQueryService;
import com.cygnus.iptn.redis_cache.RedisCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class QuizSessionAnswerController {

    private final QuizSessionAnswerService quizSessionAnswerService;
    private final QuizSessionQueryService quizSessionQueryService;

    @Operation(summary = "퀴즈 세션 제출")
    @PostMapping("/me/quiz/sessions/{sessionId}/submit")
    public ResponseEntity<?> submitQuizSession(
            @Parameter(description = "제출할 세션 ID", example = "1")
            @PathVariable Long sessionId,
            @Valid @RequestBody SubmitQuizSessionRequestForm requestForm,
            @LoginUser Long accountId) {

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
}
