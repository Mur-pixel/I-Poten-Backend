package com.cygnus.ipoten.quiz.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cygnus.ipoten.quiz.controller.request_form.*;
import com.cygnus.ipoten.quiz.controller.response_form.*;
import com.cygnus.ipoten.quiz.entity.QuizChoice;
import com.cygnus.ipoten.quiz.entity.enums.QuizSetType;
import com.cygnus.ipoten.quiz.entity.enums.SeedMode;
import com.cygnus.ipoten.quiz.repository.QuizSetRepository;
import com.cygnus.ipoten.quiz.repository.UserQuizSessionRepository;
import com.cygnus.ipoten.quiz.service.*;
import com.cygnus.ipoten.quiz.service.request.CreateQuizChoiceRequest;
import com.cygnus.ipoten.quiz.service.request.CreateQuizQuestionRequest;
import com.cygnus.ipoten.quiz.service.request.CreateQuizSetByCategoryRequest;
import com.cygnus.ipoten.quiz.service.request.CreateQuizSetByFolderRequest;
import com.cygnus.ipoten.quiz.service.response.*;
import com.cygnus.ipoten.redis_cache.RedisCacheService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.cygnus.ipoten.quiz.entity.UserQuizSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.cygnus.ipoten.quiz.entity.enums.SeedMode.AUTO;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class QuizController {

    private final QuizQuestionService quizQuestionService;
    private final QuizSetService quizSetService;
    private final QuizChoiceService quizChoiceService;
    private final RedisCacheService redisCacheService;
    private final UserQuizAnswerService userQuizAnswerService;
    private final UserQuizSessionQueryService userQuizSessionQueryService;
    private final UserQuizEraseService userQuizEraseService;
    private final QuizSetQueryService quizSetQueryService;
    private final DailyQuizService dailyQuizService;
    private final UserQuizSessionRepository userQuizSessionRepository;
    private final ObjectMapper objectMapper;
    private final QuizSetRepository quizSetRepository;
    private final QuizMetricsTrendService quizMetricsTrendService;

    /** 공통: 쿠키/헤더에서 토큰 추출 후 Redis에서 accountId 조회(없으면 null) */
    // -> 정책 변경: 쿠키 전용으로 단순화
    private Long resolveAccountId(String userToken) {
        if (userToken == null || userToken.isBlank()) {
            return null;
        }
        return redisCacheService.getValueByKey(userToken, Long.class); // TTL 만료/무효면 null
    }

    // 용어 기반 퀴즈 문제 등록하기
    @PostMapping("/terms/{termId}/quiz-questions")
    public ResponseEntity<CreateQuizQuestionResponseForm> createQuizQuestion (
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

    // 카테고리 기반 퀴즈 세트 자동 생성
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

    // 퀴즈 보기 생성
    @PostMapping("/quiz-questions/{quizQuestionId}/choices")
    public ResponseEntity<CreateQuizChoiceListResponseForm> createQuizChoices(
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

    // 즐겨찾기 용어로 퀴즈 세션 만들기
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
                    userQuizAnswerService.startFromQuizSet(
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

    // 카테고리 기반 퀴즈 세션 만들기
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
            StartUserQuizSessionResponse started = userQuizAnswerService.startFromQuizSet(
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

    // 단어장(스푼노트) 폴더 기반 퀴즈 세션 즉시 만들기
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
            StartUserQuizSessionResponse started = userQuizAnswerService.startFromQuizSet(
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

    // 프론트에서 선택한 것(폴더/카테고리)을 그대로 반영해 퀴즈 세션 시작하기 (통합 엔드포인트)
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
                    StartUserQuizSessionResponse started = userQuizAnswerService.startFromQuizSet(
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
                    StartUserQuizSessionResponse started = userQuizAnswerService.startFromQuizSet(
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

                    StartUserQuizSessionResponse started = userQuizAnswerService.startFromQuizSet(
                            accountId,
                            setId,
                            questionIds,
                            mode,
                            requestForm.getFixedSeed()
                    );
                    return ResponseEntity.status(HttpStatus.CREATED)
                            .body(CreateQuizSessionResponseForm.from(started));
                }
                case "daily": {
                    var zone = java.time.ZoneId.of("Asia/Seoul");
                    var d = (requestForm.getDate() == null || requestForm.getDate().isBlank())
                            ? java.time.LocalDate.now(zone)
                            : java.time.LocalDate.parse(requestForm.getDate());

                    var p = QuizSetType.fromParam(requestForm.getType());
                    var r = com.cygnus.ipoten.quiz.entity.enums.JobRole.from(
                            java.util.Optional.ofNullable(requestForm.getRole()).orElse("GENERAL")
                    );

                    var built = dailyQuizService.resolve(d, p, r);

                    // 실제 세트 파트 확인(로그)
                    var actualPart = quizSetQueryService.findPartTypeBySetId(built.getQuizSetId()).orElse(null);
                    log.info("[unified/daily] req.type={}, resolvedPart={}, setId={}, actualPart={}",
                            requestForm.getType(), p, built.getQuizSetId(), actualPart);

                    // === 여기서부터 핵심: 세트에서 questionIds를 다시 뽑아 3개로 고정 ===
                    List<Long> qids = quizSetQueryService.findQuestionIdsBySetId(built.getQuizSetId());
                    if (qids == null || qids.isEmpty()) {
                        throw new IllegalStateException("No questions in setId=" + built.getQuizSetId());
                    }
                    // 부족하면 있는 만큼만 쓰되, 경고 로그 남김
                    if (qids.size() < 3) {
                        log.warn("[unified/daily] setId={} only {} questions; initials needs 3.", built.getQuizSetId(), qids.size());
                    }
                    // 정확히 3개로 자르기(최대 3개)
                    qids = qids.stream().limit(3).toList();

                    // (선택) INITIALS 요청인데 세트가 CHOICE/OX로 남아있는 레거시 상황은 임시로 통과시키고, DB는 별도 교정 권장
                    if (actualPart != null && actualPart != p) {
                        log.warn("Built set partType mismatch: requested={} actual={}. Proceeding temporarily.", p, actualPart);
                        // 권장: DB에서 세트의 part_type을 p로 교정 (아래 2) 참고)
                    }

                    var started = userQuizAnswerService.startFromQuizSet(
                            accountId,
                            built.getQuizSetId(),
                            qids,
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

    // 자신이 푼 답안을 제출하여 점수 확인하기
    @PostMapping("/me/quiz/sessions/{sessionId}/submit")
    public ResponseEntity<?> submitQuizSession(
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
            var response = userQuizAnswerService.submitSession(sessionId, accountId, requestForm);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            log.warn("세션 접근 거부", e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            String message = String.valueOf(e.getMessage());
            if (message.contains("이미 제출된 세션")) {
                var summary = userQuizSessionQueryService.getSummary(sessionId, accountId);
                return ResponseEntity.ok(summary);
            }
            throw e;
        } catch (Exception e) {
            log.error("세션 제출 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 오답노트만 다시 풀기
    @PostMapping("/me/quiz/sessions/{sessionId}/retry-wrong")
    public ResponseEntity<CreateQuizSessionResponseForm> retryWrongOnly(
            @PathVariable Long sessionId,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("인증 실패: 계정 식별 불가");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            StartUserQuizSessionResponse started = userQuizAnswerService.startRetryWrongOnly(sessionId, accountId);
            return ResponseEntity.status(HttpStatus.CREATED).body(CreateQuizSessionResponseForm.from(started));
        } catch(SecurityException e) {
            log.warn("세션 접근 거부", e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("오답세션 생성 유효성 오류", e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("오답세션 생성 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 세션 요약 단건 조회하기
    @GetMapping("/me/quiz/sessions/{sessionId}")
    public ResponseEntity<SessionSummaryResponseForm> getSessionSummary(
            @PathVariable Long sessionId,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("인증 실패: 계정 식별 불가");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var summary = userQuizSessionQueryService.getSummary(sessionId, accountId);
        return ResponseEntity.ok(summary);
    }

    // 세션 문제 페이지 조회하기(페이지네이션으로 분리해 조회)
    @GetMapping("/me/quiz/sessions/{sessionId}/items")
    public ResponseEntity<SessionItemsPageResponseForm> getSessionItems(
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

        var page = userQuizSessionQueryService.getSessionItems(sessionId, accountId, offset, limit, includeAnswers);
        return ResponseEntity.ok(page);
    }

    // 최근 세션 목록 조회하기
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
        var list = userQuizSessionQueryService.listMySessions(accountId, limit, status);
        return ResponseEntity.ok(list);
    }

    // 세션 리뷰(정답/해설/내 선택)
    @GetMapping("/me/quiz/sessions/{sessionId}/review")
    public ResponseEntity<SessionReviewResponseForm> getSessionReview(
            @PathVariable Long sessionId,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var review = userQuizSessionQueryService.getReview(sessionId, accountId);
        return ResponseEntity.ok(review);
    }

    @GetMapping("/quiz/sets/{setId}/questions")
    public ResponseEntity<?> getQuestionsBySet(
            @PathVariable Long setId,
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
                            "questionText", java.util.Optional.ofNullable(q.getQuestionText()).orElse(""),
                            "answerText", java.util.Optional.ofNullable(q.getAnswerText()).orElse("")
                    ));
                }
                return ResponseEntity.ok(java.util.Map.of("total", out.size(), "questions", out));
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unsupported part");
        }
    }

    @GetMapping("/me/quiz/timeline")
    public ResponseEntity<?> getMyTimeline(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "type", required = false, defaultValue = "ALL") String type,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        QuizSetType part = ("ALL".equalsIgnoreCase(type) || type == null || type.isBlank())
                ? null
                : QuizSetType.valueOf(type.toUpperCase());
        var result = userQuizSessionQueryService.getTimeline(accountId, q, part, page, size);
        return ResponseEntity.ok(result);
    }

    // 사용자의 학습 성과 변화 추이 확인
    @GetMapping("/me/quiz/metrics")
    public ResponseEntity<QuizTrendResponseForm> getTrend(
            @RequestParam String metric,
            @RequestParam(defaultValue = "30d") String span,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(quizMetricsTrendService.getTrend(accountId, metric, span));
    }

    // 사용자가 완료한 퀴즈 세트의 총 개수를 조회
    @GetMapping("/me/quiz/metrics/total-sets")
    public ResponseEntity<TotalSetsResponseForm> getTotalSets(
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        long total = quizMetricsTrendService.getTotalSets(accountId);
        return ResponseEntity.ok(new TotalSetsResponseForm(total));
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

    private SeedMode resolveSeedMode(String raw, Long fixedSeed) {
        if (raw != null && !raw.isBlank()) {
            try { return SeedMode.valueOf(raw.trim().toUpperCase()); }
            catch (Exception ignore) { /* fall-through */ }
        }
        if (fixedSeed != null) return SeedMode.FIXED;
        return AUTO;
    }

    /**
     * 내부(Admin) 호출용: quiz 도메인 데이터(해당 계정 것만) 삭제
     * - user_quiz_session, session_answer(해당 세션들), user_wrong_note(해당 계정) 삭제
     * - '고아 quiz_set'이 되면 세트/문항/보기까지 함께 정리
     *
     * 주의: quiz_set 자체는 계정 컬럼이 없어 타 계정이 공유 중일 수 있음.
     *      따라서 '현재 어떤 세션도 참조하지 않는' 세트만 정리
     */
    @DeleteMapping("/internal/admin/accounts/{accountId}/quiz:erase")
    public ResponseEntity<?> eraseQuizByAccount(@PathVariable Long accountId) {
        var result = userQuizEraseService.eraseByAccountId(accountId);

        Map<String, Object> body = Map.of(
                "accountId", accountId,
                "deleted", Map.of(
                        "wrong_note",          result.getWrongNotes(),
                        "session_answer",      result.getSessionAnswers(),
                        "user_quiz_session",   result.getSessions(),
                        "orphan_quiz_choice",  result.getOrphanChoices(),
                        "orphan_quiz_question",result.getOrphanQuestions(),
                        "orphan_quiz_set",     result.getOrphanSets()
                )
        );

        log.info("[quiz:erase] {}", body);
        return ResponseEntity.ok(body);
    }

    // 오늘의 초성퀴즈: 문항 조회
    @GetMapping("/me/quiz/sessions/{sessionId}/questions/initials")
    public ResponseEntity<?> getInitialQuestions(
            @PathVariable Long sessionId,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        var session = userQuizSessionRepository.findByIdAndAccount_Id(sessionId, accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (session.getSeedMode() != SeedMode.DAILY)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "DAILY only");
        if (session.getQuizSet() == null || session.getQuizSet().getPartType() != QuizSetType.INITIALS)
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
                    "answerText", java.util.Optional.ofNullable(q.getAnswerText()).orElse("")
            ));
            if (picked.size() >= 3) break; // 오늘의 초성은 3개 고정
        }

        var body = new java.util.LinkedHashMap<String, Object>();
        body.put("sessionId", sessionId);
        body.put("total", picked.size());
        body.put("questions", picked);
        return ResponseEntity.ok(body);
    }


    private List<Long> extractQuestionIds(UserQuizSession session) {
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
