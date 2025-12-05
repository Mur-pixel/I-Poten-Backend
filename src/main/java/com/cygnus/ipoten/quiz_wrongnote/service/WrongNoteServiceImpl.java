package com.cygnus.ipoten.quiz_wrongnote.service;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.account.repository.AccountRepository;
import com.cygnus.ipoten.quiz.entity.QuizSet;
import com.cygnus.ipoten.quiz.entity.enums.QuestionType;
import com.cygnus.ipoten.quiz.entity.enums.QuizSetType;
import com.cygnus.ipoten.quiz.entity.enums.SessionMode;
import com.cygnus.ipoten.quiz.entity.enums.SessionStatus;
import com.cygnus.ipoten.quiz_question.repository.QuizChoiceRepository;
import com.cygnus.ipoten.quiz.repository.QuizSetRepository;
import com.cygnus.ipoten.quiz.service.util.AnswerIndexPlanner;
import com.cygnus.ipoten.quiz_question.entity.QuizChoice;
import com.cygnus.ipoten.quiz_question.entity.QuizQuestion;
import com.cygnus.ipoten.quiz_question.repository.QuizQuestionRepository;
import com.cygnus.ipoten.quiz_session.entity.QuizSession;
import com.cygnus.ipoten.quiz_session.entity.SessionAnswer;
import com.cygnus.ipoten.quiz_session.repository.QuizSessionRepository;
import com.cygnus.ipoten.quiz_session.repository.SessionAnswerRepository;
import com.cygnus.ipoten.quiz_session.service.response.StartUserQuizSessionResponse;
import com.cygnus.ipoten.quiz_wrongnote.entity.WrongNote;
import com.cygnus.ipoten.quiz_wrongnote.repository.WrongNoteRepository;
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
public class WrongNoteServiceImpl implements WrongNoteService {

    private final QuizSessionRepository quizSessionRepository;
    private final SessionAnswerRepository sessionAnswerRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizSetRepository quizSetRepository;
    private final AccountRepository accountRepository;
    private final QuizChoiceRepository quizChoiceRepository;
    private final WrongNoteRepository wrongNoteRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public StartUserQuizSessionResponse startRetryWrongOnly(Long parentSessionId, Long accountId) {
        // 1) 권한/상태 검증 : 내 세션인지 확인
        QuizSession parent = quizSessionRepository.findByIdAndAccount_Id(parentSessionId, accountId)
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
        QuizSetType resolvedPartType = resolvePartTypeFromParentOrQuestions(
                parent,
                questions
        );

        // 3) 임시 QuizSet 생성(표시용)
        // 부모 세트/카테고리 정보를 재사용
        QuizSet parentQuizSet = parent.getQuizSet();
        String title = "[재도전] 틀린 문제만 다시 풉니다.";

        QuizSet retrySet = QuizSet.create(
                parentQuizSet != null ? parentQuizSet.getQuiz() : null,
                title,
                resolvedPartType,
                parentQuizSet != null ? parentQuizSet.getTermCategory() : null
        );
        quizSetRepository.save(retrySet);

        // 4) 새 세션 시작(부모-자식 연결 + 스냅샷 저장)
        Account account = accountRepository.getReferenceById(accountId);
        QuizSession child = new QuizSession();
        child.beginWithParent(
                account,
                retrySet,
                parent,
                SessionMode.WRONG_ONLY,
                (parent.getAttemptNo() == null ? 2 : parent.getAttemptNo() + 1),
                wrongQuestionId.size(),
                toJson(wrongQuestionId)
        );
        quizSessionRepository.save(child);

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
    @Transactional
    public void saveWrongNotes(List<SessionAnswer> answers, Long accountId) {
        for (SessionAnswer a : answers) {
            if (a.isCorrect()) continue;

            var opt = wrongNoteRepository
                    .findByAccount_IdAndQuizQuestion_Id(accountId, a.getQuizQuestion().getId());

            if (opt.isPresent()) {
                // 기존 노트 업데이트
                var wn = opt.get();
                wn.setQuizChoice(a.getQuizChoice());
                wn.setExplanation(
                        a.getQuizQuestion() != null ? a.getQuizQuestion().getExplanation() : null
                );
                wn.setSubmittedAt(a.getSubmittedAt());
            } else {
                // 신규 생성
                var wn = WrongNote.builder()
                        .account(a.getQuizSession().getAccount())
                        .quizQuestion(a.getQuizQuestion())
                        .quizChoice(a.getQuizChoice())
                        .submittedAt(a.getSubmittedAt())
                        .explanation(
                                a.getQuizQuestion() != null ? a.getQuizQuestion().getExplanation() : null
                        )
                        .build();
                wrongNoteRepository.save(wn);
            }
        }
    }

    /** 부모 세트의 partType을 우선 사용, 없으면 질문 타입 기반으로 유추 */
    private QuizSetType resolvePartTypeFromParentOrQuestions(QuizSession parent, List<QuizQuestion> questions) {
        QuizSet parentSet = parent.getQuizSet();

        if (parentSet != null && parentSet.getQuizSetType() != null) {
            return parentSet.getQuizSetType();
        }

        // 질문 타입 셋 추출
        Set<QuestionType> types = questions.stream()
                .map(QuizQuestion::getQuestionType)
                .collect(Collectors.toSet());

        if (types.isEmpty()) return QuizSetType.MIX; // 가드

        if (types.size() == 1) {
            // 단일 타입이면 그 타입으로 매핑
            QuestionType only = types.iterator().next();
            return mapToPartType(only);
        }
        // 혼합
        return QuizSetType.MIX;
    }

    /** QuestionType -> QuizPartType 매핑 (상수명이 같다면 valueOf로 충분) */
    private QuizSetType mapToPartType(QuestionType qt) {
        // 상수명이 동일한 경우 (예: CHOICE, OX, INITIALS 등) 아래 한 줄이면 충분
        try {
            return QuizSetType.valueOf(qt.name());
        } catch (IllegalArgumentException e) {
            // 혹시 상수명이 다르면 스위치로 보정
            switch (qt) {
                // case MULTI: return QuizPartType.CHOICE;
                // case TRUE_FALSE: return QuizPartType.OX;
                default: return QuizSetType.MIX;
            }
        }
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
            List<QuizChoice> choices,
            AnswerIndexPlanner planner,
            long seed
    ) {
        if (choices == null || choices.size() <= 1) {
            return choices;
        }

        int size = choices.size();

        // 1) 현재 리스트에서 정답 위치 찾기 (없으면 0번으로 간주)
        int currentAnswerIndex = 0;
        for (int i = 0; i < size; i++) {
            if (choices.get(i).isAnswer()) {
                currentAnswerIndex = i;
                break;
            }
        }

        QuizChoice answer = choices.get(currentAnswerIndex);

        // 2) AnswerIndexPlanner를 통해 "정답이 들어갈 목표 인덱스"를 뽑는다.
        int targetIndex = planner.nextIndex();   // AnswerIndexPlanner에 nextIndex()가 있다고 가정
        if (targetIndex < 0 || targetIndex >= size) {
            targetIndex = Math.floorMod(targetIndex, size);
        }

        // 3) 오답 리스트 만들기 + seed 기반 셔플
        List<QuizChoice> distractors = new ArrayList<>(choices);
        distractors.remove(currentAnswerIndex);

        Collections.shuffle(distractors, new Random(seed));

        // 4) 결과 리스트에 targetIndex에 정답을 넣고, 나머지 칸에 오답을 채워넣기
        List<QuizChoice> ordered = new ArrayList<>(Collections.nCopies(size, (QuizChoice) null));
        ordered.set(targetIndex, answer);

        int di = 0;
        for (int i = 0; i < size; i++) {
            if (i == targetIndex) continue;
            ordered.set(i, distractors.get(di++));
        }

        return ordered;
    }
}
