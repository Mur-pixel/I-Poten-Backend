package com.cygnus.ipoten.quiz_session.service;

import com.cygnus.ipoten.quiz_analytics.controller.response_form.QuizTimelineResponseForm;
import com.cygnus.ipoten.quiz_question.entity.QuizChoice;
import com.cygnus.ipoten.quiz_question.entity.QuizQuestion;
import com.cygnus.ipoten.quiz_question.entity.QuizTextAnswer;
import com.cygnus.ipoten.quiz_question.entity.enums.QuestionType;
import com.cygnus.ipoten.quiz_question.repository.QuizChoiceRepository;
import com.cygnus.ipoten.quiz_question.repository.QuizQuestionRepository;
import com.cygnus.ipoten.quiz_question.repository.QuizTextAnswerRepository;
import com.cygnus.ipoten.quiz_session.controller.response_form.SessionItemsPageResponseForm;
import com.cygnus.ipoten.quiz_session.controller.response_form.SessionListResponseForm;
import com.cygnus.ipoten.quiz_session.controller.response_form.SessionReviewResponseForm;
import com.cygnus.ipoten.quiz_session.controller.response_form.SessionSummaryResponseForm;
import com.cygnus.ipoten.quiz_session.entity.QuizSession;
import com.cygnus.ipoten.quiz_session.entity.enums.SeedMode;
import com.cygnus.ipoten.quiz_session.entity.enums.SessionSourceType;
import com.cygnus.ipoten.quiz_session.entity.enums.SessionStatus;
import com.cygnus.ipoten.quiz_session.repository.QuizSessionRepository;
import com.cygnus.ipoten.quiz_session.repository.QuizSessionTimelineRepository;
import com.cygnus.ipoten.quiz_session.service.response.InitialsQuestionsResponse;
import com.cygnus.ipoten.quiz_session_answer.entity.QuizSessionAnswer;
import com.cygnus.ipoten.quiz_session_answer.repository.QuizSessionAnswerRepository;
import com.cygnus.ipoten.quiz_set.entity.QuizSet;
import com.cygnus.ipoten.quiz_set.entity.enums.QuizSetType;
import com.cygnus.ipoten.quiz_set.repository.QuizSetRepository;
import com.cygnus.ipoten.quiz_set.service.QuizSetQueryService;
import com.cygnus.ipoten.term_category.repository.TermCategoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizSessionQueryServiceImpl implements QuizSessionQueryService {

    private final QuizSessionRepository quizSessionRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizChoiceRepository quizChoiceRepository;
    private final QuizSetRepository quizSetRepository;
    private final QuizSessionAnswerRepository quizSessionAnswerRepository;
    private final QuizTextAnswerRepository quizTextAnswerRepository;
    private final QuizSessionTimelineRepository timelineRepository;
    private final QuizSetQueryService quizSetQueryService;
    private final TermCategoryRepository termCategoryRepository;
    private final ObjectMapper objectMapper;

    private static final Duration EXPIRE_AFTER = Duration.ofMinutes(60);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(KST);

    /** 상태/권한/만료 전환 포함 단건 요약 */
    @Override
    @Transactional
    public SessionSummaryResponseForm getSummary(Long sessionId, Long accountId) {
        QuizSession quizsession =
                quizSessionRepository.findByIdAndAccount_Id(sessionId, accountId)
                        .orElseThrow(() -> new SecurityException("세션이 없거나 권한이 없습니다."));

        SessionStatus effective = ensureCurrentStatus(quizsession);

        return SessionSummaryResponseForm.builder()
                .sessionId(quizsession.getId())
                .status(effective)
                .totalCount(
                        Optional.ofNullable(quizsession.getSnapshotQuestionIds())
                                .map(List::size).orElse(0)
                )
                .lastActivityAt(quizsession.getLastActivityAt())
                .seedMode(quizsession.getSeedMode())
                .build();
    }

    /** 문항 페이지: 스냅샷 순서 기준 offset/limit */
    @Override
    @Transactional
    public SessionItemsPageResponseForm getSessionItems(
            Long sessionId, Long accountId, int offset, int limit, boolean includeAnswers
    ) {

        QuizSession quizsession =
                quizSessionRepository.findByIdAndAccount_Id(sessionId, accountId)
                        .orElseThrow(() -> new SecurityException("세션이 없거나 권한이 없습니다."));

        SessionStatus effective = ensureCurrentStatus(quizsession);
        if (effective == SessionStatus.EXPIRED) {
            throw new IllegalStateException("만료된 세션 문제 조회는 금지됩니다.");
        }
        if (effective == SessionStatus.IN_PROGRESS) {
            quizsession.touchActivity();
        }

        // 1) 페이징
        List<Long> allIds = Optional.ofNullable(quizsession.getSnapshotQuestionIds()).orElse(List.of());
        int total = allIds.size();
        int from = Math.max(0, Math.min(offset, total));
        int to   = Math.max(from, Math.min(from + limit, total));
        List<Long> pageIds = allIds.subList(from, to);

        if (pageIds.isEmpty()) {
            return SessionItemsPageResponseForm.builder()
                    .sessionId(quizsession.getId())
                    .offset(offset)
                    .limit(limit)
                    .total(total)
                    .items(List.of())
                    .build();
        }

        // 2) 질문 로드 + 순서 보존을 위해 map만 만들고, 실제 순서는 pageIds로 돌림
        Map<Long, QuizQuestion> byId = quizQuestionRepository.findAllById(pageIds).stream()
                .collect(Collectors.toMap(QuizQuestion::getId, q -> q));

        // 3) 보기 로드
        List<QuizChoice> allChoices = quizChoiceRepository.findByQuizQuestionIdIn(pageIds);
        Map<Long, List<QuizChoice>> choicesByQ = allChoices.stream()
                .collect(Collectors.groupingBy(c -> c.getQuizQuestion().getId()));

        // 4) 노출 정책
        boolean canRevealAnswers = (effective == SessionStatus.SUBMITTED) && includeAnswers; // 정답(OX/객관식 + 초성 expectedText)
        boolean canRevealExplanation = (effective == SessionStatus.SUBMITTED);              // 해설은 제출 후만

        // 5) INITIALS 정답 텍스트는 "힌트 생성"을 위해서도 필요하므로 pageIds 내 INITIALS만 배치 조회
        List<Long> initialsIds = pageIds.stream()
                .filter(id -> {
                    QuizQuestion q = byId.get(id);
                    return q != null && q.getQuestionType() == QuestionType.INITIALS;
                })
                .toList();

        Map<Long, String> expectedByQid = initialsIds.isEmpty()
                ? Map.of()
                : quizTextAnswerRepository.findByQuizQuestion_IdIn(initialsIds).stream()
                .collect(Collectors.toMap(
                        a -> a.getQuizQuestion().getId(),
                        QuizTextAnswer::getAnswerText,
                        (oldV, newV) -> oldV
                ));

        // 6) 아이템 구성
        List<SessionItemsPageResponseForm.Item> items = new ArrayList<>(pageIds.size());

        for (Long qid : pageIds) {
            QuizQuestion question = byId.get(qid);
            if (question == null) continue;

            QuestionType qt = question.getQuestionType();
            boolean isInitials = (qt == QuestionType.INITIALS);

            List<SessionItemsPageResponseForm.Choice> choiceList = List.of();
            Long correctChoiceId = null;

            // 초성 힌트
            String initialsHint = null;

            // 제출/리뷰에서만 내려갈 정답 텍스트(초성 정답)
            String expectedText = null;

            if (isInitials) {
                String ans = expectedByQid.get(qid);

                // 힌트는 항상 내려줌(정답 텍스트가 아니라 초성만)
                initialsHint = toInitialsHint(ans);

                // 제출 완료 + includeAnswers=true 일 때만 정답 텍스트 노출
                if (canRevealAnswers) {
                    expectedText = ans;
                    if (expectedText == null) {
                        log.warn("[sessionItems] initials expectedText missing. qid={}", qid);
                    }
                }
            } else {
                List<QuizChoice> qChoices = choicesByQ.getOrDefault(qid, List.of());

                // OX는 보기 순서 고정
                if (qt == QuestionType.OX) {
                    qChoices = qChoices.stream()
                            .sorted(Comparator.comparing((QuizChoice c) -> {
                                String t = Optional.ofNullable(c.getChoiceText()).orElse("")
                                        .trim().toUpperCase();
                                return "O".equals(t) ? 0 : "X".equals(t) ? 1 : 2;
                            }).thenComparingLong(QuizChoice::getId))
                            .toList();
                }

                QuizChoice answerChoice = canRevealAnswers
                        ? qChoices.stream().filter(QuizChoice::isAnswer).findFirst().orElse(null)
                        : null;

                correctChoiceId = (answerChoice != null) ? answerChoice.getId() : null;

                choiceList = qChoices.stream()
                        .map(c -> SessionItemsPageResponseForm.Choice.builder()
                                .id(c.getId())
                                .text(c.getChoiceText())
                                .isAnswer(canRevealAnswers ? c.isAnswer() : null)
                                .build())
                        .toList();
            }

            items.add(SessionItemsPageResponseForm.Item.builder()
                    .questionId(qid)
                    .questionType(qt)
                    .questionText(question.getQuestionText())
                    .initialsHint(initialsHint)
                    .correctChoiceId(correctChoiceId)
                    .expectedText(expectedText)
                    .explanation(canRevealExplanation ? question.getExplanation() : null)
                    .choices(choiceList)
                    .build());
        }

        return SessionItemsPageResponseForm.builder()
                .sessionId(quizsession.getId())
                .offset(offset)
                .limit(limit)
                .total(total)
                .items(items)
                .build();
    }

    /** 최근 세션 목록 */
    @Override
    public SessionListResponseForm listMySessions(Long accountId, int limit, String statusFilter) {
        // 정렬/페이징 가드 (limit: 1~100)
        int pageSize = Math.max(1, Math.min(100, limit));
        PageRequest pr = PageRequest.of(0, pageSize, Sort.by(Sort.Direction.DESC, "startedAt"));

        // 상태 필터 파싱 (null/무효값이면 전체)
        SessionStatus status = null;
        if (statusFilter != null && !statusFilter.isBlank()) {
            try {
                status = SessionStatus.valueOf(statusFilter.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignore) { /* ALL */ }
        }

        // 조회
        var page = (status == null)
                ? quizSessionRepository.findByAccount_Id(accountId, pr)
                : quizSessionRepository.findByAccount_IdAndSessionStatus(accountId, status, pr);

        var sessions = page.getContent();

        Map<Long, String> setTitleById = loadSetTitleByIdIncludingAncestors(sessions);
        Map<Long, String> catNameById  = loadCatNameByIdIncludingAncestors(sessions);

        // 매핑
        List<SessionListResponseForm.Item> items = new ArrayList<>(page.getNumberOfElements());
        for (QuizSession s : sessions) {
            Integer total = Optional.ofNullable(s.getTotal())
                    .orElse(Optional.ofNullable(s.getSnapshotQuestionIds()).map(List::size).orElse(0));

            Integer correct = null;
            Double score = null;
            Integer scorePercent = null;

            if (s.getSessionStatus() == SessionStatus.SUBMITTED) {
                List<QuizSessionAnswer> ans = quizSessionAnswerRepository.findByQuizSession_Id(s.getId());
                int c = (int) ans.stream().filter(QuizSessionAnswer::isCorrect).count();
                correct = c;
                if (total != null && total > 0) {
                    score = c * 100.0 / total;
                    scorePercent = (int) Math.round(score);
                }
            }

            items.add(SessionListResponseForm.Item.builder()
                    .sessionId(s.getId())
                    .status(s.getSessionStatus())
                    .mode(s.getSessionMode())
                    .total(total)
                    .correct(correct)
                    .elapsedMs(s.getElapsedMs())
                    .startedAt(s.getStartedAt())
                    .submittedAt(s.getSubmittedAt())
                    .score(score)
                    .scorePercent(scorePercent)
                    .title(resolveTitle(s, setTitleById, catNameById))
                    .build());
        }

        return SessionListResponseForm.builder()
                .items(items)
                .build();
    }

    /** 세션 리뷰(정답/해설/내 선택 + 용어/카테고리/보기도 함께) */
    @Override
    public SessionReviewResponseForm getReview(Long sessionId, Long accountId) {
        QuizSession s =
                quizSessionRepository.findByIdAndAccount_Id(sessionId, accountId)
                        .orElseThrow(() -> new SecurityException("세션이 없거나 권한이 없습니다."));

        if (s.getSessionStatus() != SessionStatus.SUBMITTED) {
            throw new IllegalStateException("제출 완료된 세션만 리뷰할 수 있습니다.");
        }

        // 세션 스냅샷 순서 유지
        List<Long> qids = Optional.ofNullable(s.getSnapshotQuestionIds()).orElse(List.of());
        if (qids.isEmpty()) {
            return SessionReviewResponseForm.builder()
                    .sessionId(s.getId())
                    .total(0)
                    .correct(0)
                    .items(List.of())
                    .build();
        }

        // 질문/보기/답변 한번에 로드
        Map<Long, QuizQuestion> qById = quizQuestionRepository.findAllById(qids)
                .stream().collect(Collectors.toMap(QuizQuestion::getId, q -> q));

        List<QuizChoice> allChoices = quizChoiceRepository.findByQuizQuestionIdIn(qids);
        Map<Long, List<QuizChoice>> choicesByQ = allChoices.stream()
                .collect(Collectors.groupingBy(c -> c.getQuizQuestion().getId()));

        List<Long> initialsQids = qById.values().stream()
                .filter(qq -> qq.getQuestionType() == QuestionType.INITIALS)
                .map(QuizQuestion::getId)
                .toList();

        // INITIALS expectedText를 quiz_text_answer에서 가져오기
        Map<Long, String> expectedTextByQid = initialsQids.isEmpty()
                ? Map.of()
                : quizTextAnswerRepository.findByQuizQuestion_IdIn(initialsQids).stream()
                .collect(Collectors.toMap(
                        a -> a.getQuizQuestion().getId(),
                        QuizTextAnswer::getAnswerText,
                        (oldV, newV) -> oldV
                ));

        List<QuizSessionAnswer> answers = quizSessionAnswerRepository.findByQuizSession_Id(sessionId);
        Map<Long, QuizSessionAnswer> ansByQ = answers.stream()
                .collect(Collectors.toMap(a -> a.getQuizQuestion().getId(), a -> a, (a, b) -> a, LinkedHashMap::new));

        int correctCnt = 0;
        List<SessionReviewResponseForm.Item> items = new ArrayList<>();

        for (Long qid : qids) {
            QuizQuestion q = qById.get(qid);
            if (q == null) continue;

            QuizSessionAnswer my = ansByQ.get(qid);

            QuestionType qt = q.getQuestionType();
            boolean isInitials = (qt == QuestionType.INITIALS);

            Long myChoiceId = (my != null) ? my.getSubmittedChoiceId() : null;
            String mySubmittedText = (my != null) ? my.getSubmittedText() : null;

            // INITIALS expectedText
            String expectedText = isInitials ? expectedTextByQid.get(qid) : null;

            // choice 기반 데이터
            List<SessionReviewResponseForm.Choice> optionList = List.of();
            Long answerChoiceId = null;

            if (!isInitials) {
                List<QuizChoice> qChoices = choicesByQ.getOrDefault(qid, List.of());

                // OX는 보기 순서 고정
                if (qt == QuestionType.OX) {
                    qChoices = qChoices.stream()
                            .sorted(Comparator.comparing((QuizChoice c) -> {
                                String t = Optional.ofNullable(c.getChoiceText()).orElse("")
                                        .trim().toUpperCase();
                                return "O".equals(t) ? 0 : "X".equals(t) ? 1 : 2;
                            }).thenComparingLong(QuizChoice::getId))
                            .toList();
                }

                List<QuizChoice> answersChoiceList = qChoices.stream()
                        .filter(QuizChoice::isAnswer)
                        .toList();

                if (answersChoiceList.size() > 1) {
                    log.warn("[review] multiple correct choices. qid={}, answerIds={}", qid, answersChoiceList.stream().map(QuizChoice::getId).toList());
                }

                QuizChoice answerChoice = answersChoiceList.isEmpty() ? null : answersChoiceList.get(0);
                answerChoiceId = (answerChoice == null) ? null : answerChoice.getId();

                optionList = qChoices.stream()
                        .map(c -> SessionReviewResponseForm.Choice.builder()
                                .id(c.getId())
                                .text(c.getChoiceText())
                                .answer(c.isAnswer())
                                .build())
                        .toList();
            }

            boolean computedCorrect;

            if (isInitials) {
                String submitted = Optional.ofNullable(mySubmittedText).orElse("").trim();
                String expected  = Optional.ofNullable(expectedText).orElse("").trim();

                // 원하면 대소문자 무시/공백 정규화도 가능
                computedCorrect = !submitted.isEmpty() && submitted.equalsIgnoreCase(expected);
            } else {
                computedCorrect = (myChoiceId != null && answerChoiceId != null && myChoiceId.equals(answerChoiceId));
            }

            if (computedCorrect) correctCnt++;

            Boolean storedCorrect = (my != null) ? my.isCorrect() : null;
            if (storedCorrect != null && storedCorrect.booleanValue() != computedCorrect) {
                log.warn("[review mismatch] qid={}, storedCorrect={}, computedCorrect={}, myChoiceId={}, answerChoiceId={}, myText={}, expectedText={}",
                        qid, storedCorrect, computedCorrect, myChoiceId, answerChoiceId, mySubmittedText, expectedText);
            }

            items.add(SessionReviewResponseForm.Item.builder()
                    .quizQuestionId(qid)
                    .questionType(qt)
                    .questionText(q.getQuestionText())
                    .myChoiceId(isInitials ? null : myChoiceId)
                    .mySubmittedText(isInitials ? mySubmittedText : null)
                    .expectedText(expectedText)
                    .correct(computedCorrect)
                    .answerChoiceId(answerChoiceId)
                    .explanation(q.getExplanation())
                    .termId(Optional.ofNullable(q.getTerm()).map(t -> t.getId()).orElse(null))
                    .termTitle(Optional.ofNullable(q.getTerm()).map(t -> t.getTitle()).orElse(null))
                    .categoryId(Optional.ofNullable(q.getTermCategory()).map(c -> c.getId()).orElse(null))
                    .categoryName(Optional.ofNullable(q.getTermCategory()).map(c -> c.getName()).orElse(null))
                    .choices(optionList)
                    .build());
        }

        int total = qids.size();
        return SessionReviewResponseForm.builder()
                .sessionId(s.getId())
                .status(s.getSessionStatus())
                .total(total)
                .correct(correctCnt)
                .items(items)
                .build();
    }

    @Override
    public QuizTimelineResponseForm getTimeline(Long accountId, String q, QuizSetType part, int page, int size) {

        var pr = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(50, size)));
        var pageRes = timelineRepository.findTimelinePage(accountId, nullIfBlank(q), part, pr);
        var sessions = pageRes.getContent();

        // SET 제목: 현재/부모(SET)까지 포함해서 로딩
        Map<Long, String> setTitleById = loadSetTitleByIdIncludingAncestors(sessions);

        // 카테고리명: 현재/부모(TERM_CATEGORY)까지 포함해서 로딩
        Set<Long> catIds = new HashSet<>();
        for (QuizSession s : sessions) {
            if (s.getSourceType() == SessionSourceType.TERM_CATEGORY && s.getSourceId() != null) {
                catIds.add(s.getSourceId());
            }
            QuizSession p = s.getParentSession();
            if (p != null && p.getSourceType() == SessionSourceType.TERM_CATEGORY && p.getSourceId() != null) {
                catIds.add(p.getSourceId());
            }
        }

        Map<Long, String> catNameById = catIds.isEmpty()
                ? Map.of()
                : termCategoryRepository.findAllById(catIds).stream()
                .collect(Collectors.toMap(
                        c -> c.getId(),
                        c -> c.getName(),
                        (a, b) -> a
                ));

        // 정답/응답 수 집계(페이지에 있는 세션들)
        List<Long> ids = sessions.stream().map(QuizSession::getId).toList();

        Map<Long, Integer> correctBySession = new HashMap<>();
        Map<Long, Integer> answersBySession = new HashMap<>();

        if (!ids.isEmpty()) {
            for (Object[] row : timelineRepository.countCorrectBySessionIds(ids)) {
                correctBySession.put((Long) row[0], ((Number) row[1]).intValue());
            }
            for (Object[] row : timelineRepository.countAnswersBySessionIds(ids)) {
                answersBySession.put((Long) row[0], ((Number) row[1]).intValue());
            }
        }

        // 타임라인 타이틀 결정(부모 세션 기준 포함)
        // - 커스텀 타이틀 있으면 그걸 우선
        // - SET이면 setTitleById
        // - TERM_CATEGORY면 catNameById (없으면 "카테고리 퀴즈")
        // - WORDBOOK/기타는 기존 규칙
        java.util.function.Function<QuizSession, String> timelineTitleOf = (QuizSession base) -> {
            if (base == null) return "제목없음";

            if (base.getTitle() != null && !base.getTitle().isBlank()) {
                return base.getTitle().trim();
            }

            if (base.getSourceType() == SessionSourceType.SET) {
                return setTitleById.getOrDefault(base.getSourceId(), "세트#" + base.getSourceId());
            }

            if (base.getSourceType() == SessionSourceType.TERM_CATEGORY) {
                String name = (base.getSourceId() == null) ? null : catNameById.get(base.getSourceId());
                return (name != null && !name.isBlank()) ? name : "카테고리 퀴즈";
                // 원하면: return (name != null && !name.isBlank()) ? (name + " 퀴즈") : "카테고리 퀴즈";
            }

            if (base.getSourceType() == SessionSourceType.WORDBOOK) {
                return "단어장 퀴즈";
            }

            return "제목없음";
        };

        var items = sessions.stream().map(s -> {

            int total = Optional.ofNullable(s.getTotal())
                    .orElse(answersBySession.getOrDefault(s.getId(), 0));
            int correct = correctBySession.getOrDefault(s.getId(), 0);

            Instant when = (s.getSubmittedAt() != null) ? s.getSubmittedAt() : s.getStartedAt();

            QuizSetType pt = Optional.ofNullable(s.getPartType()).orElse(QuizSetType.CHOICE);

            // 카테고리(라벨칩) 표시용: 현재 세션이 TERM_CATEGORY일 때만
            String categoryName = null;
            if (s.getSourceType() == SessionSourceType.TERM_CATEGORY && s.getSourceId() != null) {
                categoryName = catNameById.get(s.getSourceId());
            }

            boolean isRetry = (s.getParentSession() != null);
            Long parentSessionId = isRetry ? s.getParentSession().getId() : null;

            QuizSession root = resolveRootSession(s);
            String title = timelineTitleOf.apply(root);

            return QuizTimelineResponseForm.Item.builder()
                    .id(s.getId())
                    .title(title)
                    .partType(pt.name())
                    .date(when)
                    .correct(correct)
                    .total(total)
                    .category(categoryName)
                    .isRetry(isRetry)
                    .parentSessionId(parentSessionId)
                    .sessionMode(s.getSessionMode())
                    .build();
        }).toList();

        long submitted = timelineRepository.countSubmitted(accountId);
        long retry = timelineRepository.countSubmittedRetry(accountId);
        long sumTotal = timelineRepository.sumTotalQuestionsOfSubmitted(accountId);
        long sumCorrect = timelineRepository.sumCorrectAnswersOfSubmitted(accountId);

        double accuracy = (sumTotal > 0) ? (sumCorrect * 100.0 / sumTotal) : 0.0;
        double retryRate = (submitted > 0) ? (retry * 100.0 / submitted) : 0.0;

        int accuracyRounded = (int) Math.round(accuracy);
        int retryRounded = (int) Math.round(retryRate);

        var summary = QuizTimelineResponseForm.Summary.builder()
                .totalSets(submitted)
                .accuracy(accuracyRounded)
                .retryRate(retryRounded)
                .build();

        // 최근 이력
        var recentSessions = timelineRepository.findRecentSessions(accountId, 5);
        Map<Long, String> recentSetTitleById = loadSetTitleByIdIncludingAncestors(recentSessions);

        Set<Long> recentCatIds = recentSessions.stream()
                .map(this::resolveTermCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> recentCatNameById = recentCatIds.isEmpty()
                ? Map.of()
                : termCategoryRepository.findAllById(recentCatIds).stream()
                .collect(Collectors.toMap(
                        c -> c.getId(),
                        c -> c.getName(),
                        (a, b) -> a
                ));

        var recent = recentSessions.stream()
                .map(s -> {
                    Instant when = (s.getSubmittedAt() != null) ? s.getSubmittedAt() : s.getStartedAt();
                    String whenStr = D.format(when);

                    String label;
                    if (s.getSourceType() == SessionSourceType.SET) {
                        label = recentSetTitleById.getOrDefault(s.getSourceId(), "세트#" + s.getSourceId());
                    } else if (s.getSourceType() == SessionSourceType.TERM_CATEGORY) {
                        label = recentCatNameById.getOrDefault(s.getSourceId(), "카테고리#" + s.getSourceId());
                    } else if (s.getSourceType() == SessionSourceType.WORDBOOK) {
                        label = "단어장#" + s.getSourceId();
                    } else {
                        label = "기타";
                    }

                    return QuizTimelineResponseForm.Recent.builder()
                            .label(label)
                            .when(whenStr)
                            .build();
                })
                .toList();

        return QuizTimelineResponseForm.builder()
                .summary(summary)
                .items(items)
                .recent(recent)
                .total(pageRes.getTotalElements())
                .build();
    }

    @Override
    public InitialsQuestionsResponse getDailyInitialsQuestions(Long sessionId, Long accountId) {
        var session = quizSessionRepository.findByIdAndAccount_Id(sessionId, accountId)
                .orElseThrow(() -> new SecurityException("세션이 없거나 권한이 없습니다."));

        if (session.getSeedMode() != SeedMode.DAILY)
            throw new IllegalArgumentException("DAILY only");

        if (session.getSourceType() != SessionSourceType.SET)
            throw new IllegalArgumentException("SET only");

        if (session.getPartType() != QuizSetType.INITIALS)
            throw new IllegalArgumentException("INITIALS only");

        Long setId = session.getSourceId();
        if (setId == null) throw new IllegalStateException("SET 세션인데 sourceId가 비었습니다.");

        // 1) 세션 스냅샷(id 리스트) 뽑기
        var qids = extractQuestionIds(session);

        // 2) 세트의 전체 INITIALS 문항 엔티티 조회
        var all = quizSetQueryService.findInitialsQuestionsBySetId(setId);

        // 3) 스냅샷 순서를 우선 보장
        var orderMap = new java.util.HashMap<Long, Integer>();
        for (int i = 0; i < qids.size(); i++) orderMap.put(qids.get(i), i);

        all.sort(java.util.Comparator.comparingInt(q ->
                orderMap.getOrDefault(q.getId(), Integer.MAX_VALUE)
        ));

        // 4) 스냅샷에 있는 것만 골라서 최대 3개
        List<InitialsQuestionsResponse.QuestionItem> picked = new ArrayList<>();
        int order = 1;
        for (var q : all) {
            if (!orderMap.containsKey(q.getId())) continue; // 스냅샷에 없는 건 제외
            picked.add(new InitialsQuestionsResponse.QuestionItem(
                    q.getId(),
                    order++,
                    Optional.ofNullable(q.getQuestionText()).orElse(""),
                    Optional.ofNullable(q.getExplanation()).orElse("")
            ));
            if (picked.size() >= 3) break; // 오늘의 초성은 3개 고정
        }

        return new InitialsQuestionsResponse(sessionId, picked.size(), picked);
    }

    private String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private Instant toInstant(Object ts) {
        if (ts == null) return null;
        if (ts instanceof Instant i) return i;
        if (ts instanceof java.time.LocalDateTime ldt) return ldt.atZone(KST).toInstant();
        if (ts instanceof java.time.ZonedDateTime zdt) return zdt.toInstant();
        return null;
    }

    /** 마지막 활동 60분 초과 시 EXPIRE 전환(상태 계산 및 필요 시 DB 전환) */
    @Transactional
    protected SessionStatus ensureCurrentStatus(QuizSession s) {
        SessionStatus current = s.getSessionStatus();
        if (current == SessionStatus.SUBMITTED) return current;
        if (current == SessionStatus.EXPIRED) return current;

        Instant last = Optional.ofNullable(s.getLastActivityAt())
                .orElse(Optional.ofNullable(s.getStartedAt()).orElse(Instant.now()));
        if (last.plus(EXPIRE_AFTER).isBefore(Instant.now())) {
            // 메모리 엔티티 + DB 둘 다 만료로
            s.expire(); // 엔티티 상태 반영
            quizSessionRepository.expireIfNotSubmitted(s.getId()); // DB 상태 반영
            return SessionStatus.EXPIRED;
        }
        return current;
    }

    /**
     * QuizSession에서 질문 ID 목록을 추출한다.
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

    private Long resolveTermCategoryId(QuizSession s) {
        if (s == null) return null;
        if (s.getSourceType() == SessionSourceType.TERM_CATEGORY) {
            return s.getSourceId();
        }
        return null; // SET/WORDBOOK 등은 여기서 카테고리로 단정하지 않음
    }

    private Map<Long, String> loadSetTitleByIdIncludingAncestors(List<QuizSession> sessions) {
        Set<Long> setIds = new HashSet<>();

        for (QuizSession s : sessions) {
            QuizSession cur = s;
            int guard = 0;
            while (cur != null && guard++ < 50) {
                if (cur.getSourceType() == SessionSourceType.SET && cur.getSourceId() != null) {
                    setIds.add(cur.getSourceId());
                }
                cur = cur.getParentSession();
            }
        }

        if (setIds.isEmpty()) return Map.of();

        return quizSetRepository.findAllById(setIds).stream()
                .collect(Collectors.toMap(QuizSet::getId, QuizSet::getTitle));
    }

    private Map<Long, String> loadCatNameByIdIncludingAncestors(List<QuizSession> sessions) {
        Set<Long> catIds = new HashSet<>();

        for (QuizSession s : sessions) {
            QuizSession cur = s;
            int guard = 0;
            while (cur != null && guard++ < 50) {
                if (cur.getSourceType() == SessionSourceType.TERM_CATEGORY && cur.getSourceId() != null) {
                    catIds.add(cur.getSourceId());
                }
                cur = cur.getParentSession();
            }
        }

        if (catIds.isEmpty()) return Map.of();

        return termCategoryRepository.findAllById(catIds).stream()
                .collect(Collectors.toMap(
                        c -> c.getId(),
                        c -> c.getName(),
                        (a, b) -> a
                ));
    }

    private String resolveTitle(QuizSession s,
                                Map<Long, String> setTitleById,
                                Map<Long, String> catNameById) {
        if (s.getTitle() != null && !s.getTitle().isBlank()) {
            return s.getTitle().trim();
        }

        if (s.getSourceType() == SessionSourceType.SET) {
            return setTitleById.getOrDefault(s.getSourceId(), "세트#" + s.getSourceId());
        }
        if (s.getSourceType() == SessionSourceType.TERM_CATEGORY) {
            String name = (s.getSourceId() == null) ? null : catNameById.get(s.getSourceId());
            return (name != null && !name.isBlank()) ? name : "카테고리 퀴즈";
        }
        if (s.getSourceType() == SessionSourceType.WORDBOOK) {
            return "단어장 퀴즈";
        }
        return "제목없음";
    }

    private static final char HANGUL_BASE = 0xAC00;
    private static final char HANGUL_LAST = 0xD7A3;
    private static final int  CHO_COUNT = 19;
    private static final int  JUNG_COUNT = 21;
    private static final int  JONG_COUNT = 28;
    private static final int  SYLLABLE_BLOCK = JUNG_COUNT * JONG_COUNT; // 588

    private static final String[] CHO = {
            "ㄱ","ㄲ","ㄴ","ㄷ","ㄸ","ㄹ","ㅁ","ㅂ","ㅃ","ㅅ","ㅆ","ㅇ","ㅈ","ㅉ","ㅊ","ㅋ","ㅌ","ㅍ","ㅎ"
    };

    private String toInitialsHint(String answerText) {
        if (answerText == null || answerText.isBlank()) return null;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < answerText.length(); i++) {
            char ch = answerText.charAt(i);

            if (Character.isWhitespace(ch)) {
                sb.append(' ');
                continue;
            }

            if (ch >= HANGUL_BASE && ch <= HANGUL_LAST) {
                int syllableIndex = ch - HANGUL_BASE;
                int choIndex = syllableIndex / SYLLABLE_BLOCK;
                if (choIndex >= 0 && choIndex < CHO_COUNT) sb.append(CHO[choIndex]);
                else sb.append(ch);
            } else {
                // 한글이 아니면 그대로(영문/숫자/기호)
                sb.append(ch);
            }
        }

        String out = sb.toString().trim();
        return out.isEmpty() ? null : out;
    }

    private QuizSession resolveRootSession(QuizSession s) {
        if (s == null) return null;
        QuizSession cur = s;
        int guard = 0;

        while (cur.getParentSession() != null && guard++ < 50) {
            cur = cur.getParentSession();
        }
        return cur;
    }
}
