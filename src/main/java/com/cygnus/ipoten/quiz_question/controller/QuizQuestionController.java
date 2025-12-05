package com.cygnus.ipoten.quiz_question.controller;

import com.cygnus.ipoten.quiz.controller.response_form.CreateQuizChoiceListResponseForm;
import com.cygnus.ipoten.quiz_question.controller.request_form.CreateQuizQuestionRequestForm;
import com.cygnus.ipoten.quiz_question.controller.response_form.CreateQuizQuestionResponseForm;
import com.cygnus.ipoten.quiz_question.entity.QuizChoice;
import com.cygnus.ipoten.quiz_question.service.QuizChoiceService;
import com.cygnus.ipoten.quiz_question.service.QuizQuestionService;
import com.cygnus.ipoten.quiz_question.service.request.CreateQuizChoiceRequest;
import com.cygnus.ipoten.quiz_question.service.request.CreateQuizQuestionRequest;
import com.cygnus.ipoten.quiz_question.service.response.CreateQuizQuestionResponse;
import com.cygnus.ipoten.redis_cache.RedisCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "QuizQuestion", description = "퀴즈 문제 관리 API")
public class QuizQuestionController {

    private final RedisCacheService redisCacheService;
    private final QuizQuestionService quizQuestionService;
    private final QuizChoiceService quizChoiceService;

    @Operation(
            summary = "용어 기반 퀴즈 문제 등록",
            description = "특정 용어(termId)에 연결된 퀴즈 문항(질문/정답/해설 등)을 등록합니다."
    )
    @PostMapping("/terms/{termId}/quiz-questions")
    public ResponseEntity<CreateQuizQuestionResponseForm> createQuizQuestion (
            @Parameter(description = "퀴즈 문제를 생성할 용어 ID", example = "1")
            @PathVariable("termId") Long termId,
            @Valid @RequestBody CreateQuizQuestionRequestForm requestForm,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("인증 실패: 계정 식별 불가");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("용어에 대한 퀴즈 문제 등록 요청 - termId: {}, accountId: {}",  termId, accountId);

        CreateQuizQuestionRequest request = requestForm.toCreateQuizQuestionRequest(termId);
        try {
            CreateQuizQuestionResponse response = quizQuestionService.registerQuizQuestion(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(CreateQuizQuestionResponseForm.from(response));
        } catch (Exception e) {
            log.error("퀴즈 세트 생성 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
            summary = "퀴즈 보기(선택지) 일괄 생성",
            description = "특정 퀴즈 문항에 대해 보기를 여러 개 한 번에 생성합니다."
    )
    @PostMapping("/quiz-questions/{quizQuestionId}/choices")
    public ResponseEntity<CreateQuizChoiceListResponseForm> createQuizChoices(
            @Parameter(description = "보기를 생성할 퀴즈 문항 ID", example = "1")
            @PathVariable("quizQuestionId") Long quizQuestionId,
            @Valid @RequestBody List<@Valid CreateQuizChoiceRequest> requestList,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("인증 실패: 계정 식별 불가");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (requestList == null || requestList.isEmpty()) {
            log.warn("요청 유효성 오류: choices 리스트가 비어있음");
            return ResponseEntity.badRequest().build();
        }

        log.info("퀴즈 보기 생성 요청 - quizQuestionId: {}, accountId: {}", quizQuestionId, accountId);

        try {
            List<QuizChoice> savedChoices = quizChoiceService.registerQuizChoices(quizQuestionId, requestList);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(CreateQuizChoiceListResponseForm.from(savedChoices));
        } catch (Exception e) {
            log.error("퀴즈 보기 생성 실패", e);
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
