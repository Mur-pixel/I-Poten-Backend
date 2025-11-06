package com.cygnus.ipoten.quiz.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.account.repository.AccountRepository;
import com.cygnus.ipoten.quiz.controller.request_form.SubmitQuizSessionRequestForm;
import com.cygnus.ipoten.quiz.controller.response_form.SubmitQuizSessionResponseForm;
import com.cygnus.ipoten.quiz.entity.*;
import com.cygnus.ipoten.quiz.entity.enums.*;
import com.cygnus.ipoten.quiz.repository.*;
import com.cygnus.ipoten.quiz.service.response.StartUserQuizSessionResponse;
import com.cygnus.ipoten.quiz.service.util.AnswerIndexPlanner;
import com.cygnus.ipoten.quiz.service.util.SeedUtil;
import com.cygnus.ipoten.userTrustscore.service.TrustScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserQuizAnswerServiceImpl implements UserQuizAnswerService {

    private final AccountRepository accountRepository;
    private final UserQuizSessionRepository userQuizSessionRepository;
    private final QuizSetRepository quizSetRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizChoiceRepository quizChoiceRepository;
    private final UserWrongNoteRepository userWrongNoteRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SessionAnswerRepository sessionAnswerRepository;
    private final TrustScoreService trustScoreService;

    // 정답 위치 분배 시드 계산용
    private final SeedUtil seedUtil = new SeedUtil();

    @Override
    @Transactional
    public StartUserQuizSessionResponse startFromQuizSet(
            Long accountId, Long quizSetId, List<Long> questionIds, SeedMode seedMode, Long fixedSeed) {
        Account account = accountRepository.getReferenceById(accountId);
        QuizSet quizSet = quizSetRepository.getReferenceById(quizSetId);

        long resolvedSeed;
        switch (seedMode) {
            case DAILY -> {
                var zone = ZoneId.of("Asia/Seoul");
                var today = LocalDate.now(zone);
                resolvedSeed = Objects.hash(accountId, today);
            }
            case FIXED -> {
                if (fixedSeed == null) throw new IllegalArgumentException("fixedSeed required for FIXED");
                resolvedSeed = fixedSeed;
            }
            default -> resolvedSeed = ThreadLocalRandom.current().nextLong();
        }

        UserQuizSession session = new UserQuizSession();
        // 세션 시작
        session.begin(account, quizSet, SessionMode.FULL, 1, questionIds.size(), toJson(questionIds), seedMode, resolvedSeed);

        userQuizSessionRepository.save(session);

        // 미리보기용 아이템 구성
        // 세션을 만들자마자 바로 풀 수 있도록 서버가 응답에 문항 목록(문제 본문 + 보기 텍스트)을 함께 실어주는 것
        List<QuizQuestion> questions = quizQuestionRepository.findAllById(questionIds);

        // 질문 순서 보존
        Map<Long, QuizQuestion> qMap = questions.stream()
                .collect(Collectors.toMap(QuizQuestion::getId, q -> q, (a,b)->a, LinkedHashMap::new));

        // 보기 배치 조회
        var allChoices = quizChoiceRepository.findByQuizQuestionIdIn(questionIds);
        Map<Long, List<QuizChoice>> byQ = allChoices.stream()
                .collect(Collectors.groupingBy(c -> c.getQuizQuestion().getId()));

        /* ================= 정답 위치 분배기(라운드로빈 + 셔플) =================
           - 세션 레벨 시드로 옵션 개수별 planner 를 만들고 순환 사용
           - 각 문제의 보기는 마이크로 셔플하되, 정답을 지정된 인덱스로 강제 배치
           - FIXED/DAILY 모드의 결정성을 유지하기 위해 seedMode/accountId/fixedSeed로부터 시드 생성
        ===================================================================== */
        long baseSeed = resolvedSeed;
        Map<Integer, AnswerIndexPlanner> planners = new HashMap<>();

        List<StartUserQuizSessionResponse.Item> items = questionIds.stream()
                .map(qid -> {
                    QuizQuestion q = qMap.get(qid);
                    List<QuizChoice> choices = new ArrayList<>(byQ.getOrDefault(qid, List.of()));

                    int optionCount = choices.size();
                    if (optionCount < 2) {
                        // 방어적 처리: 보기 수가 2 미만이면 그대로 반환
                        var options = choices.stream()
                                .map(c -> new StartUserQuizSessionResponse.Option(c.getId(), c.getChoiceText()))
                                .toList();
                        return new StartUserQuizSessionResponse.Item(
                                q.getId(), q.getQuestionType(), q.getQuestionText(), null, null, options
                        );
                    }

                    // 옵션 개수별 라운드로빈 planner (세션 결정 시드 기반)
                    AnswerIndexPlanner planner = planners.computeIfAbsent(
                            optionCount,
                            oc -> new AnswerIndexPlanner(oc, mixSeed(baseSeed, oc))
                    );

                    // 마이크로 랜덤성(문제별 셔플) + 정답 강제 배치
                    List<QuizChoice> ordered = reorderWithBalancedAnswerIndex(
                            choices, planner, mixSeed(baseSeed, qid));

                    var options = ordered.stream()
                            .map(c -> new StartUserQuizSessionResponse.Option(c.getId(), c.getChoiceText()))
                            .toList();

                    return new StartUserQuizSessionResponse.Item(
                            q.getId(),
                            q.getQuestionType(),
                            q.getQuestionText(),
                            null,
                            null,
                            options
                    );
                })
                .toList();

        return new StartUserQuizSessionResponse(session.getId(), quizSet.getId(), questionIds, items);
    }

    @Override
    @Transactional
    public StartUserQuizSessionResponse startRetryWrongOnly(Long parentSessionId, Long accountId) {
        // 1) 권한/상태 검증 : 내 세션인지 확인
        UserQuizSession parent = userQuizSessionRepository.findByIdAndAccount_Id(parentSessionId, accountId)
                .orElseThrow(() -> new SecurityException("본인 세션이 아니거나 존재하지 않습니다."));

        // 제출 완료된 세션만 허용
        if (parent.getSessionStatus() != SessionStatus.SUBMITTED) {
            throw new IllegalStateException("제출 완료된 세션에서만 오답 재도전이 가능합니다.");
        }

        // 2) 원본 세션에서 오답 질문만 추출
        List<Long> wrongQuestionId = sessionAnswerRepository.findWrongQuestionIds(parentSessionId);
        if (wrongQuestionId.isEmpty()) {
            throw new IllegalStateException("오답이 없어 재시작할 문제가 없습니다.");
        }

        // 2-1) 오답 질문 로드 (partType 결정을 위해 선행)
        List<QuizQuestion> questions = quizQuestionRepository.findAllById(wrongQuestionId);

        // 2-2) 부모 세트의 partType을 우선 사용, 없으면 질문 타입으로 유추(단일=그 타입, 혼합=MIX)
        QuizPartType resolvedPartType = resolvePartTypeFromParentOrQuestions(
                parent,
                questions
        );

        // 3) 임시 QuizSet 생성(표시용) - ★ partType과 category를 반드시 지정
        QuizSet retrySet = new QuizSet("틀린 문제만 다시 풉니다.", true);
        retrySet.setPartType(resolvedPartType);
        quizSetRepository.save(retrySet);

        // 4) 새 세션 시작(부모-자식 연결 + 스냅샷 저장)
        Account account = accountRepository.getReferenceById(accountId);
        UserQuizSession child = new UserQuizSession();
        child.beginWithParent(
                account,
                retrySet,
                parent,
                SessionMode.WRONG_ONLY,
                (parent.getAttemptNo() == null ? 2 : parent.getAttemptNo() + 1),
                wrongQuestionId.size(),
                toJson(wrongQuestionId)
        );
        userQuizSessionRepository.save(child);

        // 5) 미리보기용 아이템 구성(질문/보기 로딩, 순서 유지) - 위에서 이미 questions 로드했으니 재사용
        Map<Long, QuizQuestion> qMap = questions.stream()
                .collect(Collectors.toMap(QuizQuestion::getId, q -> q, (a, b) -> a, LinkedHashMap::new));

        var allChoices = quizChoiceRepository.findByQuizQuestionIdIn(wrongQuestionId);
        Map<Long, List<QuizChoice>> byQ = allChoices.stream()
                .collect(Collectors.groupingBy(c -> c.getQuizQuestion().getId()));

    /* === 정답 위치 분배(오답 재도전) =======================================
       - 부모 세션ID + 계정ID를 섞어 결정적 시드 생성
    ===================================================================== */
        long baseSeed = mixSeed(Objects.hash(parentSessionId, 1315423911L), accountId);
        Map<Integer, AnswerIndexPlanner> planners = new HashMap<>();

        List<StartUserQuizSessionResponse.Item> items = wrongQuestionId.stream()
                .map(qid -> {
                    QuizQuestion q = qMap.get(qid);
                    List<QuizChoice> choices = new ArrayList<>(byQ.getOrDefault(qid, List.of()));
                    int optionCount = choices.size();

                    if (optionCount < 2) {
                        var options = choices.stream()
                                .map(c -> new StartUserQuizSessionResponse.Option(c.getId(), c.getChoiceText()))
                                .toList();
                        return new StartUserQuizSessionResponse.Item(
                                q.getId(), q.getQuestionType(), q.getQuestionText(), null, null, options
                        );
                    }

                    AnswerIndexPlanner planner = planners.computeIfAbsent(
                            optionCount,
                            oc -> new AnswerIndexPlanner(oc, mixSeed(baseSeed, oc))
                    );

                    List<QuizChoice> ordered = reorderWithBalancedAnswerIndex(
                            choices, planner, mixSeed(baseSeed, qid));

                    var options = ordered.stream()
                            .map(c -> new StartUserQuizSessionResponse.Option(c.getId(), c.getChoiceText()))
                            .toList();

                    return new StartUserQuizSessionResponse.Item(
                            q.getId(),
                            q.getQuestionType(),
                            q.getQuestionText(),
                            null,
                            null,
                            options
                    );
                })
                .toList();
        return new StartUserQuizSessionResponse(child.getId(), retrySet.getId(), wrongQuestionId, items);
    }

    @Override
    public SubmitQuizSessionResponseForm submitSession(Long sessionId, Long accountId, SubmitQuizSessionRequestForm requestForm) {

        // 1) 세션 로드 + 소유자/상태 검증
        UserQuizSession session = userQuizSessionRepository.findById(sessionId).orElseThrow(()-> new IllegalArgumentException("세션을 찾을 수 없습니다."));
        if (!session.getAccount().getId().equals(accountId)) {
            throw new SecurityException("세션 접근 권한이 없습니다.");
        }
        if (session.getSubmittedAt() != null) {
            throw new IllegalStateException("이미 제출된 세션입니다.");
        }

        // 2) 스냅샷과 제출 문항 매칭
        Set<Long> snapshotQids = parseSnapshotIds(session.getQuestionsSnapshotJson());
        List<Long> submittedQids = requestForm.getAnswers().stream()
                .map(SubmitQuizSessionRequestForm.AnswerForm::getQuizQuestionId)
                .toList();
        if (!snapshotQids.containsAll(submittedQids)) {
            throw new IllegalArgumentException("세션에 속하지 않는 문제가 포함되어 있습니다.");
        }

        // 3) 배치 조회
        List<Long> choiceIds = requestForm.getAnswers().stream()
                .map(SubmitQuizSessionRequestForm.AnswerForm::getSelectedChoiceId)
                .toList();

        Map<Long, QuizQuestion> qMap = quizQuestionRepository.findAllById(submittedQids)
                .stream().collect(Collectors.toMap(QuizQuestion::getId, q -> q));

        Map<Long, QuizChoice> cMap = quizChoiceRepository.findAllById(choiceIds)
                .stream().collect(Collectors.toMap(QuizChoice::getId, c -> c));

        // 제출된 문항들에 대한 '정답 보기 id들'을 한 번에 맵으로 만들기
        Map<Long, List<Long>> correctIdsByQid = quizChoiceRepository.findByQuizQuestionIdIn(submittedQids)
                .stream()
                .filter(QuizChoice::isAnswer)
                .collect(Collectors.groupingBy(
                        c -> c.getQuizQuestion().getId(),
                        Collectors.mapping(QuizChoice::getId, Collectors.toList())
                ));

        // 4) 채점 + SessionAnswer 생성
        int correctCount = 0;
        LocalDateTime now = LocalDateTime.now();
        List<SessionAnswer> toSave = new ArrayList<>();
        List<SubmitQuizSessionResponseForm.Item> details = new ArrayList<>();

        for (var a : requestForm.getAnswers()) {
            QuizQuestion q = Optional.ofNullable(qMap.get(a.getQuizQuestionId()))
                    .orElseThrow(()-> new IllegalArgumentException("유효하지 않은 문제입니다: " + a.getQuizQuestionId()));

            QuizChoice c = Optional.ofNullable(cMap.get(a.getSelectedChoiceId()))
                    .orElseThrow(()-> new IllegalArgumentException("유효하지 않은 보기입니다: " + a.getSelectedChoiceId()));

            if (!c.getQuizQuestion().getId().equals(q.getId())) {
                throw new IllegalArgumentException("선택한 보기는 해당 문제의 보기가 아닙니다.");
            }

            List<Long> correctIds = correctIdsByQid.getOrDefault(q.getId(), Collections.emptyList());
            Long correctChoiceId = (correctIds.size() == 1) ? correctIds.get(0) : null;
            List<Long> correctChoiceIds = (correctIds.size() > 1) ? correctIds : null;

            boolean isCorrect = c.isAnswer();
            if (isCorrect) correctCount++;

            toSave.add(new SessionAnswer(session, q, c, now, isCorrect));

            details.add(new SubmitQuizSessionResponseForm.Item(
                    q.getId(),
                    c.getId(),
                    null,
                    correctChoiceId,
                    correctChoiceIds,
                    isCorrect
            ));
        }

        // 5) 벌크 저장 + 세션 상태 업데이트(소요시간 처리)
        sessionAnswerRepository.saveAll(toSave);

        Long elapsedMs = requestForm.getElapsedMs();
        if (elapsedMs == null && session.getStartedAt() != null) {
            elapsedMs = Duration.between(session.getStartedAt(), now).toMillis();
        }

        session.submit(correctCount, elapsedMs);
        userQuizSessionRepository.save(session);

        // 퀴즈 완료 시 갱신
        trustScoreService.calculateTrustScore(accountId);

        // 6) 오답노트 저장(중복 방지)
        saveWrongNotes(toSave, accountId);

        return new SubmitQuizSessionResponseForm(
                session.getId(),
                (session.getTotal() == null ? toSave.size() : session.getTotal()),
                correctCount,
                elapsedMs,
                details
        );
    }

    @Override
    @Transactional
    public void saveWrongNotes(List<SessionAnswer> answers, Long accountId) {
        for (SessionAnswer a : answers) {
            if (a.isCorrect()) continue;

            var opt = userWrongNoteRepository
                    .findByAccount_IdAndQuizQuestion_Id(accountId, a.getQuizQuestion().getId());

            if (opt.isPresent()) {
                // 기존 노트 업데이트
                var wn = opt.get();
                wn.setQuizChoice(a.getQuizChoice());
                wn.setExplanation(a.getQuizChoice().getExplanation());
                wn.setSubmittedAt(a.getSubmittedAt());
                // jpa dirty checking으로 UPDATE
            } else {
                // 신규 생성
                var wn = UserWrongNote.builder()
                        .account(a.getUserQuizSession().getAccount())
                        .quizQuestion(a.getQuizQuestion())
                        .quizChoice(a.getQuizChoice())
                        .submittedAt(a.getSubmittedAt())
                        .explanation(a.getQuizChoice().getExplanation())
                        .build();
                userWrongNoteRepository.save(wn);
            }
        }
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

    /* ========================= 정답 위치 재배열 유틸 ========================= */

    /**
     * 각 문제의 보기 리스트를 (결정적 셔플 + 정답 인덱스 강제 배치) 규칙으로 재배열한다.
     * - planner: 옵션 개수 단위의 라운드로빈을 유지하여 균등 분포 보장
     * - questionSeed: 문제별 미세 셔플에 사용(결정성 보장)
     */
    private List<QuizChoice> reorderWithBalancedAnswerIndex(
            List<QuizChoice> choices,
            AnswerIndexPlanner planner,
            long questionSeed
    ) {
        if (choices == null || choices.size() <= 1) return choices;

        // 문제 단위 마이크로 셔플
        List<QuizChoice> shuffled = new ArrayList<>(choices);
        Collections.shuffle(shuffled, new Random(questionSeed));

        // 정답 대상 인덱스
        int targetIdx = planner.nextIndex();

        // 현재 정답 위치
        int currentIdx = -1;
        for (int i = 0; i < shuffled.size(); i++) {
            if (Boolean.TRUE.equals(shuffled.get(i).isAnswer())) {
                currentIdx = i;
                break;
            }
        }
        if (currentIdx < 0) return shuffled; // 안전가드 (정답 없음)

        // 정답을 targetIdx로 이동
        QuizChoice correct = shuffled.remove(currentIdx);
        if (targetIdx < 0) targetIdx = 0;
        if (targetIdx > shuffled.size()) targetIdx = shuffled.size();
        shuffled.add(targetIdx, correct);

        return shuffled;
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

    /** 부모 세트의 partType을 우선 사용, 없으면 질문 타입 기반으로 유추 */
    private QuizPartType resolvePartTypeFromParentOrQuestions(UserQuizSession parent, List<QuizQuestion> questions) {
        QuizSet parentSet = parent.getQuizSet();
        if (parentSet != null && parentSet.getPartType() != null) {
            return parentSet.getPartType();
        }
        // 질문 타입 셋 추출
        Set<QuestionType> types = questions.stream()
                .map(QuizQuestion::getQuestionType)
                .collect(Collectors.toSet());

        if (types.isEmpty()) return QuizPartType.MIX; // 가드

        if (types.size() == 1) {
            // 단일 타입이면 그 타입으로 매핑
            QuestionType only = types.iterator().next();
            return mapToPartType(only);
        }
        // 혼합
        return QuizPartType.MIX;
    }

    /** QuestionType -> QuizPartType 매핑 (상수명이 같다면 valueOf로 충분) */
    private QuizPartType mapToPartType(QuestionType qt) {
        // 상수명이 동일한 경우 (예: CHOICE, OX, INITIALS 등) 아래 한 줄이면 충분
        try {
            return QuizPartType.valueOf(qt.name());
        } catch (IllegalArgumentException e) {
            // 혹시 상수명이 다르면 스위치로 보정
            switch (qt) {
                // case MULTI: return QuizPartType.CHOICE;
                // case TRUE_FALSE: return QuizPartType.OX;
                default: return QuizPartType.MIX;
            }
        }
    }
}
