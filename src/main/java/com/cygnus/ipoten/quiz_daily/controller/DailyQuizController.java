package com.cygnus.ipoten.quiz_daily.controller;

import com.cygnus.ipoten.common.annotation.LoginUser;
import com.cygnus.ipoten.quiz_daily.controller.response_form.DailyQuizStartResponseForm;
import com.cygnus.ipoten.quiz_daily.entity.enums.DailyStartMode;
import com.cygnus.ipoten.quiz_daily.service.DailyQuizService;
import com.cygnus.ipoten.quiz_question.entity.enums.QuestionType;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/me/quiz/daily")
public class DailyQuizController {

    private final DailyQuizService dailyQuizService;

    @Operation(summary = "오늘의 퀴즈 시작")
    @PostMapping("/general/start")
    public ResponseEntity<?> startGeneralDaily(
            @RequestParam(name = "mode", required = false) DailyStartMode mode,
            @LoginUser Long accountId) {

        try {
            var started = dailyQuizService.startGeneralDaily(accountId, mode);

            boolean carryOver = !started.todayYmd().equals(started.activeYmd()) && started.hasUnfinishedSession();

            DailyQuizStartResponseForm body = new DailyQuizStartResponseForm(
                    started.todayYmd(),
                    started.activeYmd(),
                    carryOver,
                    "GENERAL",
                    "DAILY -> FIXED",
                    List.of(
                            new DailyQuizStartResponseForm.Item(
                                    QuestionType.CHOICE,
                                    started.choice().getSessionId(), 3, "[오늘의 퀴즈] 객관식 퀴즈 3문제",
                                    started.choice().getQuestionIds(),
                                    started.choice().getItems()
                            ),
                            new DailyQuizStartResponseForm.Item(
                                    QuestionType.OX,
                                    started.ox().getSessionId(), 3, "[오늘의 퀴즈] OX 퀴즈 3문제",
                                    started.ox().getQuestionIds(),
                                    started.ox().getItems()
                            ),
                            new DailyQuizStartResponseForm.Item(
                                    QuestionType.INITIALS,
                                    started.initials().getSessionId(), 3, "[오늘의 퀴즈] 초성 퀴즈 3문제",
                                    started.initials().getQuestionIds(),
                                    started.initials().getItems()
                            )
                    )
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(body);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }
}
