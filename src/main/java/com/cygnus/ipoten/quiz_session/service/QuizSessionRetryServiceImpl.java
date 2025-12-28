package com.cygnus.ipoten.quiz_session.service;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.account.repository.AccountRepository;
import com.cygnus.ipoten.quiz_question.entity.QuizChoice;
import com.cygnus.ipoten.quiz_question.entity.QuizQuestion;
import com.cygnus.ipoten.quiz_question.entity.enums.QuestionType;
import com.cygnus.ipoten.quiz_question.repository.QuizChoiceRepository;
import com.cygnus.ipoten.quiz_question.repository.QuizQuestionRepository;
import com.cygnus.ipoten.quiz_question.repository.QuizTextAnswerRepository;
import com.cygnus.ipoten.quiz_session.entity.QuizSession;
import com.cygnus.ipoten.quiz_session.entity.enums.SeedMode;
import com.cygnus.ipoten.quiz_session.entity.enums.SessionMode;
import com.cygnus.ipoten.quiz_session.entity.enums.SessionSourceType;
import com.cygnus.ipoten.quiz_session.entity.enums.SessionStatus;
import com.cygnus.ipoten.quiz_session.repository.QuizSessionRepository;
import com.cygnus.ipoten.quiz_session_answer.repository.QuizSessionAnswerRepository;
import com.cygnus.ipoten.quiz_session.service.response.StartQuizSessionResponse;
import com.cygnus.ipoten.quiz_session_generator.service.util.AnswerIndexPlanner;
import com.cygnus.ipoten.quiz_set.entity.enums.QuizSetType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

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
        QuizSession parent = quizSessionRepository.findByIdAndAccount_Id(parentSessionId, accountId)
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

        // 8) 미리보기용 아이템 구성(보기 로딩, 순서 유지)
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
                                (a,b) -> a
                        ));

        long baseSeed = sessionSeed;
        Map<Integer, AnswerIndexPlanner> planners = new HashMap<>();

        List<StartQuizSessionResponse.Item> items = wrongQuestionIds.stream()
                .map(qid -> {
                    QuizQuestion q = qMap.get(qid);

                    if (q.getQuestionType() == QuestionType.INITIALS) {
                        String answerText = Optional.ofNullable(answerTextByQid.get(qid))
                                .map(String::trim)
                                .orElse(null);

                        if (answerText == null && answerText.isBlank()) {
                            throw new IllegalStateException("초성 문제 정답이 등록되지 않았습니다: " + qid);
                        }

                        return new StartQuizSessionResponse.Item(
                                q.getId(),
                                q.getQuestionType(),
                                q.getQuestionText(),
                                q.getExplanation(),
                                null,
                                List.of(),
                                answerText
                        );
                    }

                    List<QuizChoice> choices = new ArrayList<>(byQ.getOrDefault(qid, List.of()));
                    int optionCount = choices.size();

                    if (optionCount < 2) {
                        var options = choices.stream()
                                .map(c -> new StartQuizSessionResponse.Option(c.getId(), c.getChoiceText()))
                                .toList();
                        return new StartQuizSessionResponse.Item(
                                q.getId(), q.getQuestionType(), q.getQuestionText(), null, null, options, null
                        );
                    }

                    AnswerIndexPlanner planner = planners.computeIfAbsent(
                            optionCount,
                            oc -> new AnswerIndexPlanner(oc, mixSeed(baseSeed, oc))
                    );

                    List<QuizChoice> ordered = reorderWithBalancedAnswerIndex(
                            q.getQuestionType(),
                            choices,
                            planner,
                            mixSeed(baseSeed, qid)
                    );

                    var options = ordered.stream()
                            .map(c -> new StartQuizSessionResponse.Option(c.getId(), c.getChoiceText()))
                            .toList();

                    return new StartQuizSessionResponse.Item(
                            q.getId(),
                            q.getQuestionType(),
                            q.getQuestionText(),
                            null,
                            null,
                            options,
                            null
                    );
                })
                .toList();

        // response의 setId는 "SET 세션일 때만" 의미가 있으니, 아닐 경우 null
        Long setIdForResponse = (sourceType == SessionSourceType.SET) ? sourceId : null;

        return new StartQuizSessionResponse(child.getId(), setIdForResponse, wrongQuestionIds, items);
    }

    @Override
    @Transactional
    public StartQuizSessionResponse startRetryAll(Long sessionId, Long accountId) {
        // 1) 권한/상태 검증
        QuizSession parent = quizSessionRepository.findByIdAndAccount_Id(sessionId, accountId)
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
        long baseSeed = sessionSeed;
        Map<Integer, AnswerIndexPlanner> planners = new HashMap<>();

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

                        return new StartQuizSessionResponse.Item(
                                q.getId(),
                                q.getQuestionType(),
                                q.getQuestionText(),
                                q.getExplanation(),
                                null,
                                List.of(),
                                answerText
                        );
                    }

                    List<QuizChoice> choices = new ArrayList<>(byQ.getOrDefault(qid, List.of()));
                    int optionCount = choices.size();

                    if (optionCount < 2) {
                        var options = choices.stream()
                                .map(c -> new StartQuizSessionResponse.Option(c.getId(), c.getChoiceText()))
                                .toList();
                        return new StartQuizSessionResponse.Item(
                                q.getId(), q.getQuestionType(), q.getQuestionText(), null, null, options, null
                        );
                    }

                    AnswerIndexPlanner planner = planners.computeIfAbsent(
                            optionCount,
                            oc -> new AnswerIndexPlanner(oc, mixSeed(baseSeed, oc))
                    );

                    List<QuizChoice> ordered = reorderWithBalancedAnswerIndex(
                            q.getQuestionType(),
                            choices,
                            planner,
                            mixSeed(baseSeed, qid)
                    );

                    var options = ordered.stream()
                            .map(c -> new StartQuizSessionResponse.Option(c.getId(), c.getChoiceText()))
                            .toList();

                    return new StartQuizSessionResponse.Item(
                            q.getId(), q.getQuestionType(), q.getQuestionText(), null, null, options, null
                    );
                })
                .toList();

        Long setIdForResponse = (sourceType == SessionSourceType.SET) ? sourceId : null;

        return new StartQuizSessionResponse(child.getId(), setIdForResponse, questionIds, items);
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
     * 정답 위치를 AnswerIndexPlanner로 균등 분배하면서 보기들을 재배치한다.
     * - 각 문항마다 정답이 들어갈 인덱스를 planner에서 하나 꺼내서 사용
     * - 나머지 오답들은 seed 기반으로 섞어서 채운다.
     */
    private List<QuizChoice> reorderWithBalancedAnswerIndex(
            QuestionType questionType,
            List<QuizChoice> choices,
            AnswerIndexPlanner planner,
            long seed
    ) {
        if (choices == null || choices.size() <= 1) {
            return choices;
        }

        if (questionType == QuestionType.OX) {
            return choices.stream()
                    .sorted(Comparator.comparing((QuizChoice c) -> {
                        String t = Optional.ofNullable(c.getChoiceText()).orElse("")
                                .trim().toUpperCase();
                        return "O".equals(t) ? 0 : "X".equals(t) ? 1 : 2;
                    }).thenComparingLong(QuizChoice::getId))
                    .toList();
        }

        int size = choices.size();

        // 1) 현재 리스트에서 정답 위치 찾기 (없으면 0번)
        int currentAnswerIndex = 0;
        for (int i = 0; i < size; i++) {
            if (choices.get(i).isAnswer()) {
                currentAnswerIndex = i;
                break;
            }
        }

        QuizChoice answer = choices.get(currentAnswerIndex);

        // 2) planner로 목표 인덱스 선정
        int targetIndex = planner.nextIndex();
        if (targetIndex < 0 || targetIndex >= size) {
            targetIndex = Math.floorMod(targetIndex, size);
        }

        // 3) 오답 리스트 만들기 + seed 셔플
        List<QuizChoice> distractors = new ArrayList<>(choices);
        distractors.remove(currentAnswerIndex);
        Collections.shuffle(distractors, new Random(seed));

        // 4) targetIndex에 정답 배치 후 나머지 채우기
        List<QuizChoice> ordered = new ArrayList<>(Collections.nCopies(size, (QuizChoice) null));
        ordered.set(targetIndex, answer);

        int di = 0;
        for (int i = 0; i < size; i++) {
            if (i == targetIndex) continue;
            ordered.set(i, distractors.get(di++));
        }

        return ordered;
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
}
