package com.cygnus.ipoten.quiz_session_answer.service;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.account.repository.AccountRepository;
import com.cygnus.ipoten.quiz_question.entity.QuizChoice;
import com.cygnus.ipoten.quiz_question.entity.QuizQuestion;
import com.cygnus.ipoten.quiz_question.entity.QuizTextAnswer;
import com.cygnus.ipoten.quiz_question.entity.enums.QuestionType;
import com.cygnus.ipoten.quiz_question.repository.QuizChoiceRepository;
import com.cygnus.ipoten.quiz_question.repository.QuizQuestionRepository;
import com.cygnus.ipoten.quiz_question.repository.QuizTextAnswerRepository;
import com.cygnus.ipoten.quiz_session.entity.QuizSession;
import com.cygnus.ipoten.quiz_session.entity.enums.SeedMode;
import com.cygnus.ipoten.quiz_session.entity.enums.SessionMode;
import com.cygnus.ipoten.quiz_session.entity.enums.SessionSourceType;
import com.cygnus.ipoten.quiz_session.repository.QuizSessionRepository;
import com.cygnus.ipoten.quiz_session.service.response.StartQuizSessionResponse;
import com.cygnus.ipoten.quiz_session_answer.controller.request_form.SubmitQuizSessionRequestForm;
import com.cygnus.ipoten.quiz_session.controller.response_form.SubmitQuizSessionResponseForm;
import com.cygnus.ipoten.quiz_session_answer.entity.QuizSessionAnswer;
import com.cygnus.ipoten.quiz_session_answer.repository.QuizSessionAnswerRepository;
import com.cygnus.ipoten.quiz_session_generator.service.util.AnswerIndexPlanner;
import com.cygnus.ipoten.quiz_session_scope.value_objects.SessionSource;
import com.cygnus.ipoten.quiz_set.entity.enums.QuizSetType;
import com.cygnus.ipoten.quiz_set.entity.QuizSet;
import com.cygnus.ipoten.quiz_set.repository.QuizSetRepository;
import com.cygnus.ipoten.quiz_wrongnote.service.QuizWrongNoteService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizSessionAnswerServiceImpl implements QuizSessionAnswerService {

    private final AccountRepository accountRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final QuizSetRepository quizSetRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizChoiceRepository quizChoiceRepository;
    private final QuizTextAnswerRepository quizTextAnswerRepository;
    private final QuizSessionAnswerRepository quizSessionAnswerRepository;
    private final QuizWrongNoteService quizWrongNoteService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public StartQuizSessionResponse startFromQuizSet(
            Long accountId,
            Long quizSetId,
            List<Long> questionIds,
            SeedMode seedMode,
            Long fixedSeed,
            String customTitle
    ) {
        if (accountId == null) throw new IllegalArgumentException("accountId required");
        if (quizSetId == null) throw new IllegalArgumentException("quizSetId required");
        if (questionIds == null || questionIds.isEmpty()) throw new IllegalArgumentException("questionIds required");

        Account account = accountRepository.getReferenceById(accountId);

        // 응답용(세트 존재 확인)
        QuizSet quizSet = quizSetRepository.findById(quizSetId)
                .orElseThrow(() -> new IllegalArgumentException("quizSet을 찾을 수 없습니다. id=" + quizSetId));

        long resolvedSeed = resolveSeed(seedMode, accountId, fixedSeed);

        // sourceKey: SessionSource 규칙("set:{id}")과 통일
        SessionSource src = SessionSource.of(SessionSourceType.SET, quizSetId, null);
        String sourceKey = src.getSourceKey();

        int attemptNo = quizSessionRepository.findMaxAttemptNoBySourceKey(accountId, sourceKey) + 1;

        // 질문 중복 제거 + 순서 보존
        List<Long> uniqIds = new ArrayList<>(new LinkedHashSet<>(questionIds));

        // 질문 로드/검증
        List<QuizQuestion> questions = quizQuestionRepository.findAllById(uniqIds);
        Map<Long, QuizQuestion> qMap = questions.stream()
                .collect(Collectors.toMap(QuizQuestion::getId, q -> q, (a, b) -> a, LinkedHashMap::new));

        if (qMap.size() != uniqIds.size()) {
            Set<Long> missing = new LinkedHashSet<>(uniqIds);
            missing.removeAll(qMap.keySet());
            throw new IllegalStateException("일부 질문을 찾지 못했습니다(삭제/비활성 가능): " + missing);
        }

        QuizSetType partType = resolvePartTypeFromQuestions(questions);

        QuizSession session = new QuizSession();
        session.beginFromSource(
                account,
                SessionSourceType.SET,
                quizSetId,
                sourceKey,
                partType,
                SessionMode.FULL,
                attemptNo,
                uniqIds.size(),
                toJson(uniqIds),
                seedMode,
                resolvedSeed
        );

        if (customTitle != null && !customTitle.isBlank()) {
            session.changeTitle(customTitle.trim());
        }

        quizSessionRepository.save(session);

        // 보기 배치 조회
        var allChoices = quizChoiceRepository.findByQuizQuestionIdInOrderByQuizQuestionIdAscIdAsc(uniqIds);
        Map<Long, List<QuizChoice>> byQ = allChoices.stream()
                .collect(Collectors.groupingBy(c -> c.getQuizQuestion().getId()));

        long baseSeed = resolvedSeed;
        Map<Integer, AnswerIndexPlanner> planners = new HashMap<>();


        List<Long> initialsQids = uniqIds.stream()
                .filter(id -> qMap.get(id).getQuestionType() == QuestionType.INITIALS)
                .toList();

        Map<Long, String> answerTextByQid =
                initialsQids.isEmpty()
                    ? Map.of()
                    : quizTextAnswerRepository.findByQuizQuestion_IdIn(initialsQids).stream()
                            .collect(Collectors.toMap(
                                    a -> a.getQuizQuestion().getId(),
                                    QuizTextAnswer::getAnswerText,
                                    (a,b) -> a
                            ));

        List<StartQuizSessionResponse.Item> items = uniqIds.stream()
                .map(qid -> {
                    QuizQuestion q = qMap.get(qid);

                    if (q.getQuestionType() == QuestionType.INITIALS) {

                        String answerText = Optional.ofNullable(answerTextByQid.get(qid))
                                .map(String::trim)
                                .orElse(null);

                        // 정답이 없을 시 세션 생성이 안 되도록 함
                        if (answerText == null || answerText.isBlank()) {
                            throw new IllegalStateException("초성 문제 정답이 등록되지 않았습니다: "+qid);
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
                        Long correctChoiceId = choices.stream()
                                .filter(c -> Boolean.TRUE.equals(c.isAnswer()))
                                .findFirst()
                                .map(QuizChoice::getId)
                                .orElse(null);

                        var options = choices.stream()
                                .map(c -> new StartQuizSessionResponse.Option(c.getId(), c.getChoiceText()))
                                .toList();

                        return new StartQuizSessionResponse.Item(
                                q.getId(),
                                q.getQuestionType(),
                                q.getQuestionText(),
                                q.getExplanation(),
                                correctChoiceId,
                                options,
                                null
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

                    Long correctChoiceId = ordered.stream()
                            .filter(c -> Boolean.TRUE.equals(c.isAnswer()))
                            .findFirst()
                            .map(QuizChoice::getId)
                            .orElse(null);

                    var options = ordered.stream()
                            .map(c -> new StartQuizSessionResponse.Option(c.getId(), c.getChoiceText()))
                            .toList();

                    return new StartQuizSessionResponse.Item(
                            q.getId(),
                            q.getQuestionType(),
                            q.getQuestionText(),
                            q.getExplanation(),
                            correctChoiceId,
                            options,
                            null
                    );
                })
                .toList();

        return new StartQuizSessionResponse(session.getId(), quizSet.getId(), uniqIds, items);
    }

    @Override
    @Transactional
    public SubmitQuizSessionResponseForm submitSession(Long sessionId, Long accountId, SubmitQuizSessionRequestForm requestForm) {

        QuizSession session = quizSessionRepository
                .findByIdAndAccount_IdAndDeletedAtIsNull(sessionId, accountId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다."));
        if (!session.getAccount().getId().equals(accountId)) throw new SecurityException("세션 접근 권한이 없습니다.");
        if (session.getSubmittedAt() != null) throw new IllegalStateException("이미 제출된 세션입니다.");

        // 스냅샷 매칭(부분 제출 방지)
        Set<Long> snapshotQids = parseSnapshotIds(session.getQuestionsSnapshotJson());
        List<Long> submittedQids = requestForm.getAnswers().stream()
                .map(SubmitQuizSessionRequestForm.AnswerForm::getQuizQuestionId)
                .toList();

        if (submittedQids.size() != snapshotQids.size()
                || new HashSet<>(submittedQids).size() != submittedQids.size()) {
            throw new IllegalArgumentException("제출 문항이 중복되었거나 스냅샷과 불일치합니다.");
        }

        // 배치 조회
        Map<Long, QuizQuestion> qMap = quizQuestionRepository.findAllById(submittedQids)
                .stream().collect(Collectors.toMap(QuizQuestion::getId, q -> q));

        List<Long> choiceIds = requestForm.getAnswers().stream()
                .map(SubmitQuizSessionRequestForm.AnswerForm::getSelectedChoiceId)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, QuizChoice> cMap = quizChoiceRepository.findAllById(choiceIds)
                .stream().collect(Collectors.toMap(QuizChoice::getId, c -> c));

        // INITIALS 정답 텍스트 배치 조회
        List<Long> initialsQids = submittedQids.stream()
                .filter(qid -> qMap.get(qid) != null && qMap.get(qid).getQuestionType() == QuestionType.INITIALS)
                .toList();

        Map<Long, String> expectedTextByQid =
                quizTextAnswerRepository.findByQuizQuestion_IdIn(initialsQids).stream()
                        .collect(Collectors.toMap(
                                a -> a.getQuizQuestion().getId(),
                                QuizTextAnswer::getAnswerText,
                                (a,b) -> a
                        ));

        // 정답 보기들(선택형)
        Map<Long, List<Long>> correctIdsByQid = quizChoiceRepository.findByQuizQuestionIdIn(submittedQids)
                .stream()
                .filter(QuizChoice::isAnswer)
                .collect(Collectors.groupingBy(
                        c -> c.getQuizQuestion().getId(),
                        Collectors.mapping(QuizChoice::getId, Collectors.toList())
                ));

        int correctCount = 0;
        Instant now = Instant.now();

        List<QuizSessionAnswer> answersToSave = new ArrayList<>();
        List<SubmitQuizSessionResponseForm.Item> details = new ArrayList<>();

        for (var a : requestForm.getAnswers()) {
            QuizQuestion q = Optional.ofNullable(qMap.get(a.getQuizQuestionId()))
                    .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 문제입니다: " + a.getQuizQuestionId()));

            // ===== INITIALS(텍스트형) =====
            if (q.getQuestionType() == QuestionType.INITIALS) {

                String submittedRaw = Optional.ofNullable(a.getTextAnswer()).orElse("");
                if (submittedRaw.isBlank()) throw new IllegalArgumentException("초성 퀴즈 답변이 비었습니다: " + q.getId());

                String expectedRaw = Optional.ofNullable(expectedTextByQid.get(q.getId()))
                        .orElseThrow(() -> new IllegalArgumentException("초성 퀴즈 정답이 등록되지 않았습니다: " + q.getId()));

                String submittedNorm = normalizeTextAnswer(submittedRaw);
                String expectedNorm = normalizeTextAnswer(expectedRaw);

                boolean isCorrect = !submittedNorm.isBlank() && submittedNorm.equals(expectedNorm);
                if (isCorrect) correctCount++;

                String submittedToSave = submittedRaw.trim();
                String expectedToShow  = expectedRaw.trim();

                answersToSave.add(QuizSessionAnswer.forText(session, q, submittedToSave, isCorrect, now));

                details.add(new SubmitQuizSessionResponseForm.Item(
                        q.getId(),
                        null,
                        null,
                        null,
                        null,
                        submittedToSave,
                        expectedToShow,
                        isCorrect
                ));
                continue;
            }

            // ===== CHOICE / OX =====
            Long selectedId = a.getSelectedChoiceId();
            if (selectedId == null) throw new IllegalArgumentException("선택형 보기 ID가 비었습니다: " + q.getId());

            QuizChoice c = Optional.ofNullable(cMap.get(selectedId))
                    .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 보기입니다: " + selectedId));

            if (!c.getQuizQuestion().getId().equals(q.getId())) {
                throw new IllegalArgumentException("선택한 보기는 해당 문제의 보기가 아닙니다.");
            }

            List<Long> correctIds = correctIdsByQid.getOrDefault(q.getId(), List.of());
            Long correctChoiceId = (correctIds.size() == 1) ? correctIds.get(0) : null;
            List<Long> correctChoiceIds = (correctIds.size() > 1) ? correctIds : null;

            boolean isCorrect = c.isAnswer();
            if (isCorrect) correctCount++;

            answersToSave.add(QuizSessionAnswer.forChoice(
                    session,
                    q,
                    c.getId(),
                    c.getChoiceText(),
                    isCorrect,
                    now
            ));

            details.add(new SubmitQuizSessionResponseForm.Item(
                    q.getId(),
                    c.getId(),
                    null,
                    correctChoiceId,
                    correctChoiceIds,
                    null,
                    null,
                    isCorrect
            ));
        }

        quizSessionAnswerRepository.saveAll(answersToSave);

        Long elapsedMs = requestForm.getElapsedMs();
        if (elapsedMs == null && session.getStartedAt() != null) {
            elapsedMs = Duration.between(session.getStartedAt(), now).toMillis();
        }

        session.submit(correctCount, elapsedMs);
        quizSessionRepository.save(session);

        // 오답노트 저장(선택형+텍스트형 모두 QuizSessionAnswer 하나로 처리)
        quizWrongNoteService.saveWrongNotes(answersToSave, accountId);

        int total = submittedQids.size();
        return new SubmitQuizSessionResponseForm(
                session.getId(),
                total,
                correctCount,
                elapsedMs,
                details
        );
    }

    @Override
    @Transactional
    public StartQuizSessionResponse startFromScope(
            Long accountId,
            SessionSource source,
            List<Long> pickedQuestionIds,
            SeedMode mode,
            long seedValue,
            String customTitle
    ) {
        if (accountId == null) throw new IllegalArgumentException("accountId required");
        if (source == null) throw new IllegalArgumentException("source required");
        if (pickedQuestionIds == null || pickedQuestionIds.isEmpty())
            throw new IllegalArgumentException("pickedQuestionIds required");

        SessionSourceType sourceType = source.getSourceType();
        Long sourceId = source.getSourceId();
        String sourceKey = source.getSourceKey();
        QuizSetType partType = source.getPartType();

        if (sourceType == null) throw new IllegalArgumentException("sourceType required");
        if (sourceId == null) throw new IllegalArgumentException("sourceId required");

        if (sourceKey == null || sourceKey.isBlank()) {
            sourceKey = buildSourceKey(sourceType, sourceId, mode);
        }

        int attemptNo = quizSessionRepository.findMaxAttemptNoBySourceKey(accountId, sourceKey) + 1;

        // 질문 중복 제거 + 순서 보존
        List<Long> uniqIds = new ArrayList<>(new LinkedHashSet<>(pickedQuestionIds));

        List<QuizQuestion> questions = quizQuestionRepository.findAllById(uniqIds);
        Map<Long, QuizQuestion> qMap = questions.stream()
                .collect(Collectors.toMap(QuizQuestion::getId, q -> q, (a, b) -> a, LinkedHashMap::new));

        if (qMap.size() != uniqIds.size()) {
            Set<Long> missing = new LinkedHashSet<>(uniqIds);
            missing.removeAll(qMap.keySet());
            throw new IllegalStateException("일부 질문을 찾지 못했습니다(삭제/비활성 가능): " + missing);
        }

        if (partType == null) {
            partType = resolvePartTypeFromQuestions(questions);
        }

        Account account = accountRepository.getReferenceById(accountId);

        QuizSession session = new QuizSession();
        session.beginFromSource(
                account,
                sourceType,
                sourceId,
                sourceKey,
                partType,
                SessionMode.FULL,
                attemptNo,
                uniqIds.size(),
                toJson(uniqIds),
                mode,
                seedValue
        );

        // customTitle 저장
        if (customTitle != null && !customTitle.isBlank()) {
            session.changeTitle(customTitle.trim());
        }

        quizSessionRepository.save(session);

        // 보기 배치 조회
        var allChoices = quizChoiceRepository.findByQuizQuestionIdInOrderByQuizQuestionIdAscIdAsc(uniqIds);
        Map<Long, List<QuizChoice>> byQ = allChoices.stream()
                .collect(Collectors.groupingBy(c -> c.getQuizQuestion().getId()));

        long baseSeed = seedValue;
        Map<Integer, AnswerIndexPlanner> planners = new HashMap<>();

        List<Long> initialsQids = uniqIds.stream()
                .filter(id -> qMap.get(id).getQuestionType() == QuestionType.INITIALS)
                .toList();

        Map<Long, String> answerTextByQid =
                initialsQids.isEmpty()
                        ? Map.of()
                        : quizTextAnswerRepository.findByQuizQuestion_IdIn(initialsQids).stream()
                        .collect(Collectors.toMap(
                                a -> a.getQuizQuestion().getId(),
                                QuizTextAnswer::getAnswerText,
                                (a, b) -> a
                        ));

        List<StartQuizSessionResponse.Item> items = uniqIds.stream()
                .map(qid -> {
                    QuizQuestion q = qMap.get(qid);

                    if (q.getQuestionType() == QuestionType.INITIALS) {

                        String answerText = Optional.ofNullable(answerTextByQid.get(qid))
                                .map(String::trim)
                                .orElse(null);

                        // 정답이 없을 시 세션 생성이 안 되도록 함
                        if (answerText == null) {
                            throw new IllegalStateException("초성퀴즈 정답이 등록되지 않았습니다: "+qid);
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
                        Long correctChoiceId = choices.stream()
                                .filter(QuizChoice::isAnswer)
                                .findFirst()
                                .map(QuizChoice::getId)
                                .orElse(null);

                        var options = choices.stream()
                                .map(c -> new StartQuizSessionResponse.Option(c.getId(), c.getChoiceText()))
                                .toList();

                        return new StartQuizSessionResponse.Item(
                                q.getId(),
                                q.getQuestionType(),
                                q.getQuestionText(),
                                q.getExplanation(),
                                correctChoiceId,
                                options,
                                null
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

                    Long correctChoiceId = ordered.stream()
                            .filter(QuizChoice::isAnswer)
                            .findFirst()
                            .map(QuizChoice::getId)
                            .orElse(null);

                    var options = ordered.stream()
                            .map(c -> new StartQuizSessionResponse.Option(c.getId(), c.getChoiceText()))
                            .toList();

                    return new StartQuizSessionResponse.Item(
                            q.getId(),
                            q.getQuestionType(),
                            q.getQuestionText(),
                            q.getExplanation(),
                            correctChoiceId,
                            options,
                            null
                    );
                })
                .toList();

        Long quizSetId = (sourceType == SessionSourceType.SET) ? sourceId : null;
        return new StartQuizSessionResponse(session.getId(), quizSetId, uniqIds, items);
    }

    @Override
    @Transactional(readOnly = true)
    public StartQuizSessionResponse loadForPlay(Long accountId, Long sessionId) {

        QuizSession session = quizSessionRepository
                .findByIdAndAccount_IdAndDeletedAtIsNull(sessionId, accountId)
                .orElseThrow(() -> new IllegalArgumentException("세션이 없습니다."));

        if (!session.getAccount().getId().equals(accountId)) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }

        // 1) 세션 스냅샷에서 문항 순서 복구 (IN_PROGRESS에서도 항상 존재)
        List<Long> qids = parseSnapshotIdList(session.getQuestionsSnapshotJson());
        if (qids.isEmpty()) {
            return StartQuizSessionResponse.fromExistingWithItems(sessionId, null, List.of(), List.of());
        }

        // 2) 질문 로드
        List<QuizQuestion> questions = quizQuestionRepository.findAllById(qids);
        Map<Long, QuizQuestion> qById = questions.stream()
                .collect(Collectors.toMap(QuizQuestion::getId, q -> q));

        // 3) 보기 배치 로드
        List<QuizChoice> choices = quizChoiceRepository.findByQuizQuestionIdInOrderByQuizQuestionIdAscIdAsc(qids);
        Map<Long, List<QuizChoice>> choicesByQ = choices.stream()
                .collect(Collectors.groupingBy(c -> c.getQuizQuestion().getId()));

        // 4) start 때와 동일한 옵션 순서 재현 (seed 기반)
        long baseSeed = session.getSeedValue();

        Map<Integer, AnswerIndexPlanner> planners = new HashMap<>();

        List<StartQuizSessionResponse.Item> items = new ArrayList<>();

        List<Long> initialsQids = qids.stream()
                .filter(id -> qById.get(id) != null && qById.get(id).getQuestionType() == QuestionType.INITIALS)
                .toList();

        Map<Long, String> answerTextByQid =
                quizTextAnswerRepository.findByQuizQuestion_IdIn(initialsQids).stream()
                        .collect(Collectors.toMap(
                                a -> a.getQuizQuestion().getId(),
                                QuizTextAnswer::getAnswerText,
                                (a,b) -> a
                        ));

        for (Long qid : qids) {
            QuizQuestion q = qById.get(qid);
            if (q == null) continue;

            if (q.getQuestionType() == QuestionType.INITIALS) {
                String answerText = Optional.ofNullable(answerTextByQid.get(qid))
                        .map(String::trim)
                        .orElse(null);

                if (answerText == null || answerText.isBlank()) {
                    throw new IllegalStateException("초성퀴즈 정답이 등록되지 않았습니다: "+qid);
                }

                items.add(new StartQuizSessionResponse.Item(
                        q.getId(),
                        q.getQuestionType(),
                        q.getQuestionText(),
                        q.getExplanation(),
                        null,
                        List.of(),
                        answerText
                ));
                continue;
            }

            List<QuizChoice> cs = new ArrayList<>(choicesByQ.getOrDefault(qid, List.of()));
            int optionCount = cs.size();

            List<QuizChoice> ordered = cs;

            if (optionCount >= 2) {
                AnswerIndexPlanner planner = planners.computeIfAbsent(
                        optionCount,
                        oc -> new AnswerIndexPlanner(oc, mixSeed(baseSeed, oc))
                );
                ordered = reorderWithBalancedAnswerIndex(
                        q.getQuestionType(),
                        cs,
                        planner,
                        mixSeed(baseSeed, qid)
                );
            }

            Long correctChoiceId = ordered.stream()
                    .filter(QuizChoice::isAnswer)
                    .findFirst()
                    .map(QuizChoice::getId)
                    .orElse(null);

            List<StartQuizSessionResponse.Option> opts = ordered.stream()
                    .map(c -> new StartQuizSessionResponse.Option(c.getId(), c.getChoiceText()))
                    .toList();

            items.add(new StartQuizSessionResponse.Item(
                    q.getId(),
                    q.getQuestionType(),
                    q.getQuestionText(),
                    q.getExplanation(),
                    correctChoiceId,
                    opts,
                    null
            ));
        }

        // quizSetId는 SET 세션이면 sourceId를 넣는 게 더 정확할 때가 많음
        Long quizSetId = null;
        if (session.getSourceType() == SessionSourceType.SET) {   // 엔티티 필드명에 맞게
            quizSetId = session.getSourceId();
        }

        return StartQuizSessionResponse.fromExistingWithItems(sessionId, quizSetId, qids, items);
    }

    private long resolveSeed(SeedMode seedMode, Long accountId, Long fixedSeed) {
        if (seedMode == null) return ThreadLocalRandom.current().nextLong();
        return switch (seedMode) {
            case DAILY -> {
                var zone = ZoneId.of("Asia/Seoul");
                var today = LocalDate.now(zone);
                yield Objects.hash(accountId, today);
            }
            case FIXED -> {
                if (fixedSeed == null) throw new IllegalArgumentException("fixedSeed required for FIXED");
                yield fixedSeed;
            }
            default -> ThreadLocalRandom.current().nextLong();
        };
    }

    private String buildSourceKey(SessionSourceType sourceType, Long sourceId, SeedMode mode) {
        if (mode == SeedMode.DAILY) {
            var today = LocalDate.now(ZoneId.of("Asia/Seoul"));
            return sourceType.name() + ":" + sourceId + ":DAILY:" + today;
        }
        return sourceType.name() + ":" + sourceId;
    }

    private Set<Long> parseSnapshotIds(String json) {
        if (json == null || json.isBlank()) return Collections.emptySet();
        try {
            List<Long> ids = objectMapper.readValue(json, new TypeReference<List<Long>>() {});
            return new HashSet<>(ids);
        } catch (Exception e) {
            log.warn("세션 스냅샷 파싱 실패: {}", e.getMessage());
            return Collections.emptySet();
        }
    }

    private List<QuizChoice> reorderWithBalancedAnswerIndex(
            QuestionType questionType,
            List<QuizChoice> choices,
            AnswerIndexPlanner planner,
            long questionSeed
    ) {
        if (choices == null || choices.size() <= 1) return choices;

        // OX는 보기 순서 고정: "O" -> "X"
        if (questionType == QuestionType.OX) {
            return choices.stream()
                    .sorted(Comparator.comparing((QuizChoice c) -> {
                        String t = Optional.ofNullable(c.getChoiceText()).orElse("")
                                .trim().toUpperCase();
                        return "O".equals(t) ? 0 : "X".equals(t) ? 1 : 2;
                    }).thenComparingLong(QuizChoice::getId))
                    .toList();
        }

        List<QuizChoice> shuffled = new ArrayList<>(choices);
        Collections.shuffle(shuffled, new Random(questionSeed));

        int targetIdx = planner.nextIndex();

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

    private QuizSetType resolvePartTypeFromQuestions(List<QuizQuestion> questions) {
        if (questions == null || questions.isEmpty()) return QuizSetType.MIX;

        Set<QuestionType> types = questions.stream()
                .map(QuizQuestion::getQuestionType)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (types.isEmpty()) return QuizSetType.MIX;
        if (types.size() == 1) return mapToPartType(types.iterator().next());
        return QuizSetType.MIX;
    }

    private QuizSetType mapToPartType(QuestionType qt) {
        try {
            return QuizSetType.valueOf(qt.name());
        } catch (IllegalArgumentException e) {
            return QuizSetType.MIX;
        }
    }

    private List<Long> parseSnapshotIdList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            log.warn("세션 스냅샷(List) 파싱 실패: {}", e.getMessage());
            return List.of();
        }
    }

    private static String normalizeTextAnswer(String s) {
        if (s == null) return "";
        // (선택) 유니코드 정규화: 특수 케이스 대비
        String t = Normalizer.normalize(s, Normalizer.Form.NFKC);

        // 모든 공백 제거(스페이스/탭/개행 등)
        t = t.replaceAll("\\s+", "");

        // 대소문자 무시(영문 대비)
        t = t.toLowerCase(Locale.ROOT);

        return t;
    }
}
