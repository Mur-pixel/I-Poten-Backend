package com.cygnus.ipoten.quiz_session.service;

import com.cygnus.ipoten.quiz_question.entity.QuizChoice;
import com.cygnus.ipoten.quiz_question.entity.QuizQuestion;
import com.cygnus.ipoten.quiz_question.repository.QuizChoiceRepository;
import com.cygnus.ipoten.quiz_question.repository.QuizQuestionRepository;
import com.cygnus.ipoten.quiz_session.entity.SessionAnswer;
import com.cygnus.ipoten.quiz_session.entity.QuizSession;
import com.cygnus.ipoten.quiz.entity.enums.QuizSetType;
import com.cygnus.ipoten.quiz.entity.enums.SessionStatus;
import com.cygnus.ipoten.quiz_analytics.controller.response_form.TimelineResponseForm;
import com.cygnus.ipoten.quiz_session.controller.response_form.SessionItemsPageResponseForm;
import com.cygnus.ipoten.quiz_session.controller.response_form.SessionListResponseForm;
import com.cygnus.ipoten.quiz_session.controller.response_form.SessionReviewResponseForm;
import com.cygnus.ipoten.quiz_session.controller.response_form.SessionSummaryResponseForm;
import com.cygnus.ipoten.quiz_session.repository.SessionAnswerRepository;
import com.cygnus.ipoten.quiz_session.repository.QuizSessionRepository;
import com.cygnus.ipoten.quiz_analytics.repository.QuizSessionTimelineRepository;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizSessionQueryServiceImpl implements QuizSessionQueryService {

    private final QuizSessionRepository quizSessionRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizChoiceRepository quizChoiceRepository;
    private final SessionAnswerRepository sessionAnswerRepository;
    private final QuizSessionTimelineRepository timelineRepository;

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
            Long sessionId, Long accountId, int offset, int limit, boolean includeAnswers) {

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

        List<Long> allIds = Optional.ofNullable(quizsession.getSnapshotQuestionIds()).orElse(List.of());
        int total = allIds.size();
        int from = Math.max(0, Math.min(offset, total));
        int to   = Math.max(from, Math.min(from + limit, total));
        List<Long> pageIds = allIds.subList(from, to);

        Map<Long, QuizQuestion> byId = quizQuestionRepository.findAllById(pageIds)
                .stream().collect(Collectors.toMap(QuizQuestion::getId, q -> q));

        List<QuizChoice> allChoices = quizChoiceRepository.findByQuizQuestionIdIn(pageIds);
        Map<Long, List<QuizChoice>> choicesByQ = allChoices.stream()
                .collect(Collectors.groupingBy(c -> c.getQuizQuestion().getId()));

        // 제출 완료이거나(includeAnswers=true)면 정답/해설 공개
        boolean canReveal = (effective == SessionStatus.SUBMITTED) || includeAnswers;

        List<SessionItemsPageResponseForm.Item> items = new ArrayList<>();
        for (Long qid : pageIds) {
            QuizQuestion question = byId.get(qid);
            if (question == null) continue;

            List<QuizChoice> qChoices = choicesByQ.getOrDefault(qid, List.of());
            QuizChoice answerChoice = canReveal
                    ? qChoices.stream().filter(QuizChoice::isAnswer).findFirst().orElse(null)
                    : null;

            List<SessionItemsPageResponseForm.Choice> choiceList = qChoices.stream()
                    .map(c -> SessionItemsPageResponseForm.Choice.builder()
                            .id(c.getId())
                            .text(c.getChoiceText())
                            .isAnswer(canReveal ? c.isAnswer() : null)
                            .build())
                    .toList();

            items.add(SessionItemsPageResponseForm.Item.builder()
                    .questionId(qid)
                    .questionText(question.getQuestionText())
                    .correctChoiceId(answerChoice != null ? answerChoice.getId() : null)
                    .explanation(canReveal ? question.getExplanation() : null)
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

        // 매핑
        List<SessionListResponseForm.Item> items = new ArrayList<>(page.getNumberOfElements());
        for (QuizSession s : page.getContent()) {
            // 총 문항 수: total 저장값 우선, 없으면 스냅샷 크기
            Integer total = Optional.ofNullable(s.getTotal())
                    .orElse(Optional.ofNullable(s.getSnapshotQuestionIds()).map(List::size).orElse(0));

            // 제출된 세션만 정답 수/점수 계산
            Integer correct = null;
            Double score = null;
            Integer scorePercent = null;
            if (s.getSessionStatus() == SessionStatus.SUBMITTED) {
                List<SessionAnswer> ans = sessionAnswerRepository.findByQuizSession_Id(s.getId());
                int c = (int) ans.stream().filter(SessionAnswer::isCorrect).count();
                correct = c;
                if (total != null && total > 0) {
                    score = c * 100.0 / total;              // 소수 가능
                    scorePercent = (int) Math.round(score);  // 퍼센트 정수
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
                    .scorePercent(scorePercent) // 추가 필드
                    .title(Optional.ofNullable(s.getQuizSet()).map(qs -> qs.getTitle()).orElse(null))
                    .build());
        }

        // 응답
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

        List<SessionAnswer> answers = sessionAnswerRepository.findByQuizSession_Id(sessionId);
        Map<Long, SessionAnswer> ansByQ = answers.stream()
                .collect(Collectors.toMap(a -> a.getQuizQuestion().getId(), a -> a, (a, b) -> a, LinkedHashMap::new));

        int correctCnt = 0;
        List<SessionReviewResponseForm.Item> items = new ArrayList<>();

        for (Long qid : qids) {
            QuizQuestion q = qById.get(qid);
            if (q == null) continue;

            List<QuizChoice> qChoices = choicesByQ.getOrDefault(qid, List.of());
            QuizChoice answerChoice = qChoices.stream().filter(QuizChoice::isAnswer).findFirst().orElse(null);
            SessionAnswer my = ansByQ.get(qid);

            Long myChoiceId = (my == null || my.getQuizChoice() == null) ? null : my.getQuizChoice().getId();
            boolean myCorrect = (my != null && my.isCorrect());
            if (myCorrect) correctCnt++;

            List<SessionReviewResponseForm.Choice> optionList = qChoices.stream()
                    .map(c -> SessionReviewResponseForm.Choice.builder()
                            .id(c.getId())
                            .text(c.getChoiceText())
                            .answer(c.isAnswer())
                            .build())
                    .toList();

            items.add(SessionReviewResponseForm.Item.builder()
                    .quizQuestionId(qid)
                    .questionType(q.getQuestionType())
                    .questionText(q.getQuestionText())
                    .myChoiceId(myChoiceId)
                    .correct(myCorrect)
                    .answerChoiceId(answerChoice == null ? null : answerChoice.getId())
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
                .total(total)
                .correct(correctCnt)
                .items(items)
                .build();
    }

    @Override
    public TimelineResponseForm getTimeline(Long accountId, String q, QuizSetType part, int page, int size) {

        var pr = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(50, size)));
        var pageRes = timelineRepository.findTimelinePage(accountId, nullIfBlank(q), part, pr);
        var sessions = pageRes.getContent();

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

        var items = sessions.stream().map(s -> {
            var qs = s.getQuizSet();
            int total = Optional.ofNullable(s.getTotal())
                    .orElse(answersBySession.getOrDefault(s.getId(), 0));
            int correct = correctBySession.getOrDefault(s.getId(), 0);

            Instant when = (s.getSubmittedAt() != null) ? s.getSubmittedAt() : s.getStartedAt();

            return TimelineResponseForm.Item.builder()
                    .id(s.getId())
                    .title(qs != null ? qs.getTitle() : "제목없음")
                    .partType(
                            qs != null && qs.getQuizSetType() != null
                                    ? qs.getQuizSetType().name()
                                    : QuizSetType.CHOICE.name()
                    )
                    .date(when)
                    .correct(correct)
                    .total(total)
                    .category(qs != null && qs.getTermCategory() != null ? qs.getTermCategory().getName() : null)
                    .build();
        }).toList();

        long submitted = timelineRepository.countSubmitted(accountId);
        long retry = timelineRepository.countSubmittedRetry(accountId);
        long sumTotal = timelineRepository.sumTotalQuestionsOfSubmitted(accountId);
        long sumCorrect = timelineRepository.sumCorrectAnswersOfSubmitted(accountId);

        double accuracy = (sumTotal > 0) ? (sumCorrect * 100.0 / sumTotal) : 0.0;
        double retryRate = (submitted > 0) ? (retry * 100.0 / submitted) : 0.0;

        int accuracyRounded = (int) Math.round(accuracy);
        int retryRounded    = (int) Math.round(retryRate);

        var summary = TimelineResponseForm.Summary.builder()
                .totalSets(submitted)
                .accuracy(accuracyRounded)
                .retryRate(retryRounded)
                .build();

        var recentRaw = timelineRepository.findRecentRaw(accountId, 10);
        var recent = recentRaw.stream()
                .map(r -> {
                    String whenStr;
                    Object ts = r[1];
                    if (ts instanceof java.time.Instant i) {
                        whenStr = D.format(i);
                    } else if (ts instanceof java.time.LocalDateTime ldt) {
                        whenStr = D.format(ldt.atZone(KST).toInstant());
                    } else if (ts instanceof java.time.ZonedDateTime zdt) {
                        whenStr = D.format(zdt.toInstant());
                    } else {
                        whenStr = String.valueOf(ts);
                    }
                    return TimelineResponseForm.Recent.builder()
                            .label(String.valueOf(r[0]))
                            .when(whenStr)
                            .build();
                })
                .toList();

        return TimelineResponseForm.builder()
                .summary(summary)
                .items(items)
                .recent(recent)
                .total(pageRes.getTotalElements())
                .build();
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

        Instant last = Optional.ofNullable(s.getLastActivityAt()).orElse(Instant.EPOCH);
        if (last.plus(EXPIRE_AFTER).isBefore(Instant.now())) {
            // 메모리 엔티티 + DB 둘 다 만료로
            s.expire(); // 엔티티 상태 반영
            quizSessionRepository.expireIfNotSubmitted(s.getId()); // DB 상태 반영
            return SessionStatus.EXPIRED;
        }
        return current;
    }
}
