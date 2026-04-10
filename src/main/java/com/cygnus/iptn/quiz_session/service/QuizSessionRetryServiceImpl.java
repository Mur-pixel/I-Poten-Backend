package com.cygnus.iptn.quiz_session.service;

import com.cygnus.iptn.account.entity.Account;
import com.cygnus.iptn.account.repository.AccountRepository;
import com.cygnus.iptn.quiz_question.entity.QuizChoice;
import com.cygnus.iptn.quiz_question.entity.QuizQuestion;
import com.cygnus.iptn.quiz_question.entity.enums.QuestionType;
import com.cygnus.iptn.quiz_question.repository.QuizChoiceRepository;
import com.cygnus.iptn.quiz_question.repository.QuizQuestionRepository;
import com.cygnus.iptn.quiz_question.repository.QuizTextAnswerRepository;
import com.cygnus.iptn.quiz_session.entity.QuizSession;
import com.cygnus.iptn.quiz_session.entity.enums.SeedMode;
import com.cygnus.iptn.quiz_session.entity.enums.SessionMode;
import com.cygnus.iptn.quiz_session.entity.enums.SessionSourceType;
import com.cygnus.iptn.quiz_session.entity.enums.SessionStatus;
import com.cygnus.iptn.quiz_session.repository.QuizSessionRepository;
import com.cygnus.iptn.quiz_session_answer.repository.QuizSessionAnswerRepository;
import com.cygnus.iptn.quiz_session.service.response.StartQuizSessionResponse;
import com.cygnus.iptn.quiz_set.entity.enums.QuizSetType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static com.cygnus.iptn.quiz_question.util.HangulInitials.toInitialsHint;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizSessionRetryServiceImpl implements QuizSessionRetryService {

    private final QuizSessionRepository quizSessionRepository;
    private final QuizSessionAnswerRepository quizSessionAnswerRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final AccountRepository accountRepository;
    private final QuizChoiceRepository quizChoiceRepository;
    private final QuizTextAnswerRepository quizTextAnswerRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public StartQuizSessionResponse startRetryWrongOnly(Long parentSessionId, Long accountId) {
        // 1) 권한/상태 검증 : 내 세션인지 확인
        QuizSession parent = quizSessionRepository.findByIdAndAccount_IdAndDeletedAtIsNull(parentSessionId, accountId)
                .orElseThrow(() -> new SecurityException("본인 세션이 아니거나 존재하지 않습니다."));

        // 제출 완료된 세션만 허용
        if (parent.getSessionStatus() != SessionStatus.SUBMITTED) {
            throw new IllegalStateException("제출 완료된 세션에서만 오답 재도전이 가능합니다.");
        }

        // 2) 원본 세션에서 오답 질문만 추출
        List<Long> wrongQuestionId = quizSessionAnswerRepository.findWrongQuestionIds(parentSessionId);
        if (wrongQuestionId == null || wrongQuestionId.isEmpty()) {
            throw new IllegalStateException("오답이 없어 재시작할 문제가 없습니다.");
        }

        // 중복 제거 및 리스트화
        List<Long> wrongQuestionIds = new ArrayList<>(new LinkedHashSet<>(wrongQuestionId));

        // 3) 새 세션용 seed (매 재도전마다 달라지게 함)
        long sessionSeed = mixSeed(System.nanoTime(), mixSeed(parentSessionId, accountId));

        // 문항 순서 셔플
        Collections.shuffle(wrongQuestionIds, new Random(sessionSeed));

        // 4) 오답 질문 로드 (partType 결정을 위해 선행)
        List<QuizQuestion> questions = quizQuestionRepository.findAllById(wrongQuestionIds);
        Map<Long, QuizQuestion> qMap = questions.stream()
                .collect(Collectors.toMap(QuizQuestion::getId, q -> q, (a, b) -> a, LinkedHashMap::new));

        if (qMap.size() != wrongQuestionIds.size()) {
            Set<Long> missing = new LinkedHashSet<>(wrongQuestionIds);
            missing.removeAll(qMap.keySet());
            throw new IllegalStateException("일부 질문을 찾지 못했습니다(삭제/비활성 가능): " + missing);
        }

        // 5) 부모 세션의 partType 우선, 없으면 질문 타입 기반 유추
        QuizSetType resolvedPartType = resolvePartTypeFromParentOrQuestions(parent, questions);

        // 6) source 메타: 부모 세션의 source를 그대로 계승(타임라인/필터/카테고리 매핑 유지)
        SessionSourceType sourceType = requireParentSourceType(parent);
        Long sourceId = requireParentSourceId(parent);
        String sourceKey = requireParentSourceKey(parent);

        // attemptNo는 sourceKey 기준으로 증가
        int attemptNo = quizSessionRepository.findMaxAttemptNoBySourceKey(accountId, sourceKey) + 1;

        // 7) 새 세션 생성(부모-자식 연결 + 스냅샷 + seed 저장)
        Account account = accountRepository.getReferenceById(accountId);

        QuizSession child = new QuizSession();
        child.beginFromSourceWithParent(
                account,
                parent,
                sourceType,
                sourceId,
                sourceKey,
                resolvedPartType,
                SessionMode.WRONG_ONLY,
                attemptNo,
                wrongQuestionIds.size(),
                toJson(wrongQuestionIds),
                SeedMode.FIXED,
                sessionSeed
        );

        String inheritedTitle = inheritTitleFromAncestor(parent);
        if (inheritedTitle != null) {
            child.changeTitle(inheritedTitle);
        }

        quizSessionRepository.save(child);

        // 8) 보기/초성 정답 로드 + response items 구성
        var allChoices = quizChoiceRepository.findByQuizQuestionIdIn(wrongQuestionIds);
        Map<Long, List<QuizChoice>> byQ = allChoices.stream()
                .collect(Collectors.groupingBy(c -> c.getQuizQuestion().getId()));

        List<Long> initialsQids = wrongQuestionIds.stream()
                .filter(id -> qMap.get(id) != null && qMap.get(id).getQuestionType() == QuestionType.INITIALS)
                .toList();

        Map<Long, String> answerTextByQid =
                quizTextAnswerRepository.findByQuizQuestion_IdIn(initialsQids).stream()
                        .collect(Collectors.toMap(
                                a -> a.getQuizQuestion().getId(),
                                a -> a.getAnswerText(),
                                (a, b) -> a
                        ));

        List<StartQuizSessionResponse.Item> items = wrongQuestionIds.stream()
                .map(qid -> {
                    QuizQuestion q = qMap.get(qid);

                    if (q.getQuestionType() == QuestionType.INITIALS) {
                        String answerText = Optional.ofNullable(answerTextByQid.get(qid))
                                .map(String::trim)
                                .orElse(null);

                        if (answerText == null || answerText.isBlank()) {
                            throw new IllegalStateException("초성 문제 정답이 등록되지 않았습니다: " + qid);
                        }

                        String initialsHint = toInitialsHint(answerText);

                        return new StartQuizSessionResponse.Item(
                                q.getId(),
                                q.getQuestionType(),
                                q.getQuestionText(),
                                initialsHint,
                                List.of()
                        );
                    }

                    List<QuizChoice> choices = new ArrayList<>(byQ.getOrDefault(qid, List.of()));
                    int optionCount = choices.size();

                    if (optionCount < 2) {
                        var options = choices.stream()
                                .map(c -> new StartQuizSessionResponse.Option(c.getId(), c.getChoiceText()))
                                .toList();

                        return new StartQuizSessionResponse.Item(
                                q.getId(),
                                q.getQuestionType(),
                                q.getQuestionText(),
                                null,
                                options
                        );
                    }

                    List<QuizChoice> ordered = reorderDeterministic(
                            q.getQuestionType(),
                            choices,
                            sessionSeed,
                            qid
                    );

                    var options = ordered.stream()
                            .map(c -> new StartQuizSessionResponse.Option(c.getId(), c.getChoiceText()))
                            .toList();

                    return new StartQuizSessionResponse.Item(
                            q.getId(),
                            q.getQuestionType(),
                            q.getQuestionText(),
                            null,
                            options
                    );
                })
                .toList();

        Long setIdForResponse = (sourceType == SessionSourceType.SET) ? sourceId : null;
        return new StartQuizSessionResponse(child.getId(), setIdForResponse, wrongQuestionIds, items);
    }

    @Override
    @Transactional
    public StartQuizSessionResponse startRetryAll(Long sessionId, Long accountId) {
        // 1) 권한/상태 검증
        QuizSession parent = quizSessionRepository.findByIdAndAccount_IdAndDeletedAtIsNull(sessionId, accountId)
                .orElseThrow(() -> new SecurityException("본인 세션이 아니거나 존재하지 않습니다."));

        if (parent.getSessionStatus() != SessionStatus.SUBMITTED) {
            throw new IllegalStateException("제출 완료된 세션에서만 재도전이 가능합니다.");
        }

        // 2) 부모 세션의 출제 스냅샷(전체 문항) 기반으로 재도전
        List<Long> snapshot = parent.getSnapshotQuestionIds();
        if (snapshot == null || snapshot.isEmpty()) {
            throw new IllegalStateException("세션 스냅샷이 없어 재도전할 문제가 없습니다.");
        }

        List<Long> questionIds = new ArrayList<>(new LinkedHashSet<>(snapshot));

        // 3) 새 세션용 seed (매 도전마다 달라지게)
        long sessionSeed = mixSeed(System.nanoTime(), mixSeed(sessionId, accountId));

        // 문항 순서 셔플
        Collections.shuffle(questionIds, new Random(sessionSeed));

        // 4) 질문 로드 (셔플된 순서 유지하려면 id 기반 접근)
        List<QuizQuestion> questions = quizQuestionRepository.findAllById(questionIds);
        Map<Long, QuizQuestion> qMap = questions.stream()
                .collect(Collectors.toMap(QuizQuestion::getId, q -> q, (a, b) -> a, LinkedHashMap::new));

        if (qMap.size() != questionIds.size()) {
            Set<Long> missing = new LinkedHashSet<>(questionIds);
            missing.removeAll(qMap.keySet());
            throw new IllegalStateException("일부 질문을 찾지 못했습니다(삭제/비활성 가능): " + missing);
        }

        // 5) partType 결정(부모 우선, 없으면 질문 타입 기반)
        QuizSetType resolvedPartType = resolvePartTypeFromParentOrQuestions(parent, questions);

        // 6) source 메타 계승
        SessionSourceType sourceType = requireParentSourceType(parent);
        Long sourceId = requireParentSourceId(parent);
        String sourceKey = requireParentSourceKey(parent);

        int attemptNo = quizSessionRepository.findMaxAttemptNoBySourceKey(accountId, sourceKey) + 1;

        // 7) 새 세션 생성(부모-자식 연결 + 스냅샷 + seed 저장)
        Account account = accountRepository.getReferenceById(accountId);

        QuizSession child = new QuizSession();
        child.beginFromSourceWithParent(
                account,
                parent,
                sourceType,
                sourceId,
                sourceKey,
                resolvedPartType,
                SessionMode.FULL,
                attemptNo,
                questionIds.size(),
                toJson(questionIds),
                SeedMode.FIXED,
                sessionSeed
        );

        String inheritedTitle = inheritTitleFromAncestor(parent);
        if (inheritedTitle != null) {
            child.changeTitle(inheritedTitle);
        }

        quizSessionRepository.save(child);

        // 8) 보기 로드
        var allChoices = quizChoiceRepository.findByQuizQuestionIdIn(questionIds);
        Map<Long, List<QuizChoice>> byQ = allChoices.stream()
                .collect(Collectors.groupingBy(c -> c.getQuizQuestion().getId()));

        List<Long> initialsQids = questionIds.stream()
                .filter(id -> qMap.get(id) != null && qMap.get(id).getQuestionType() == QuestionType.INITIALS)
                .toList();

        Map<Long, String> answerTextByQid =
                quizTextAnswerRepository.findByQuizQuestion_IdIn(initialsQids).stream()
                        .collect(Collectors.toMap(
                                a -> a.getQuizQuestion().getId(),
                                a -> a.getAnswerText(),
                                (a, b) -> a
                        ));

        // 9) 보기 배치(정답 위치 균등 분배 + seed 기반 섞기)
        List<StartQuizSessionResponse.Item> items = questionIds.stream()
                .map(qid -> {
                    QuizQuestion q = qMap.get(qid);

                    if (q.getQuestionType() == QuestionType.INITIALS) {

                        String answerText = Optional.ofNullable(answerTextByQid.get(qid))
                                .map(String::trim)
                                .orElse(null);

                        if (answerText == null || answerText.isBlank()) {
                            throw new IllegalStateException("초성 문제 정답이 등록되지 않았습니다: " + qid);
                        }

                        String initialsHint = toInitialsHint(answerText);

                        return new StartQuizSessionResponse.Item(
                                q.getId(),
                                q.getQuestionType(),
                                q.getQuestionText(),
                                initialsHint,
                                List.of()
                        );
                    }

                    List<QuizChoice> choices = new ArrayList<>(byQ.getOrDefault(qid, List.of()));
                    int optionCount = choices.size();

                    if (optionCount < 2) {
                        var options = choices.stream()
                                .map(c -> new StartQuizSessionResponse.Option(c.getId(), c.getChoiceText()))
                                .toList();
                        return new StartQuizSessionResponse.Item(
                                q.getId(),
                                q.getQuestionType(),
                                q.getQuestionText(),
                                null,
                                options
                        );
                    }

                    List<QuizChoice> ordered = reorderDeterministic(
                            q.getQuestionType(),
                            choices,
                            sessionSeed,
                            qid
                    );

                    var options = ordered.stream()
                            .map(c -> new StartQuizSessionResponse.Option(c.getId(), c.getChoiceText()))
                            .toList();

                    return new StartQuizSessionResponse.Item(
                            q.getId(),
                            q.getQuestionType(),
                            q.getQuestionText(),
                            null,
                            options
                    );
                })
                .toList();

        Long setIdForResponse = (sourceType == SessionSourceType.SET) ? sourceId : null;

        return new StartQuizSessionResponse(child.getId(), setIdForResponse, questionIds, items);
    }

    @Override
    @Transactional
    public StartQuizSessionResponse startQuickRetry(Long accountId) {

        final int MAX_SESSIONS_TO_SCAN = 20;
        final int MAX_QUESTIONS = 40;

        // 1) 7일 우선
        List<QuizSession> sessions7 = findSubmittedSessions(accountId, 7, MAX_SESSIONS_TO_SCAN);
        List<Long> wrongIds7 = collectWrongQuestionIdsDedupLatestFirst(sessions7, MAX_QUESTIONS);

        List<QuizSession> baseSessions;
        List<Long> questionIds;
        int appliedDays;

        // 2) 7일 내 오답이 있는 경우
        if (!wrongIds7.isEmpty()) {
            baseSessions = sessions7;
            questionIds = wrongIds7;
            appliedDays = 7;
        } else {
            // 3) 7일 내 오답이 없으면 30일 폴백
            List<QuizSession> sessions30 = findSubmittedSessions(accountId, 30, MAX_SESSIONS_TO_SCAN);
            List<Long> wrongIds30 = collectWrongQuestionIdsDedupLatestFirst(sessions30, MAX_QUESTIONS);

            if (sessions30.isEmpty()) {
                throw new IllegalStateException("최근 30일 이내 제출한 퀴즈 이력이 없습니다.");
            }

            if (wrongIds30.isEmpty()) {
                throw new IllegalStateException("최근 30일 이내 오답 문제가 없습니다.");
            }

            baseSessions = sessions30;
            questionIds = wrongIds30;
            appliedDays = 30;
        }

        // 메타데이터/부모 세션은 "가장 최신 SUBMITTED 세션"을 anchor로 사용
        QuizSession anchor = baseSessions.get(0);
        
        // 4) 새 세션용 SEED
        long sessionSeed = mixSeed(System.nanoTime(), mixSeed(anchor.getId(), accountId));
        
        // 문항 순서 셔플
        Collections.shuffle(questionIds, new Random(sessionSeed));
        
        // 5) 질문 로드
        List<QuizQuestion> questions = quizQuestionRepository.findAllById(questionIds);
        Map<Long, QuizQuestion> qMap = questions.stream()
                .collect(Collectors.toMap(QuizQuestion::getId, q -> q, (a, b) -> a, LinkedHashMap::new));

        if (qMap.size() != questionIds.size()) {
            Set<Long> missing = new LinkedHashSet<>(questionIds);
            missing.removeAll(qMap.keySet());
            throw new IllegalStateException("일부 질문을 찾지 못했습니다(삭제/비활성 가능): " + missing);
        }

        // 6) partType 결정(부모 우선)
        QuizSetType resolvedPartType = resolvePartTypeFromParentOrQuestions(anchor, questions);
        
        // 7) source 메타는 anchor에서 계승
        SessionSourceType sourceType = requireParentSourceType(anchor);
        Long sourceId = requireParentSourceId(anchor);
        String sourceKey = requireParentSourceKey(anchor);

        int attemptNo = quizSessionRepository.findMaxAttemptNoBySourceKey(accountId, sourceKey) + 1;
        
        // 8) 새 세션 생성 (부모 = anchor)
        Account account = accountRepository.getReferenceById(accountId);

        QuizSession child = new QuizSession();
        child.beginFromSourceWithParent(
                account,
                anchor,
                sourceType,
                sourceId,
                sourceKey,
                resolvedPartType,
                SessionMode.WRONG_ONLY,
                attemptNo,
                questionIds.size(),
                toJson(questionIds),
                SeedMode.FIXED,
                sessionSeed
        );

        // 타이틀 결정
        String inheritedTitle = inheritTitleFromAncestor(anchor);
        if (inheritedTitle == null) inheritedTitle = "퀴즈";
        child.changeTitle("[빠른 재도전] 최근 " + appliedDays + "일 오답만 - " + inheritedTitle);

        quizSessionRepository.save(child);

        // 9) 보기/초성 정답 로드 + response items 구성
        var allChoices = quizChoiceRepository.findByQuizQuestionIdIn(questionIds);
        Map<Long, List<QuizChoice>> byQ = allChoices.stream()
                .collect(Collectors.groupingBy(c -> c.getQuizQuestion().getId()));

        List<Long> initialsQids = questionIds.stream()
                .filter(id -> qMap.get(id) != null && qMap.get(id).getQuestionType() == QuestionType.INITIALS)
                .toList();

        Map<Long, String> answerTextByQid =
                quizTextAnswerRepository.findByQuizQuestion_IdIn(initialsQids).stream()
                        .collect(Collectors.toMap(
                                a -> a.getQuizQuestion().getId(),
                                a -> a.getAnswerText(),
                                (a, b) -> a
                        ));

        List<StartQuizSessionResponse.Item> items = questionIds.stream()
                .map(qid -> {
                    QuizQuestion q = qMap.get(qid);

                    if (q.getQuestionType() == QuestionType.INITIALS) {
                        String answerText = Optional.ofNullable(answerTextByQid.get(qid))
                                .map(String::trim)
                                .orElse(null);

                        if (answerText == null || answerText.isBlank()) {
                            throw new IllegalStateException("초성 문제 정답이 등록되지 않았습니다: " + qid);
                        }

                        String initialsHint = toInitialsHint(answerText);

                        return new StartQuizSessionResponse.Item(
                                q.getId(),
                                q.getQuestionType(),
                                q.getQuestionText(),
                                initialsHint,
                                List.of()
                        );
                    }

                    List<QuizChoice> choices = new ArrayList<>(byQ.getOrDefault(qid, List.of()));
                    int optionCount = choices.size();

                    if (optionCount < 2) {
                        var options = choices.stream()
                                .map(c -> new StartQuizSessionResponse.Option(c.getId(), c.getChoiceText()))
                                .toList();
                        return new StartQuizSessionResponse.Item(
                                q.getId(),
                                q.getQuestionType(),
                                q.getQuestionText(),
                                null,
                                options
                        );
                    }

                    List<QuizChoice> ordered = reorderDeterministic(
                            q.getQuestionType(),
                            choices,
                            sessionSeed,
                            qid
                    );

                    var options = ordered.stream()
                            .map(c -> new StartQuizSessionResponse.Option(c.getId(), c.getChoiceText()))
                            .toList();

                    return new StartQuizSessionResponse.Item(
                            q.getId(),
                            q.getQuestionType(),
                            q.getQuestionText(),
                            null,
                            options
                    );
                })
                .toList();

        Long setIdForResponse = (sourceType == SessionSourceType.SET) ? sourceId : null;
        return new StartQuizSessionResponse(child.getId(), setIdForResponse, questionIds, items);
    }

    @Override
    @Transactional
    public StartQuizSessionResponse startQuickRetry(Long accountId, int days) {

        final int MAX_SESSIONS_TO_SCAN = 20;
        final int MAX_QUESTIONS = 40;

        int appliedDays = normalizeDays(days);

        Instant after = Instant.now().minus(appliedDays, ChronoUnit.DAYS);
        log.info("[quickRetry] reqDays={}, appliedDays={}, after={}", days, appliedDays, after);

        List<QuizSession> sessions = findSubmittedSessions(accountId, appliedDays, MAX_SESSIONS_TO_SCAN);

        if (sessions.isEmpty()) {
            throw new IllegalStateException("최근 " + appliedDays + "일 이내 제출한 퀴즈 이력이 없습니다.");
        }

        List<Long> questionIds = collectWrongQuestionIdsDedupLatestFirst(sessions, MAX_QUESTIONS);
        if (questionIds.isEmpty()) {
            throw new IllegalStateException("최근 " + appliedDays + "일 이내 오답 문제가 없습니다.");
        }

        return buildQuickRetrySession(accountId, sessions, questionIds, appliedDays);
    }

    /** 부모 세션(QuizSession)에 저장된 partType을 우선 보거나, 없으면 질문 타입으로 유추 */
    private QuizSetType resolvePartTypeFromParentOrQuestions(QuizSession parent, List<QuizQuestion> questions) {
        if (parent.getPartType() != null) {
            return parent.getPartType();
        }

        Set<QuestionType> types = questions.stream()
                .map(QuizQuestion::getQuestionType)
                .collect(Collectors.toSet());

        if (types.isEmpty()) return QuizSetType.MIX;

        if (types.size() == 1) {
            return mapToPartType(types.iterator().next());
        }
        return QuizSetType.MIX;
    }

    /** QuestionType -> QuizSetType 매핑 */
    private QuizSetType mapToPartType(QuestionType qt) {
        try {
            return QuizSetType.valueOf(qt.name());
        } catch (IllegalArgumentException e) {
            return QuizSetType.MIX;
        }
    }

    private SessionSourceType requireParentSourceType(QuizSession parent) {
        SessionSourceType st = parent.getSourceType();
        if (st == null) throw new IllegalStateException("부모 세션 sourceType이 비었습니다. parentSessionId=" + parent.getId());
        return st;
    }

    private Long requireParentSourceId(QuizSession parent) {
        Long sid = parent.getSourceId();
        if (sid == null) throw new IllegalStateException("부모 세션 sourceId가 비었습니다. parentSessionId=" + parent.getId());
        return sid;
    }

    private String requireParentSourceKey(QuizSession parent) {
        String sk = parent.getSourceKey();
        if (sk == null || sk.isBlank())
            throw new IllegalStateException("부모 세션 sourceKey가 비었습니다. parentSessionId=" + parent.getId());
        return sk;
    }

    /** 간단 시드 믹싱(결정성 유지) */
    private static long mixSeed(long a, long b) {
        long x = a ^ (b + 0x9E3779B97F4A7C15L);
        x = (x ^ (x >>> 30)) * 0xBF58476D1CE4E5B9L;
        x = (x ^ (x >>> 27)) * 0x94D049BB133111EBL;
        x = x ^ (x >>> 31);
        return x;
    }

    private String toJson(List<Long> ids) {
        try {
            return objectMapper.writeValueAsString(ids);
        } catch (Exception e) {
            throw new IllegalStateException("questionIds 직렬화 실패", e);
        }
    }

    /**
     * start/play/review 모두 동일하게 만들기 위한 "결정적 reorder".
     * - OX: 항상 O -> X 고정
     * - 그 외: (1) base(id 정렬) (2) (seed,qid)로 셔플 (3) targetIdx(=f(seed,qid,optionCount))에 정답 고정
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

        // 1) base order 안정화 (DB 반환 순서 흔들림 방지)
        List<QuizChoice> base = normalizeBaseOrder(choices);

        // 2) 문제 단위 셔플
        long questionSeed = mixSeed(sessionSeed, qid);
        List<QuizChoice> shuffled = new ArrayList<>(base);
        Collections.shuffle(shuffled, new Random(questionSeed));

        int optionCount = shuffled.size();
        if (optionCount < 2) return shuffled;

        // 3) qid 기반 targetIdx
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

        QuizChoice answer = shuffled.remove(currentIdx);
        int safeTarget = Math.max(0, Math.min(targetIdx, shuffled.size()));
        shuffled.add(safeTarget, answer);

        return shuffled;
    }

    private static List<QuizChoice> normalizeBaseOrder(List<QuizChoice> choices) {
        return choices.stream()
                .sorted(Comparator.comparingLong(QuizChoice::getId))
                .toList();
    }

    private static int computeTargetAnswerIndex(long sessionSeed, long qid, int optionCount) {
        long h = mixSeed(sessionSeed, qid);
        return Math.floorMod((int) h, optionCount);
    }

    /** 부모 체인에서 title이 있는 첫 title을 상속 */
    private String inheritTitleFromAncestor(QuizSession parent) {
        QuizSession cur = parent;
        int guard = 0;
        while (cur != null && guard++ < 50) {
            String t = cur.getTitle();
            if (t != null && !t.isBlank()) return t.trim();
            cur = cur.getParentSession();
        }
        return null;
    }

    /** 기간 내 SUBMITTED 세션 최근 N개 조회 */
    private List<QuizSession> findSubmittedSessions(Long accountId, int days, int limit) {
        Instant after = Instant.now().minus(days, ChronoUnit.DAYS);
        return quizSessionRepository
                .findByAccount_IdAndSessionStatusAndDeletedAtIsNullAndSubmittedAtAfterOrderBySubmittedAtDesc(
                        accountId,
                        SessionStatus.SUBMITTED,
                        after,
                        PageRequest.of(0, limit)
                )
                .getContent();
    }

    /** 여러 세션에서 오답 questionId를 최신 세션 우선으로 모으고 중복 제거 */
    private List<Long> collectWrongQuestionIdsDedupLatestFirst(List<QuizSession> sessions, int maxQuestions) {
        if (sessions == null || sessions.isEmpty()) return List.of();

        List<Long> sessionIds = sessions.stream().map(QuizSession::getId).toList();

        // 최신 세션 우선 정렬된 rows
        var rows = quizSessionAnswerRepository.findWrongRowsOrdered(sessionIds);

        LinkedHashSet<Long> set = new LinkedHashSet<>();
        for (var r : rows) {
            set.add(r.getQuestionId());
            if (maxQuestions > 0 && set.size() >= maxQuestions) break;
        }
        return new ArrayList<>(set);
    }

    private StartQuizSessionResponse buildQuickRetrySession(
            Long accountId,
            List<QuizSession> baseSessions,
            List<Long> questionIds,
            int appliedDays
    ) {
        QuizSession anchor = baseSessions.get(0);

        long sessionSeed = mixSeed(System.nanoTime(), mixSeed(anchor.getId(), accountId));

        // 원본 리스트 사이드이펙트를 방지하기 위해 복사
        List<Long> shuffledIds = new ArrayList<>(questionIds);
        Collections.shuffle(shuffledIds, new Random(sessionSeed));

        List<QuizQuestion> questions = quizQuestionRepository.findAllById(shuffledIds);
        Map<Long, QuizQuestion> qMap = questions.stream()
                .collect(Collectors.toMap(QuizQuestion::getId, q -> q, (a, b) -> a, LinkedHashMap::new));

        if (qMap.size() != shuffledIds.size()) {
            Set<Long> missing = new LinkedHashSet<>(shuffledIds);
            missing.removeAll(qMap.keySet());
            throw new IllegalStateException("일부 질문을 찾지 못했습니다(삭제/비활성 가능): " + missing);
        }

        QuizSetType resolvedPartType = resolvePartTypeFromParentOrQuestions(anchor, questions);

        SessionSourceType sourceType = requireParentSourceType(anchor);
        Long sourceId = requireParentSourceId(anchor);
        String sourceKey = requireParentSourceKey(anchor);

        int attemptNo = quizSessionRepository.findMaxAttemptNoBySourceKey(accountId, sourceKey) + 1;

        Account account = accountRepository.getReferenceById(accountId);

        QuizSession child = new QuizSession();
        child.beginFromSourceWithParent(
                account,
                anchor,
                sourceType,
                sourceId,
                sourceKey,
                resolvedPartType,
                SessionMode.WRONG_ONLY,
                attemptNo,
                shuffledIds.size(),
                toJson(shuffledIds),
                SeedMode.FIXED,
                sessionSeed
        );

        String inheritedTitle = inheritTitleFromAncestor(anchor);
        if (inheritedTitle == null) inheritedTitle = "퀴즈";
        child.changeTitle("[빠른 재도전] 최근 " + appliedDays + "일 오답만 - " + inheritedTitle);

        quizSessionRepository.save(child);

        var allChoices = quizChoiceRepository.findByQuizQuestionIdIn(shuffledIds);
        Map<Long, List<QuizChoice>> byQ = allChoices.stream()
                .collect(Collectors.groupingBy(c -> c.getQuizQuestion().getId()));

        List<Long> initialsQids = shuffledIds.stream()
                .filter(id -> qMap.get(id) != null && qMap.get(id).getQuestionType() == QuestionType.INITIALS)
                .toList();

        Map<Long, String> answerTextByQid =
                quizTextAnswerRepository.findByQuizQuestion_IdIn(initialsQids).stream()
                        .collect(Collectors.toMap(
                                a -> a.getQuizQuestion().getId(),
                                a -> a.getAnswerText(),
                                (a, b) -> a
                        ));

        List<StartQuizSessionResponse.Item> items = shuffledIds.stream()
                .map(qid -> {
                    QuizQuestion q = qMap.get(qid);

                    if (q.getQuestionType() == QuestionType.INITIALS) {
                        String answerText = Optional.ofNullable(answerTextByQid.get(qid))
                                .map(String::trim)
                                .orElse(null);

                        if (answerText == null || answerText.isBlank()) {
                            throw new IllegalStateException("초성 문제 정답이 등록되지 않았습니다: " + qid);
                        }

                        String initialsHint = toInitialsHint(answerText);

                        return new StartQuizSessionResponse.Item(
                                q.getId(),
                                q.getQuestionType(),
                                q.getQuestionText(),
                                initialsHint,
                                List.of()
                        );
                    }

                    List<QuizChoice> choices = new ArrayList<>(byQ.getOrDefault(qid, List.of()));
                    int optionCount = choices.size();

                    if (optionCount < 2) {
                        var options = choices.stream()
                                .map(c -> new StartQuizSessionResponse.Option(c.getId(), c.getChoiceText()))
                                .toList();
                        return new StartQuizSessionResponse.Item(
                                q.getId(),
                                q.getQuestionType(),
                                q.getQuestionText(),
                                null,
                                options
                        );
                    }

                    List<QuizChoice> ordered = reorderDeterministic(
                            q.getQuestionType(),
                            choices,
                            sessionSeed,
                            qid
                    );

                    var options = ordered.stream()
                            .map(c -> new StartQuizSessionResponse.Option(c.getId(), c.getChoiceText()))
                            .toList();

                    return new StartQuizSessionResponse.Item(
                            q.getId(),
                            q.getQuestionType(),
                            q.getQuestionText(),
                            null,
                            options
                    );
                })
                .toList();

        Long setIdForResponse = (sourceType == SessionSourceType.SET) ? sourceId : null;
        return new StartQuizSessionResponse(child.getId(), setIdForResponse, shuffledIds, items);
    }

    /** days 파라미터 보정 */
    private int normalizeDays(int days) {
        if (days == 7 || days == 30) return days;
        throw new IllegalArgumentException("days는 7 또는 30만 허용: " + days);
    }
}
