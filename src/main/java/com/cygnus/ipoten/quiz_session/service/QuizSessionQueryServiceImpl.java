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
import com.cygnus.ipoten.quiz_session.entity.enums.SessionMode;
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

import static com.cygnus.ipoten.quiz_question.util.HangulInitials.toInitialsHint;

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

    private static final Duration EXPIRE_AFTER = Duration.ofHours(3);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(KST);

    /** 상태/권한/만료 전환 포함 단건 요약 */
    @Override
    @Transactional
    public SessionSummaryResponseForm getSummary(Long sessionId, Long accountId) {
        QuizSession quizsession =
                quizSessionRepository.findByIdAndAccount_IdAndDeletedAtIsNull(sessionId, accountId)
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
                quizSessionRepository.findByIdAndAccount_IdAndDeletedAtIsNull(sessionId, accountId)
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
        List<QuizChoice> allChoices = quizChoiceRepository.findByQuizQuestionIdInOrderByQuizQuestionIdAscIdAsc(pageIds);
        Map<Long, List<QuizChoice>> choicesByQ = allChoices.stream()
                .collect(Collectors.groupingBy(c -> c.getQuizQuestion().getId()));

        // 4) 노출 정책
        boolean canRevealAnswers = (effective == SessionStatus.SUBMITTED) && includeAnswers;
        boolean canRevealExplanation = (effective == SessionStatus.SUBMITTED);

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

        long baseSeed = Optional.ofNullable(quizsession.getSeedValue()).orElse(0L);

        for (Long qid : pageIds) {
            QuizQuestion question = byId.get(qid);
            if (question == null) continue;

            QuestionType qt = question.getQuestionType();
            boolean isInitials = (qt == QuestionType.INITIALS);

            List<SessionItemsPageResponseForm.Choice> choiceList = List.of();
            Long correctChoiceId = null;

            String initialsHint = null;
            String expectedText = null;

            if (isInitials) {
                String ans = expectedByQid.get(qid);

                initialsHint = toInitialsHint(ans);

                if (canRevealAnswers) {
                    expectedText = ans;
                    if (expectedText == null) {
                        log.warn("[sessionItems] initials expectedText missing. qid={}", qid);
                    }
                }

            } else {
                List<QuizChoice> qChoices = choicesByQ.getOrDefault(qid, List.of());

                // start/page/review 모두 동일하게 만들기 위해:
                // 1) 입력 안정화(id 정렬) -> 2) 문제 seed로 셔플 -> 3) qid 기반 targetIdx로 정답 위치 고정
                qChoices = normalizeBaseOrder(qChoices);

                qChoices = reorderDeterministic(
                        qt,
                        qChoices,
                        baseSeed,
                        qid
                );

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
    @Transactional
    public SessionListResponseForm listMySessions(Long accountId, int limit, String statusFilter) {
        int pageSize = Math.max(1, Math.min(100, limit));
        PageRequest pr = PageRequest.of(0, pageSize, Sort.by(Sort.Direction.DESC, "startedAt"));

        SessionStatus status = null;
        if (statusFilter != null && !statusFilter.isBlank()) {
            try {
                status = SessionStatus.valueOf(statusFilter.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignore) { /* ALL */ }
        }

        var page = (status == null)
                ? quizSessionRepository.findByAccount_IdAndDeletedAtIsNull(accountId, pr)
                : quizSessionRepository.findByAccount_IdAndSessionStatusAndDeletedAtIsNull(accountId, status, pr);

        var sessions = page.getContent();

        Map<Long, String> setTitleById = loadSetTitleByIdIncludingAncestors(sessions);
        Map<Long, String> catNameById  = loadCatNameByIdIncludingAncestors(sessions);

        List<SessionListResponseForm.Item> items = new ArrayList<>(page.getNumberOfElements());
        for (QuizSession s : sessions) {

            SessionStatus effective = ensureCurrentStatus(s);

            Integer total = Optional.ofNullable(s.getTotal())
                    .orElse(Optional.ofNullable(s.getSnapshotQuestionIds()).map(List::size).orElse(0));

            Integer correct = null;
            Double score = null;
            Integer scorePercent = null;

            if (effective == SessionStatus.SUBMITTED) {
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
                    .status(effective)
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
                quizSessionRepository.findByIdAndAccount_IdAndDeletedAtIsNull(sessionId, accountId)
                        .orElseThrow(() -> new SecurityException("세션이 없거나 권한이 없습니다."));

        if (s.getSessionStatus() != SessionStatus.SUBMITTED) {
            throw new IllegalStateException("제출 완료된 세션만 리뷰할 수 있습니다.");
        }

        List<Long> qids = Optional.ofNullable(s.getSnapshotQuestionIds()).orElse(List.of());
        if (qids.isEmpty()) {
            return SessionReviewResponseForm.builder()
                    .sessionId(s.getId())
                    .total(0)
                    .correct(0)
                    .items(List.of())
                    .build();
        }

        Map<Long, QuizQuestion> qById = quizQuestionRepository.findAllById(qids)
                .stream().collect(Collectors.toMap(QuizQuestion::getId, q -> q));

        List<QuizChoice> allChoices = quizChoiceRepository.findByQuizQuestionIdInOrderByQuizQuestionIdAscIdAsc(qids);
        Map<Long, List<QuizChoice>> choicesByQ = allChoices.stream()
                .collect(Collectors.groupingBy(c -> c.getQuizQuestion().getId()));

        List<Long> initialsQids = qById.values().stream()
                .filter(qq -> qq.getQuestionType() == QuestionType.INITIALS)
                .map(QuizQuestion::getId)
                .toList();

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

        long baseSeed = Optional.ofNullable(s.getSeedValue()).orElse(0L);

        for (Long qid : qids) {
            QuizQuestion q = qById.get(qid);
            if (q == null) continue;

            QuizSessionAnswer my = ansByQ.get(qid);

            QuestionType qt = q.getQuestionType();
            boolean isInitials = (qt == QuestionType.INITIALS);

            Long myChoiceId = (my != null) ? my.getSubmittedChoiceId() : null;
            String mySubmittedText = (my != null) ? my.getSubmittedText() : null;

            String expectedText = isInitials ? expectedTextByQid.get(qid) : null;

            List<SessionReviewResponseForm.Choice> optionList = List.of();
            Long answerChoiceId = null;

            if (!isInitials) {
                List<QuizChoice> qChoices = choicesByQ.getOrDefault(qid, List.of());
                qChoices = normalizeBaseOrder(qChoices);

                // getSessionItems와 동일한 순서 규칙
                qChoices = reorderDeterministic(qt, qChoices, baseSeed, qid);

                List<QuizChoice> answersChoiceList = qChoices.stream()
                        .filter(QuizChoice::isAnswer)
                        .toList();

                if (answersChoiceList.size() > 1) {
                    log.warn("[review] multiple correct choices. qid={}, answerIds={}",
                            qid, answersChoiceList.stream().map(QuizChoice::getId).toList());
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

        Map<Long, String> setTitleById = loadSetTitleByIdIncludingAncestors(sessions);

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

            String categoryName = null;
            if (s.getSourceType() == SessionSourceType.TERM_CATEGORY && s.getSourceId() != null) {
                categoryName = catNameById.get(s.getSourceId());
            }

            boolean isRetry = (s.getParentSession() != null);
            Long parentSessionId = isRetry ? s.getParentSession().getId() : null;

            QuizSession root = resolveRootSession(s);

            String title = timelineTitleOf.apply(s);
            String originTitle = Optional.ofNullable(timelineTitleOf.apply(root)).orElse(title);

            QuizTimelineResponseForm.RetryKind retryKind = null;
            if (isRetry) {
                SessionMode mode = Optional.ofNullable(s.getSessionMode()).orElse(SessionMode.FULL);
                retryKind = (mode == SessionMode.WRONG_ONLY)
                        ? QuizTimelineResponseForm.RetryKind.WRONG_ONLY
                        : QuizTimelineResponseForm.RetryKind.RETRY_ALL;
            }

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
                    .originTitle(originTitle)
                    .retryKind(retryKind)
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
        var session = quizSessionRepository.findByIdAndAccount_IdAndDeletedAtIsNull(sessionId, accountId)
                .orElseThrow(() -> new SecurityException("세션이 없거나 권한이 없습니다."));

        if (session.getSeedMode() != SeedMode.DAILY)
            throw new IllegalArgumentException("DAILY only");

        if (session.getSourceType() != SessionSourceType.SET)
            throw new IllegalArgumentException("SET only");

        if (session.getPartType() != QuizSetType.INITIALS)
            throw new IllegalArgumentException("INITIALS only");

        Long setId = session.getSourceId();
        if (setId == null) throw new IllegalStateException("SET 세션인데 sourceId가 비었습니다.");

        var qids = extractQuestionIds(session);
        var all = quizSetQueryService.findInitialsQuestionsBySetId(setId);

        var orderMap = new java.util.HashMap<Long, Integer>();
        for (int i = 0; i < qids.size(); i++) orderMap.put(qids.get(i), i);

        all.sort(java.util.Comparator.comparingInt(q ->
                orderMap.getOrDefault(q.getId(), Integer.MAX_VALUE)
        ));

        List<InitialsQuestionsResponse.QuestionItem> picked = new ArrayList<>();
        int order = 1;
        for (var q : all) {
            if (!orderMap.containsKey(q.getId())) continue;
            picked.add(new InitialsQuestionsResponse.QuestionItem(
                    q.getId(),
                    order++,
                    Optional.ofNullable(q.getQuestionText()).orElse(""),
                    Optional.ofNullable(q.getExplanation()).orElse("")
            ));
            if (picked.size() >= 3) break;
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

    /** 마지막 활동 3시간 초과 시 EXPIRE 전환(상태 계산 및 필요 시 DB 전환) */
    @Transactional
    protected SessionStatus ensureCurrentStatus(QuizSession s) {
        SessionStatus current = s.getSessionStatus();
        if (current == SessionStatus.SUBMITTED) return current;
        if (current == SessionStatus.EXPIRED) return current;

        Instant last = Optional.ofNullable(s.getLastActivityAt())
                .orElse(Optional.ofNullable(s.getStartedAt()).orElse(Instant.now()));

        if (last.plus(EXPIRE_AFTER).isBefore(Instant.now())) {
            int updated = quizSessionRepository.expireIfInProgress(s.getId());
            if (updated > 0) {
                s.expire();
                return SessionStatus.EXPIRED;
            }
            return quizSessionRepository.findByIdAndDeletedAtIsNull(s.getId())
                    .map(QuizSession::getSessionStatus).orElse(current);
        }
        return current;
    }

    private List<Long> extractQuestionIds(QuizSession session) {
        try {
            List<Long> ids = session.getSnapshotQuestionIds();
            if (ids != null && !ids.isEmpty()) return ids;
        } catch (Exception ignore) {}

        try {
            var m = session.getClass().getMethod("getQuestionIds");
            @SuppressWarnings("unchecked")
            List<Long> ids = (List<Long>) m.invoke(session);
            if (ids != null && !ids.isEmpty()) return ids;
        } catch (NoSuchMethodException ignore) {
        } catch (Exception e) {
            log.warn("[initials] getQuestionIds() reflection failed", e);
        }

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
        return null;
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

    private QuizSession resolveRootSession(QuizSession s) {
        if (s == null) return null;
        QuizSession cur = s;
        int guard = 0;

        while (cur.getParentSession() != null && guard++ < 50) {
            cur = cur.getParentSession();
        }
        return cur;
    }

    private static long mixSeed(long a, long b) {
        long x = a ^ (b + 0x9E3779B97F4A7C15L);
        x = (x ^ (x >>> 30)) * 0xBF58476D1CE4E5B9L;
        x = (x ^ (x >>> 27)) * 0x94D049BB133111EBL;
        x = x ^ (x >>> 31);
        return x;
    }

    private static List<QuizChoice> normalizeBaseOrder(List<QuizChoice> choices) {
        if (choices == null) return List.of();
        return choices.stream()
                .sorted(Comparator.comparingLong(QuizChoice::getId))
                .toList();
    }

    /**
     * start/page/review 모두 동일하게 만드는 결정적 reorder:
     * - OX: 항상 O -> X 고정
     * - CHOICE: (1) base(id 정렬) (2) questionSeed로 셔플 (3) qid 기반 targetIdx에 정답을 고정
     */
    private List<QuizChoice> reorderDeterministic(
            QuestionType questionType,
            List<QuizChoice> choices,
            long sessionSeed,
            long qid
    ) {
        if (choices == null || choices.size() <= 1) return choices;

        // OX는 항상 O -> X 고정
        if (questionType == QuestionType.OX) {
            return choices.stream()
                    .sorted(Comparator.comparing((QuizChoice c) -> {
                        String t = Optional.ofNullable(c.getChoiceText()).orElse("")
                                .trim().toUpperCase();
                        return "O".equals(t) ? 0 : "X".equals(t) ? 1 : 2;
                    }).thenComparingLong(QuizChoice::getId))
                    .toList();
        }

        List<QuizChoice> base = normalizeBaseOrder(choices);

        // 문제 단위 셔플(= start와 동일하게 만들 seed 규칙)
        long questionSeed = mixSeed(sessionSeed, qid);
        List<QuizChoice> shuffled = new ArrayList<>(base);
        Collections.shuffle(shuffled, new Random(questionSeed));

        int optionCount = shuffled.size();
        if (optionCount < 2) return shuffled;

        // 핵심: targetIdx를 "qid 기반"으로 결정 → 호출 횟수/페이지 크기/재조회와 무관
        int targetIdx = computeTargetAnswerIndex(sessionSeed, qid, optionCount);

        // 정답 찾기
        int currentIdx = -1;
        for (int i = 0; i < shuffled.size(); i++) {
            if (Boolean.TRUE.equals(shuffled.get(i).isAnswer())) {
                currentIdx = i;
                break;
            }
        }
        if (currentIdx < 0) return shuffled;

        QuizChoice correct = shuffled.remove(currentIdx);
        int safeTarget = Math.max(0, Math.min(targetIdx, shuffled.size()));
        shuffled.add(safeTarget, correct);

        return shuffled;
    }

    /**
     * seed/qid로 정답 보기 위치(0~N-1) 결정
     * seed가 같으면 항상 동일, seed가 바뀌면 위치도 바뀜
     */
    private static int computeTargetAnswerIndex(long sessionSeed, long qid, int optionCount) {
        long h = mixSeed(sessionSeed, qid);
        return Math.floorMod((int) h, optionCount);
    }
}
