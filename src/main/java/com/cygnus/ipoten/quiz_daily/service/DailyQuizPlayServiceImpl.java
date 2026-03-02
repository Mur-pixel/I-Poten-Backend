package com.cygnus.ipoten.quiz_daily.service;

import com.cygnus.ipoten.quiz_daily.controller.request_form.CheckDailyQuestionRequestForm;
import com.cygnus.ipoten.quiz_daily.controller.response_form.CheckDailyQuestionResponseForm;
import com.cygnus.ipoten.quiz_daily.entity.DailyQuizAnswer;
import com.cygnus.ipoten.quiz_daily.repository.DailyQuizAnswerRepository;
import com.cygnus.ipoten.quiz_question.entity.QuizChoice;
import com.cygnus.ipoten.quiz_question.entity.QuizQuestion;
import com.cygnus.ipoten.quiz_question.entity.QuizTextAnswer;
import com.cygnus.ipoten.quiz_question.entity.enums.QuestionType;
import com.cygnus.ipoten.quiz_question.repository.QuizChoiceRepository;
import com.cygnus.ipoten.quiz_question.repository.QuizQuestionRepository;
import com.cygnus.ipoten.quiz_question.repository.QuizTextAnswerRepository;
import com.cygnus.ipoten.quiz_session.entity.QuizSession;
import com.cygnus.ipoten.quiz_session.entity.enums.SessionStatus;
import com.cygnus.ipoten.quiz_session.repository.QuizSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyQuizPlayServiceImpl implements DailyQuizPlayService {

    private final QuizSessionRepository quizSessionRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizChoiceRepository quizChoiceRepository;
    private final QuizTextAnswerRepository quizTextAnswerRepository;
    private final DailyQuizAnswerRepository dailyQuizAnswerRepository;
    private final DailyQuizAnswerQueryService dailyQuizAnswerQueryService;

    /**
     * 데일리 문항 즉시 채점
     * - (sessionId, questionId) 기준으로 idempotent: 이미 채점된 문항이면 기존 결과 그대로 반환
     * - 동시 클릭/중복 요청 대비: unique 제약 위반 시 재조회하여 반환
     */
    @Override
    @Transactional
    public CheckDailyQuestionResponseForm checkQuestion(
            Long sessionId,
            Long questionId,
            Long accountId,
            CheckDailyQuestionRequestForm requestForm
    ) {
        // 1) 세션 및 소유권 검증
        QuizSession quizSession = quizSessionRepository
                .findByIdAndAccount_IdAndDeletedAtIsNull(sessionId, accountId)
                .orElseThrow(() -> new NoSuchElementException("세션을 찾을 수 없습니다."));

        // 2) 데일리 세션 검증
        if (quizSession.getDailyIssueType() == null || quizSession.getDailyIssueType().isBlank()) {
            throw new IllegalArgumentException("데일리 세션이 아닙니다.");
        }

        // 3) 기존 답안 있으면: 같으면 반환 / 다르면 재채점 후 업데이트
        var existingOpt = dailyQuizAnswerRepository.findBySessionIdAndQuestionId(sessionId, questionId);
        if (existingOpt.isPresent()) {
            DailyQuizAnswer existing = existingOpt.get();

            // 진행 중인지(재시도도 IN_PROGRESS에서만 허용할지 정책)
            if (quizSession.getSessionStatus() != SessionStatus.IN_PROGRESS) {
                throw new IllegalStateException("진행 중인 세션이 아닙니다.");
            }

            QuizQuestion quizQuestion = quizQuestionRepository.findById(questionId)
                    .orElseThrow(() -> new NoSuchElementException("문항을 찾을 수 없습니다."));
            QuestionType questionType = quizQuestion.getQuestionType();

            NextInfo nextInfo = computeNextInfo(quizSession, questionId);

            if (questionType == QuestionType.INITIALS) {
                String incoming = requestForm.getAnswerText();
                if (incoming == null || incoming.isBlank()) throw new IllegalArgumentException("answerText가 필요합니다.");

                // 같은 답이면 그대로 반환 (idempotent)
                if (normalize(incoming).equals(normalize(existing.getSubmittedText()))) {
                    return new CheckDailyQuestionResponseForm(
                            sessionId, questionId, existing.getQuestionType(), existing.isCorrect(),
                            existing.getCorrectChoiceId(), safe(existing.getAnswerText()), safe(existing.getExplanation()),
                            nextInfo.nextQuestionId(), nextInfo.lastQuestion()
                    );
                }

                // 답이 달라졌으면 재채점 후 update
                QuizTextAnswer ta = quizTextAnswerRepository.findByQuizQuestion_IdIn(List.of(questionId))
                        .stream().findFirst()
                        .orElseThrow(() -> new IllegalStateException("초성 정답 텍스트를 찾을 수 없습니다."));

                String textAnswer = safe(ta.getAnswerText());
                boolean correct = normalize(incoming).equals(normalize(textAnswer));

                // DailyQuizAnswer에 updateInitialsAttempt 메서드 추가 권장
                existing.updateInitialsAttempt(
                        incoming,
                        correct,
                        textAnswer,
                        safe(quizQuestion.getExplanation())
                );
                dailyQuizAnswerRepository.save(existing);

                return new CheckDailyQuestionResponseForm(
                        sessionId, questionId, questionType, correct,
                        null, safe(textAnswer), safe(quizQuestion.getExplanation()),
                        nextInfo.nextQuestionId(), nextInfo.lastQuestion()
                );
            }

            // CHOICE/OX: choiceId 기준
            if (questionType == QuestionType.CHOICE || questionType == QuestionType.OX) {
                Long incoming = requestForm.getChoiceId();
                if (incoming == null) throw new IllegalArgumentException("choiceId가 필요합니다.");

                // 같은 선택이면 그대로 반환 (idempotent)
                if (Objects.equals(existing.getChosenChoiceId(), incoming)) {
                    return new CheckDailyQuestionResponseForm(
                            sessionId, questionId, existing.getQuestionType(), existing.isCorrect(),
                            existing.getCorrectChoiceId(), safe(existing.getAnswerText()), safe(existing.getExplanation()),
                            nextInfo.nextQuestionId(), nextInfo.lastQuestion()
                    );
                }

                // 선택이 달라졌으면 재채점 + UPDATE
                QuizChoice correctChoice = quizChoiceRepository.findCorrectChoices(List.of(questionId))
                        .stream().findFirst()
                        .orElseThrow(() -> new IllegalStateException("정답 보기를 찾을 수 없습니다."));

                boolean correct = Objects.equals(correctChoice.getId(), incoming);

                existing.updateChoiceAttempt(
                        incoming,
                        correct,
                        correctChoice.getId(),
                        safe(correctChoice.getChoiceText()),
                        safe(quizQuestion.getExplanation())
                );
                dailyQuizAnswerRepository.save(existing);

                return new CheckDailyQuestionResponseForm(
                        sessionId, questionId, questionType, correct,
                        correctChoice.getId(), safe(correctChoice.getChoiceText()), safe(quizQuestion.getExplanation()),
                        nextInfo.nextQuestionId(), nextInfo.lastQuestion()
                );
            }

            // INITIALS도 동일: submittedText가 같으면 return, 다르면 재채점 후 update
        }

        // 4) 아직 답안 없을 때만 상태 체크 (복습형: 기존 기록은 상태 상관없이 보여줄 수 있음)
        if (quizSession.getSessionStatus() != SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("진행 중인 세션이 아닙니다.");
        }

        // 5) 문제 조회
        QuizQuestion quizQuestion = quizQuestionRepository.findById(questionId)
                .orElseThrow(() -> new NoSuchElementException("문항을 찾을 수 없습니다."));

        QuestionType questionType = quizQuestion.getQuestionType();

        // 6) next 계산(응답에 포함)
        NextInfo nextInfo = computeNextInfo(quizSession, questionId);

        // 7) 타입별 채점
        boolean correct;
        String answerText;                       // 정답 텍스트(보기 텍스트 or 정답 텍스트)
        String explanation = safe(quizQuestion.getExplanation());
        Long correctChoiceId = null;

        // 저장용: 사용자가 제출한 값
        Long chosenChoiceId = null;
        String submittedText = null;

        switch (questionType) {
            case CHOICE, OX -> {
                if (requestForm.getChoiceId() == null) {
                    throw new IllegalArgumentException("choiceId가 필요합니다.");
                }

                chosenChoiceId = requestForm.getChoiceId();

                List<QuizChoice> correctChoices = quizChoiceRepository.findCorrectChoices(List.of(questionId));
                if (correctChoices.isEmpty()) {
                    throw new IllegalStateException("정답 보기를 찾을 수 없습니다.");
                }

                QuizChoice correctChoice = correctChoices.get(0);
                correctChoiceId = correctChoice.getId();

                answerText = safe(correctChoice.getChoiceText());
                correct = Objects.equals(correctChoice.getId(), chosenChoiceId);
            }

            case INITIALS -> {
                if (requestForm.getAnswerText() == null || requestForm.getAnswerText().isBlank()) {
                    throw new IllegalArgumentException("answerText가 필요합니다.");
                }

                submittedText = requestForm.getAnswerText();

                List<QuizTextAnswer> quizTextAnswers =
                        quizTextAnswerRepository.findByQuizQuestion_IdIn(List.of(questionId));

                if (quizTextAnswers.isEmpty()) {
                    throw new IllegalStateException("초성 정답 텍스트를 찾을 수 없습니다.");
                }

                QuizTextAnswer ta = quizTextAnswers.get(0);
                String textAnswer = safe(ta.getAnswerText());

                answerText = safe(textAnswer);
                correct = normalize(submittedText).equals(normalize(textAnswer));
            }

            default -> throw new IllegalStateException("지원하지 않는 문제 타입입니다: " + questionType);
        }

        // 8) 답안 저장 (동시 요청 대비: unique 제약 위반 시 기존 결과 재조회)
        try {
            dailyQuizAnswerRepository.save(
                    DailyQuizAnswer.of(
                            sessionId, questionId, accountId, questionType,
                            correct, chosenChoiceId, correctChoiceId,
                            submittedText, answerText, explanation
                    )
            );
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateKey(e)) {
                DailyQuizAnswer a = dailyQuizAnswerQueryService.getBySessionAndQuestion(sessionId, questionId);
                NextInfo ni = computeNextInfo(quizSession, questionId);

                return new CheckDailyQuestionResponseForm(
                        sessionId, questionId, a.getQuestionType(), a.isCorrect(),
                        a.getCorrectChoiceId(), safe(a.getAnswerText()), safe(a.getExplanation()),
                        ni.nextQuestionId(), ni.lastQuestion()
                );
            }
            throw e;
        }

        // 9) 응답
        return new CheckDailyQuestionResponseForm(
                sessionId,
                questionId,
                questionType,
                correct,
                correctChoiceId,
                safe(answerText),
                safe(explanation),
                nextInfo.nextQuestionId(),
                nextInfo.lastQuestion()
        );
    }

    /**
     * 세션 문항 순서를 기준으로 다음 문항/마지막 여부 계산
     * - includeAnswers=false로 가볍게 조회
     */
    private NextInfo computeNextInfo(QuizSession session, Long questionId) {

        List<Long> orderedIds = session.getSnapshotQuestionIds();
        if (orderedIds == null || orderedIds.isEmpty()) {
            throw new IllegalStateException("세션 문항을 찾을 수 없습니다.");
        }

        int idx = orderedIds.indexOf(questionId);
        if (idx < 0) {
            throw new NoSuchElementException("해당 문항은 세션에 포함되어 있지 않습니다.");
        }

        Long nextQuestionId = (idx + 1 < orderedIds.size()) ? orderedIds.get(idx + 1) : null;
        boolean lastQuestion = (nextQuestionId == null);

        return new NextInfo(nextQuestionId, lastQuestion);
    }

    private record NextInfo(Long nextQuestionId, boolean lastQuestion) {}

    private String normalize(String s) {
        if (s == null) return "";
        return s.trim().replaceAll("\\s+", "").toLowerCase();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private boolean isDuplicateKey(DataIntegrityViolationException e) {
        Throwable root = e.getMostSpecificCause();
        if (root == null) return false;
        String msg = root.getMessage();
        return msg != null && msg.contains("Duplicate entry");
    }
}