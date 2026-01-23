package com.cygnus.ipoten.quiz_admin.controller;

import com.cygnus.ipoten.quiz_admin.service.QuizAdminService;

import com.cygnus.ipoten.quiz_admin.controller.request_form.CreateQuizQuestionRequestForm;
import com.cygnus.ipoten.quiz_admin.controller.response_form.CreateQuizChoiceListResponseForm;
import com.cygnus.ipoten.quiz_admin.controller.response_form.CreateQuizQuestionResponseForm;
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
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
@Tag(name = "QuizAdmin", description = "퀴즈 문제 관리 및 사용자의 퀴즈 데이터 삭제 API")
public class QuizAdminController {

    private final QuizAdminService quizAdminService;
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
     * 내부(Admin/배치) 호출용: 특정 계정과 연관된 quiz 도메인 "사용자 데이터" 정리.
     *
     * 삭제 대상(계정 스코프):
     * - quiz_wrong_note (account_id 기준)
     * - session_answer  (해당 account의 quiz_session을 조인해서 삭제)
     * - quiz_session    (해당 account의 세션; parent_session 포함)
     *
     * 삭제하지 않음(콘텐츠/품질 로그/운영 데이터로 유지):
     * - quiz_set / quiz_question / quiz_choice 등 "문항 콘텐츠"는 공유/재사용 및 품질 분석 대상이므로
     *   이 API에서는 삭제하지 않습니다.
     *
     */
    @Operation(
            summary = "[내부] 특정 계정의 퀴즈 사용자 데이터 일괄 삭제",
            description = """
                Admin/배치용.
                accountId에 연결된 퀴즈 세션/답안/오답노트(사용자 데이터)만 삭제합니다.
                quiz_set/quiz_question/quiz_choice(문항 콘텐츠)는 이 API에서 삭제하지 않습니다.
                """
    )
    @DeleteMapping("/internal/admin/accounts/{accountId}/quiz:erase")
    public ResponseEntity<?> eraseQuizByAccount(
            @Parameter(description = "정리 대상 계정 ID", example = "1")
            @PathVariable Long accountId) {
        var result = quizAdminService.eraseByAccountId(accountId);

        Map<String, Object> body = Map.of(
                "accountId", accountId,
                "deleted", Map.of(
                        "wrong_note",          result.getWrongNotes(),
                        "session_answer",      result.getSessionAnswers(),
                        "quiz_session",        result.getSessions(),
                        "orphan_quiz_choice",  result.getOrphanChoices(),
                        "orphan_quiz_question",result.getOrphanQuestions(),
                        "orphan_quiz_set",     result.getOrphanSets()
                )
        );

        log.info("[quiz:erase] {}", body);
        return ResponseEntity.ok(body);
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
