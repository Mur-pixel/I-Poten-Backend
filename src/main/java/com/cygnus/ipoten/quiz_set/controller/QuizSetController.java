package com.cygnus.ipoten.quiz_set.controller;

import com.cygnus.ipoten.common.annotation.LoginUser;
import com.cygnus.ipoten.common.annotation.PublicEndpoint;
import com.cygnus.ipoten.quiz_question.entity.enums.DifficultyLevel;
import com.cygnus.ipoten.quiz_set.controller.response_form.ResolveQuizSetResponseForm;
import com.cygnus.ipoten.quiz_set.entity.enums.QuizSetType;
import com.cygnus.ipoten.quiz_set.service.QuizSetQueryService;
import com.cygnus.ipoten.quiz_set.service.response.ResolveQuizSetResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.NoSuchElementException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Quiz", description = "퀴즈 세트 관리 API")
public class QuizSetController {

    private final QuizSetQueryService quizSetQueryService;

    @Operation(summary = "Quiz Set ID resolve")
    @GetMapping("/me/quiz/sets/resolve")
    public ResponseEntity<ResolveQuizSetResponseForm> resolve(
            @RequestParam Long termCategoryId,
            @RequestParam String type,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) Integer count,
            @LoginUser Long accountId) {

        QuizSetType typeFilter = QuizSetType.fromParam(type);
        if (typeFilter == QuizSetType.MIX) typeFilter = null;

        DifficultyLevel levelFilter = DifficultyLevel.fromParam(level);
        if (levelFilter == DifficultyLevel.MIX) levelFilter = null;

        int c = (count == null) ? 10 : count;
        if (c < 5 || c > 20) throw new ResponseStatusException(BAD_REQUEST, "문항 수는 5~20개입니다.");

        try {
            ResolveQuizSetResult resolved = quizSetQueryService.resolve(termCategoryId, typeFilter, levelFilter, c);
            return ResponseEntity.ok(ResolveQuizSetResponseForm.from(resolved));
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "조건에 맞는 퀴즈 세트가 없습니다.");
        }
    }

    @PublicEndpoint
    @Operation(summary = "퀴즈 세트에 포함된 문항 조회")
    @GetMapping("/quiz/sets/{setId}/questions")
    public ResponseEntity<?> getQuestionsBySet(
            @Parameter(description = "조회할 퀴즈 세트 ID", example = "1")
            @PathVariable Long setId,
            @Parameter(description = "세트 파트 타입 (CHOICE/OX/INITIALS)")
            @RequestParam(name = "part", required = false) QuizSetType part) {

        var actual = quizSetQueryService.findPartTypeBySetId(setId).orElse(null);
        if (actual == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "quiz set not found");
        }
        if (part == null) part = actual;
        if (part != actual) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "part mismatch: requested=" + part + " actual=" + actual);
        }

        switch (part) {
            case CHOICE, OX -> {
                var items = quizSetQueryService.findChoiceQuestionsBySetId(setId);
                return ResponseEntity.ok(Map.of("total", items.size(), "questions", items));
            }
            case INITIALS -> {
                var qs = quizSetQueryService.findInitialsQuestionsBySetId(setId);
                var out = new java.util.ArrayList<java.util.Map<String, Object>>();
                int order = 1;
                for (var q : qs) {
                    out.add(java.util.Map.of(
                            "id", q.getId(),
                            "order", order++,
                            "questionText", java.util.Optional.ofNullable(q.getQuestionText()).orElse("")
                    ));
                }
                return ResponseEntity.ok(java.util.Map.of("total", out.size(), "questions", out));
            }
            default -> throw new ResponseStatusException(BAD_REQUEST, "unsupported part");
        }
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Void> handleSecurityException(SecurityException ex) {
        log.warn("보안/소유권 오류 → 404 변환: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Void> handleIllegalState(IllegalStateException ex) {
        log.warn("상태 충돌(409): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
}
