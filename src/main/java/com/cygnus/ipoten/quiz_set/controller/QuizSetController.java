package com.cygnus.ipoten.quiz_set.controller;

import com.cygnus.ipoten.quiz_question.entity.enums.DifficultyLevel;
import com.cygnus.ipoten.quiz_set.controller.response_form.ResolveQuizSetResponseForm;
import com.cygnus.ipoten.quiz_set.entity.enums.QuizSetType;
import com.cygnus.ipoten.quiz_set.service.QuizSetQueryService;
import com.cygnus.ipoten.quiz_set.service.response.ResolveQuizSetResult;
import com.cygnus.ipoten.redis_cache.RedisCacheService;
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

    private final RedisCacheService redisCacheService;
    private final QuizSetQueryService quizSetQueryService;

    @Operation(
            summary = "Quiz Set ID resolve",
            description = "termCategoryId/type/level/count 조건에 맞는 기존 퀴즈 세트를 찾아 반환합니다."
    )
    @GetMapping("/me/quiz/sets/resolve")
    public ResponseEntity<ResolveQuizSetResponseForm> resolve(
            @RequestParam Long termCategoryId,
            @RequestParam String type,
            @RequestParam(required=false) String level,
            @RequestParam(required=false) Integer count,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

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

    @Operation(
            summary = "퀴즈 세트에 포함된 문항 조회",
            description = "세트 ID와 파트 타입(part)에 따라 객관식/OX/초성 문항 목록을 조회합니다."
    )
    @GetMapping("/quiz/sets/{setId}/questions")
    public ResponseEntity<?> getQuestionsBySet(
            @Parameter(description = "조회할 퀴즈 세트 ID", example = "1")
            @PathVariable Long setId,
            @Parameter(description = "세트 파트 타입 (CHOICE/OX/INITIALS)", required = false)
            @RequestParam(name = "part", required = false) QuizSetType part
    ) {
        // 세트의 실제 타입
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
                // 엔티티 전부 조회 (orderIndex -> id 기준 정렬)
                var qs = quizSetQueryService.findInitialsQuestionsBySetId(setId);

                var out = new java.util.ArrayList<java.util.Map<String,Object>>();
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

    /** 정책: 소유권 위반/존재하지 않음은 404로 숨김 */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Void> handleSecurityException(SecurityException ex) {
        log.warn("보안/소유권 오류 → 404 변환: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    /** 만료 등 상태 충돌은 409 */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Void> handleIllegalState(IllegalStateException ex) {
        log.warn("상태 충돌(409): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

}
