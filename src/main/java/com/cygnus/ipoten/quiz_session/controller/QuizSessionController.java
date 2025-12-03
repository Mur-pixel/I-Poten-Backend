package com.cygnus.ipoten.quiz_session.controller;

import com.cygnus.ipoten.quiz.entity.enums.QuizSetType;
import com.cygnus.ipoten.quiz.entity.enums.SeedMode;
import com.cygnus.ipoten.quiz.service.QuizSetQueryService;
import com.cygnus.ipoten.quiz.service.QuizSetService;
import com.cygnus.ipoten.quiz.service.request.CreateQuizSetByFolderRequest;
import com.cygnus.ipoten.quiz.service.response.BuiltQuizSetResponse;
import com.cygnus.ipoten.quiz_session.controller.request_form.*;
import com.cygnus.ipoten.quiz_session.controller.response_form.*;
import com.cygnus.ipoten.quiz_session.entity.QuizSession;
import com.cygnus.ipoten.quiz_session.repository.QuizSessionRepository;
import com.cygnus.ipoten.quiz_session.service.QuizAnswerService;
import com.cygnus.ipoten.quiz_session.service.QuizSessionQueryService;
import com.cygnus.ipoten.quiz_session.service.response.CreateQuizSessionResponse;
import com.cygnus.ipoten.quiz_session.service.response.StartUserQuizSessionResponse;
import com.cygnus.ipoten.redis_cache.RedisCacheService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.cygnus.ipoten.quiz.entity.enums.SeedMode.AUTO;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class QuizSessionController {

    private final RedisCacheService redisCacheService;
    private final QuizAnswerService quizAnswerService;
    private final QuizSetService quizSetService;
    private final QuizSessionQueryService quizSessionQueryService;
    private final QuizSetQueryService quizSetQueryService;
    private final ObjectMapper objectMapper;
    private final QuizSessionRepository quizSessionRepository;

    @Operation(
            summary = "카테고리 기반 퀴즈 세션 생성",
            description = "카테고리를 선택해 세트를 구성하고, 바로 퀴즈 세션을 시작합니다."
    )
    @PostMapping("/me/quiz/sessions/from-category")
    public ResponseEntity<CreateQuizSessionResponseForm> createFromCategory(
            @Valid @RequestBody StartQuizSessionByCategoryRequestForm requestForm,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            // 1) 세트 구성 (questionIds 포함)
            var built = quizSetService.registerQuizSetByCategory(requestForm.toCategoryBasedRequest());

            // 2) 문자열 seedMode -> enum 변환 (대소문자 무시, 예외 시 AUTO)
            SeedMode seedMode;
            try {
                seedMode = SeedMode.valueOf(requestForm.getSeedMode().toUpperCase());
            } catch (Exception e) {
                seedMode = AUTO;
            }

            // 3) 세션 시작
            StartUserQuizSessionResponse started = quizAnswerService.startFromQuizSet(
                    accountId,
                    built.getQuizSetId(),
                    built.getQuestionIds(),
                    seedMode,
                    requestForm.getFixedSeed()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(CreateQuizSessionResponseForm.from(started));
        } catch (IllegalArgumentException e) {
            log.warn("요청 오류", e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("카테고리 기반 세션 생성 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
            summary = "단어장 폴더 기반 퀴즈 세션 생성",
            description = "사용자의 특정 단어장 폴더에 포함된 용어들을 기반으로 퀴즈 세트를 구성하고 세션을 시작합니다."
    )
    @PostMapping("/me/quiz/sessions/from-folder")
    public ResponseEntity<CreateQuizSessionResponseForm> createFromFolder(
            @Valid @RequestBody CreateQuizSessionFromFolderRequestForm requestForm,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        SeedMode mode = resolveSeedMode(requestForm.getSeedMode(), requestForm.getFixedSeed());

        try {
            // 1) 세트 구성
            CreateQuizSetByFolderRequest request = requestForm.toFolderBasedRequest(accountId);
            BuiltQuizSetResponse built = quizSetService.registerQuizSetByFolderReturningQuestions(request);

            // 2) 세션 시작
            StartUserQuizSessionResponse started = quizAnswerService.startFromQuizSet(
                    accountId,
                    built.getQuizSetId(),
                    built.getQuestionIds(),
                    mode,
                    requestForm.getFixedSeed()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(CreateQuizSessionResponseForm.from(started));

        } catch (SecurityException e) {
            log.warn("[from-folder] 권한/소유권 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            log.warn("[from-folder] 요청 오류: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("[from-folder] 서버 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
            summary = "퀴즈 세션 통합 시작 엔드포인트",
            description = "source 값(folder/category/set/daily)에 따라 폴더/카테고리/세트/오늘의퀴즈를 기반으로 세션을 시작합니다."
    )
    @PostMapping("/me/quiz/sessions/start")
    public ResponseEntity<CreateQuizSessionResponseForm> startQuizUnified(
            @Valid @RequestBody StartQuizSessionUnifiedRequestForm requestForm,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        // seedMode 통일 처리
        SeedMode mode = resolveSeedMode(requestForm.getSeedMode(), requestForm.getFixedSeed());

        try {
            String source = requestForm.getSource() == null ? "" : requestForm.getSource().trim().toLowerCase();

            switch (source) {
                case "folder": {
                    if (requestForm.getFolderId() == null) {
                        log.warn("[unified] source=folder인데 folderId 누락");
                        return ResponseEntity.badRequest().build();
                    }

                    // 1) 세트 구성 (기존 폼으로 위임 변환)
                    var folderForm = requestForm.toFolderForm();
                    var built = quizSetService.registerQuizSetByFolderReturningQuestions(folderForm.toFolderBasedRequest(accountId));

                    // 2) 세션 시작
                    StartUserQuizSessionResponse started = quizAnswerService.startFromQuizSet(
                            accountId,
                            built.getQuizSetId(),
                            built.getQuestionIds(),
                            mode,
                            requestForm.getFixedSeed()
                    );
                    return ResponseEntity.status(HttpStatus.CREATED).body(CreateQuizSessionResponseForm.from(started));
                }

                case "category": {
                    if (requestForm.getCategoryId() == null) {
                        log.warn("[unified] source = category인데 categoryId가 누락");
                        return ResponseEntity.badRequest().build();
                    }

                    // 1) 세트 구성
                    var categoryForm = requestForm.toCategoryForm();
                    var built = quizSetService.registerQuizSetByCategory(categoryForm.toCategoryBasedRequest());

                    // 2) 세션 시작
                    StartUserQuizSessionResponse started = quizAnswerService.startFromQuizSet(
                            accountId,
                            built.getQuizSetId(),
                            built.getQuestionIds(),
                            mode,
                            requestForm.getFixedSeed()
                    );
                    return ResponseEntity.status(HttpStatus.CREATED).body(CreateQuizSessionResponseForm.from(started));
                }

                case "set": {
                    if (requestForm.getSetId() == null) {
                        log.warn("[unified] source = set 인데 setId 누락");
                        return ResponseEntity.badRequest().build();
                    }

                    Long setId = requestForm.getSetId();

                    // 세트에 포함된 문제ID 조회 (서비스에 아래 메서드가 없다면 추가 필요)
                    var questionIds = quizSetQueryService.findQuestionIdsBySetId(setId);
                    if (questionIds == null || questionIds.isEmpty()) {
                        log.warn("[unified] setId={} 에 문제 없음", setId);
                        return ResponseEntity.unprocessableEntity().build();
                    }

                    StartUserQuizSessionResponse started = quizAnswerService.startFromQuizSet(
                            accountId,
                            setId,
                            questionIds,
                            mode,
                            requestForm.getFixedSeed()
                    );
                    return ResponseEntity.status(HttpStatus.CREATED)
                            .body(CreateQuizSessionResponseForm.from(started));
                }
                default:
                    log.warn("[unified] source 값이 유효하지 않음: {}", source);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
        } catch (SecurityException e) {
            log.warn("[unified] 권한/소유권 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            log.warn("[unified] 요청 유효성 오류: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.warn("[unified] 서버 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
            summary = "퀴즈 세션 제출",
            description = "사용자가 푼 퀴즈 세션을 제출하고 점수 및 통계를 계산합니다. 이미 제출된 세션이면 요약 정보를 반환합니다."
    )
    @PostMapping("/me/quiz/sessions/{sessionId}/submit")
    public ResponseEntity<?> submitQuizSession(
            @Parameter(description = "제출할 세션 ID", example = "1")
            @PathVariable Long sessionId,
            @Valid @RequestBody SubmitQuizSessionRequestForm requestForm,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("인증 실패: 계정 식별 불가");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            var response = quizAnswerService.submitSession(sessionId, accountId, requestForm);
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

    @Operation(
            summary = "즐겨찾기 기반 퀴즈 세션 생성",
            description = "사용자의 즐겨찾기(북마크) 용어들을 기반으로 퀴즈 세트를 구성하고 곧바로 세션을 시작합니다."
    )
    @PostMapping("/me/quiz/sessions/from-favorites")
    public ResponseEntity<CreateQuizSessionResponseForm> createFromFavorites(
            @Valid @RequestBody CreateQuizSessionRequestForm requestForm,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("인증 실패: 계정 식별 불가");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            CreateQuizSessionResponse response = quizSetService.registerQuizSetByFavorites(requestForm.toServiceRequest(accountId));
            StartUserQuizSessionResponse started =
                    quizAnswerService.startFromQuizSet(
                            accountId,
                            response.getQuizSetId(),
                            response.getQuestionIds(),
                            requestForm.getSeedMode(),
                            requestForm.getFixedSeed()
                    );
            return ResponseEntity.status(HttpStatus.CREATED).body(CreateQuizSessionResponseForm.from(started));
        } catch (IllegalArgumentException e) {
            log.warn("요청 오류", e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("즐겨찾기 기반 세션 생성 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
            summary = "퀴즈 세션 요약 조회",
            description = "정답/오답 개수, 점수 등 해당 세션의 요약 정보를 조회합니다."
    )
    @GetMapping("/me/quiz/sessions/{sessionId}")
    public ResponseEntity<SessionSummaryResponseForm> getSessionSummary(
            @Parameter(description = "조회할 세션 ID", example = "1")
            @PathVariable Long sessionId,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("인증 실패: 계정 식별 불가");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var summary = quizSessionQueryService.getSummary(sessionId, accountId);
        return ResponseEntity.ok(summary);
    }

    @Operation(
            summary = "퀴즈 세션 문항 목록 조회",
            description = "특정 세션에 포함된 문항 목록을 페이지네이션(offset/limit) 형태로 조회합니다. 필요 시 정답 포함 여부를 제어할 수 있습니다."
    )
    @GetMapping("/me/quiz/sessions/{sessionId}/items")
    public ResponseEntity<SessionItemsPageResponseForm> getSessionItems(
            @Parameter(description = "세션 ID", example = "1")
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(name = "includeAnswers", defaultValue = "false") boolean includeAnswers,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("인증 실패: 계정 식별 불가");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        limit = Math.max(1, Math.min(100, limit));

        var page = quizSessionQueryService.getSessionItems(sessionId, accountId, offset, limit, includeAnswers);
        return ResponseEntity.ok(page);
    }

    @Operation(
            summary = "내 퀴즈 세션 목록 조회",
            description = "가장 최근에 진행한 퀴즈 세션들을 상태(IN_PROGRESS/SUBMITTED) 필터와 함께 조회합니다."
    )
    @GetMapping("/me/quiz/sessions")
    public ResponseEntity<SessionListResponseForm> listMySessions(
            @RequestParam(name = "limit", defaultValue = "20") int limit,
            @RequestParam(name = "status", required = false) String status, // SUBMITTED/IN_PROGRESS/null
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (limit <= 0 || limit > 100) limit = 20;
        var list = quizSessionQueryService.listMySessions(accountId, limit, status);
        return ResponseEntity.ok(list);
    }

    @Operation(
            summary = "퀴즈 세션 리뷰 조회",
            description = "각 문항별 정답, 해설, 사용자의 선택 내역을 포함한 세션 리뷰 정보를 조회합니다."
    )
    @GetMapping("/me/quiz/sessions/{sessionId}/review")
    public ResponseEntity<SessionReviewResponseForm> getSessionReview(
            @Parameter(description = "리뷰를 조회할 세션 ID", example = "1")
            @PathVariable Long sessionId,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var review = quizSessionQueryService.getReview(sessionId, accountId);
        return ResponseEntity.ok(review);
    }

    @Operation(
            summary = "오늘의 초성퀴즈 문항 조회",
            description = "DAILY/INITIALS 타입 세션에 대해, 세션 스냅샷 기준 3개의 초성 퀴즈 문항을 조회합니다."
    )
    @GetMapping("/me/quiz/sessions/{sessionId}/questions/initials")
    public ResponseEntity<?> getInitialQuestions(
            @Parameter(description = "오늘의 초성퀴즈 세션 ID", example = "1")
            @PathVariable Long sessionId,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        var session = quizSessionRepository.findByIdAndAccount_Id(sessionId, accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (session.getSeedMode() != SeedMode.DAILY)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "DAILY only");
        if (session.getQuizSet() == null || session.getQuizSet().getQuizSetType() != QuizSetType.INITIALS)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "INITIALS only");

        // 1) 세션 스냅샷(id 리스트) 뽑기
        var qids = extractQuestionIds(session);

        // 2) 세트의 전체 INITIALS 문항 엔티티 조회
        var all = quizSetQueryService.findInitialsQuestionsBySetId(session.getQuizSet().getId());

        // 3) 스냅샷 순서를 우선 보장
        var orderMap = new java.util.HashMap<Long, Integer>();
        for (int i = 0; i < qids.size(); i++) orderMap.put(qids.get(i), i);

        all.sort(java.util.Comparator.comparingInt(q ->
                orderMap.getOrDefault(q.getId(), Integer.MAX_VALUE)
        ));

        // 4) 스냅샷에 있는 것만 골라서 최대 3개
        var picked = new java.util.ArrayList<Map<String, Object>>();
        int order = 1;
        for (var q : all) {
            if (!orderMap.containsKey(q.getId())) continue; // 스냅샷에 없는 건 제외
            picked.add(java.util.Map.of(
                    "id", q.getId(),
                    "order", order++,
                    "questionText", java.util.Optional.ofNullable(q.getQuestionText()).orElse(""),
                    "answerText", java.util.Optional.ofNullable(q.getExplanation()).orElse("")
            ));
            if (picked.size() >= 3) break; // 오늘의 초성은 3개 고정
        }

        var body = new java.util.LinkedHashMap<String, Object>();
        body.put("sessionId", sessionId);
        body.put("total", picked.size());
        body.put("questions", picked);
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

    /**
     * seedMode 문자열과 fixedSeed 값에 따라 SeedMode를 결정한다.
     * - raw가 유효한 enum 문자열이면 그대로 사용
     * - raw가 없고 fixedSeed가 있으면 FIXED
     * - 그 외에는 AUTO
     */
    private SeedMode resolveSeedMode(String raw, Long fixedSeed) {
        if (raw != null && !raw.isBlank()) {
            try { return SeedMode.valueOf(raw.trim().toUpperCase()); }
            catch (Exception ignore) { /* fall-through */ }
        }
        if (fixedSeed != null) return SeedMode.FIXED;
        return AUTO;
    }

    /**
     * UserQuizSession에서 질문 ID 목록을 추출한다.
     * 우선순위:
     *  0) 엔티티에 구현된 getSnapshotQuestionIds() 사용
     *  1) getQuestionIds() 리플렉션 호출 (구버전 호환)
     *  2) questionsSnapshotJson을 파싱해 [1,2,3] 또는 [{id:1}, {id:2}] 형태를 모두 지원
     */
    private List<Long> extractQuestionIds(QuizSession session) {
        // 0) 엔티티에 이미 구현된 헬퍼가 있으면 최우선 사용
        try {
            List<Long> ids = session.getSnapshotQuestionIds(); // 존재함
            if (ids != null && !ids.isEmpty()) return ids;
        } catch (Exception ignore) {}

        // 1) getQuestionIds()가 있는 환경을 위한 리플렉션 (없으면 그냥 통과)
        try {
            var m = session.getClass().getMethod("getQuestionIds");
            @SuppressWarnings("unchecked")
            List<Long> ids = (List<Long>) m.invoke(session);
            if (ids != null && !ids.isEmpty()) return ids;
        } catch (NoSuchMethodException ignore) {
            // method가 없는 경우: 무시하고 스냅샷 JSON 파싱으로 진행
        } catch (Exception e) {
            log.warn("[initials] getQuestionIds() reflection failed", e);
        }

        // 2) 스냅샷 JSON 파싱 (두 형태 모두 지원: [1,2,3] 또는 [{id:1}, {id:2}])
        String snap = session.getQuestionsSnapshotJson();
        if (snap == null || snap.isBlank()) return List.of();

        try {
            JsonNode arr = objectMapper.readTree(snap);
            List<Long> out = new ArrayList<>();
            if (arr.isArray()) {
                for (JsonNode n : arr) {
                    if (n.isNumber()) {
                        out.add(n.asLong());
                    } else if (n.isObject()) {
                        long id = n.path("id").asLong(0L);
                        if (id > 0L) out.add(id);
                    }
                }
            }
            return out;
        } catch (Exception e) {
            log.warn("[initials] snapshot parse failed: {}", snap, e);
            return List.of();
        }
    }
}