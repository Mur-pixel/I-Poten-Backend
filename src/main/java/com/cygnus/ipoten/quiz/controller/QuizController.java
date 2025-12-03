package com.cygnus.ipoten.quiz.controller;

import com.cygnus.ipoten.quiz.controller.request_form.*;
import com.cygnus.ipoten.quiz.controller.response_form.*;
import com.cygnus.ipoten.quiz.entity.enums.QuizSetType;
import com.cygnus.ipoten.quiz.service.*;
import com.cygnus.ipoten.quiz.service.request.CreateQuizSetByCategoryRequest;
import com.cygnus.ipoten.quiz.service.response.*;
import com.cygnus.ipoten.redis_cache.RedisCacheService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Quiz", description = "퀴즈 세트 관리 API")
public class QuizController {

    private final QuizSetService quizSetService;
    private final RedisCacheService redisCacheService;
    private final QuizSetQueryService quizSetQueryService;

    // 카테고리 기반 퀴즈 세트 자동 생성
    @Operation(
            summary = "카테고리 기반 퀴즈 세트 생성",
            description = "선택한 카테고리/난이도/문항 수를 기준으로 자동 퀴즈 세트를 구성합니다."
    )
    @PostMapping("/quiz-sets")
    public ResponseEntity<CreateQuizSetByCategoryResponseForm> createByCategory (
            @Valid @RequestBody CreateQuizSetByCategoryRequestForm requestForm,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("인증 실패: 계정 식별 불가");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.info("카테고리 기반 퀴즈 세트 구성 요청 - accountId: {}", accountId);

        CreateQuizSetByCategoryRequest request = requestForm.toCategoryBasedRequest();
        try {
            CreateQuizSetByCategoryResponse response = quizSetService.registerQuizSetByCategory(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(CreateQuizSetByCategoryResponseForm.from(response));
        } catch (Exception e) {
            log.error("퀴즈 세트 생성 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
        if (part == null) part = actual;
        if (part != actual) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
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
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unsupported part");
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
