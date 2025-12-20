package com.cygnus.ipoten.quiz_session_answer.service;

import com.cygnus.ipoten.quiz_question.entity.QuizChoice;
import com.cygnus.ipoten.quiz_question.entity.QuizQuestion;
import com.cygnus.ipoten.quiz_question.entity.QuizTextAnswer;
import com.cygnus.ipoten.quiz_question.entity.enums.QuestionType;
import com.cygnus.ipoten.quiz_question.repository.QuizChoiceRepository;
import com.cygnus.ipoten.quiz_question.repository.QuizQuestionRepository;
import com.cygnus.ipoten.quiz_question.repository.QuizTextAnswerRepository;
import com.cygnus.ipoten.quiz_review.service.QuizReviewService;
import com.cygnus.ipoten.quiz_session.entity.QuizSession;
import com.cygnus.ipoten.quiz_session.entity.enums.SeedMode;
import com.cygnus.ipoten.quiz_session.entity.enums.SessionMode;
import com.cygnus.ipoten.quiz_session_answer.entity.QuizSessionAnswer;
import com.cygnus.ipoten.quiz_session_answer.entity.QuizSessionTextAnswer;
import com.cygnus.ipoten.quiz_session_answer.repository.QuizSessionAnswerRepository;
import com.cygnus.ipoten.quiz_session.repository.QuizSessionRepository;
import com.cygnus.ipoten.quiz_session_answer.repository.QuizSessionTextAnswerRepository;
import com.cygnus.ipoten.quiz_set.entity.QuizSet;
import com.cygnus.ipoten.quiz_set.repository.QuizSetRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.account.repository.AccountRepository;
import com.cygnus.ipoten.quiz_session_answer.controller.request_form.SubmitQuizSessionRequestForm;
import com.cygnus.ipoten.quiz_session.controller.response_form.SubmitQuizSessionResponseForm;
import com.cygnus.ipoten.quiz_session.service.response.StartQuizSessionResponse;
import com.cygnus.ipoten.quiz_session_generator.service.util.AnswerIndexPlanner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
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
    private final QuizSessionTextAnswerRepository quizSessionTextAnswerRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final QuizReviewService quizReviewService;

    @Override
    @Transactional
    public StartQuizSessionResponse startFromQuizSet(
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

        int attemptNo = quizSessionRepository.findMaxAttemptNo(accountId, quizSetId) + 1;



        QuizSession session = new QuizSession();
        // 세션 시작
        session.begin(account, quizSet, SessionMode.FULL, attemptNo, questionIds.size(), toJson(questionIds), seedMode, resolvedSeed);

        quizSessionRepository.save(session);

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

        List<StartQuizSessionResponse.Item> items = questionIds.stream()
                .map(qid -> {
                    QuizQuestion q = qMap.get(qid);
                    List<QuizChoice> choices = new ArrayList<>(byQ.getOrDefault(qid, List.of()));

                    int optionCount = choices.size();
                    if (optionCount < 2) {
                        // 방어적 처리: 보기 수가 2 미만이면 그대로 반환
                        var options = choices.stream()
                                .map(c -> new StartQuizSessionResponse.Option(c.getId(), c.getChoiceText()))
                                .toList();
                        return new StartQuizSessionResponse.Item(
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
                            .map(c -> new StartQuizSessionResponse.Option(c.getId(), c.getChoiceText()))
                            .toList();

                    return new StartQuizSessionResponse.Item(
                            q.getId(),
                            q.getQuestionType(),
                            q.getQuestionText(),
                            null,
                            null,
                            options
                    );
                })
                .toList();

        return new StartQuizSessionResponse(session.getId(), quizSet.getId(), questionIds, items);
    }

    @Override
    @Transactional
    public SubmitQuizSessionResponseForm submitSession(Long sessionId, Long accountId, SubmitQuizSessionRequestForm requestForm) {

        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다."));
        if (!session.getAccount().getId().equals(accountId)) throw new SecurityException("세션 접근 권한이 없습니다.");
        if (session.getSubmittedAt() != null) throw new IllegalStateException("이미 제출된 세션입니다.");

        // ✅ 스냅샷 매칭(부분 제출 방지)
        Set<Long> snapshotQids = parseSnapshotIds(session.getQuestionsSnapshotJson());
        List<Long> submittedQids = requestForm.getAnswers().stream()
                .map(SubmitQuizSessionRequestForm.AnswerForm::getQuizQuestionId)
                .toList();

        if (!snapshotQids.equals(new HashSet<>(submittedQids))) {
            throw new IllegalArgumentException("제출 문항이 세션 스냅샷과 일치하지 않습니다.");
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

        // ✅ INITIALS 정답 텍스트 배치 조회 (PK = quizQuestionId)
        List<Long> initialsQids = submittedQids.stream()
                .filter(qid -> qMap.get(qid) != null && qMap.get(qid).getQuestionType() == QuestionType.INITIALS)
                .toList();

        Map<Long, String> expectedTextByQid = quizTextAnswerRepository.findAllById(initialsQids).stream()
                .collect(Collectors.toMap(QuizTextAnswer::getId, QuizTextAnswer::getAnswerText));

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

        List<QuizSessionAnswer> choiceToSave = new ArrayList<>();
        List<QuizSessionTextAnswer> textToSave = new ArrayList<>();
        List<SubmitQuizSessionResponseForm.Item> details = new ArrayList<>();

        for (var a : requestForm.getAnswers()) {
            QuizQuestion q = Optional.ofNullable(qMap.get(a.getQuizQuestionId()))
                    .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 문제입니다: " + a.getQuizQuestionId()));

            // ===== INITIALS =====
            if (q.getQuestionType() == QuestionType.INITIALS) {
                String submitted = Optional.ofNullable(a.getTextAnswer()).orElse("").trim();
                if (submitted.isBlank()) throw new IllegalArgumentException("주관식 답변이 비었습니다: " + q.getId());

                String expected = Optional.ofNullable(expectedTextByQid.get(q.getId()))
                        .orElseThrow(() -> new IllegalArgumentException("주관식 정답이 등록되지 않았습니다: " + q.getId()))
                        .trim();

                boolean isCorrect = submitted.equals(expected);
                if (isCorrect) correctCount++;

                textToSave.add(QuizSessionTextAnswer.create(session, q, submitted, isCorrect, now));

                // response details: 선택형 필드들은 null, 텍스트는 채워서 내려주기
                details.add(new SubmitQuizSessionResponseForm.Item(
                        q.getId(),
                        null,
                        null,
                        null,
                        null,
                        submitted,
                        expected,
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

            choiceToSave.add(new QuizSessionAnswer(session, q, c, now, isCorrect));

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

        quizSessionAnswerRepository.saveAll(choiceToSave);
        quizSessionTextAnswerRepository.saveAll(textToSave);

        Long elapsedMs = requestForm.getElapsedMs();
        if (elapsedMs == null && session.getStartedAt() != null) {
            elapsedMs = Duration.between(session.getStartedAt(), now).toMillis();
        }

        session.submit(correctCount, elapsedMs);
        quizSessionRepository.save(session);

        // ✅ 일단 선택형 오답노트는 그대로 저장
        quizReviewService.saveWrongNotes(choiceToSave, accountId);

        // TODO: INITIALS 오답노트도 저장하려면 QuizReviewService에 textToSave용 메서드 추가

        int total = submittedQids.size();
        return new SubmitQuizSessionResponseForm(
                session.getId(),
                total,
                correctCount,
                elapsedMs,
                details
        );
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
}
