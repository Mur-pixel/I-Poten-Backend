package com.cygnus.ipoten.quiz_wrongnote.service;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.quiz_question.entity.QuizChoice;
import com.cygnus.ipoten.quiz_question.entity.QuizQuestion;
import com.cygnus.ipoten.quiz_question.entity.QuizTextAnswer;
import com.cygnus.ipoten.quiz_question.entity.enums.QuestionType;
import com.cygnus.ipoten.quiz_question.repository.QuizChoiceRepository;
import com.cygnus.ipoten.quiz_question.repository.QuizTextAnswerRepository;
import com.cygnus.ipoten.quiz_wrongnote.controller.response_form.WrongNoteItemResponseForm;
import com.cygnus.ipoten.quiz_wrongnote.controller.response_form.WrongNoteListResponseForm;
import com.cygnus.ipoten.quiz_wrongnote.entity.QuizWrongNote;
import com.cygnus.ipoten.quiz_wrongnote.entity.enums.WrongNoteStatus;
import com.cygnus.ipoten.quiz_wrongnote.repository.QuizWrongNoteRepository;
import com.cygnus.ipoten.quiz_session_answer.entity.QuizSessionAnswer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizWrongNoteServiceImpl implements QuizWrongNoteService {

    private final QuizWrongNoteRepository quizWrongNoteRepository;
    private final QuizChoiceRepository quizChoiceRepository;
    private final QuizTextAnswerRepository quizTextAnswerRepository;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Override
    @Transactional
    public void saveWrongNotes(List<QuizSessionAnswer> answers, Long accountId) {
        if (answers == null || answers.isEmpty()) return;

        List<QuizWrongNote> toSave = new ArrayList<>();

        for (QuizSessionAnswer answer : answers) {
            if (answer == null || answer.isCorrect()) continue;

            var session = answer.getQuizSession();
            if (session == null) continue;

            Account account = session.getAccount();
            if (account == null) continue;

            if (accountId != null && !account.getId().equals(accountId)) continue;

            Long quizSessionId = session.getId();
            if (quizSessionId == null) continue;

            QuizQuestion quizQuestion = answer.getQuizQuestion();
            if (quizQuestion == null) continue;

            Instant submittedAt = answer.getSubmittedAt();

            Long submittedChoiceId = answer.getSubmittedChoiceId();
            String submittedChoiceText = answer.getSubmittedChoiceText();
            String submittedText = answer.getSubmittedText();

            boolean hasChoice =
                    (submittedChoiceId != null) ||
                            (submittedChoiceText != null && !submittedChoiceText.isBlank());

            boolean hasText =
                    (submittedText != null && !submittedText.isBlank());

            if (hasChoice) {
                toSave.add(QuizWrongNote.forChoice(
                        account,
                        quizSessionId,
                        quizQuestion,
                        submittedChoiceId,
                        submittedChoiceText,
                        submittedAt
                ));
                continue;
            }

            if (hasText) {
                toSave.add(QuizWrongNote.forText(
                        account,
                        quizSessionId,
                        quizQuestion,
                        submittedText,
                        submittedAt
                ));
            }
        }

        if (!toSave.isEmpty()) quizWrongNoteRepository.saveAll(toSave);
    }

    @Transactional(readOnly = true)
    public WrongNoteListResponseForm listWrongNotes(Long accountId, int page, int size, WrongNoteSearchCondition condition, boolean includeAnswers) {
        page = Math.max(0, page);
        size = Math.max(1, Math.min(100, size));

        var sort = (condition.sortKey() == WrongNoteSearchCondition.SortKey.OLDEST)
                ? Sort.by(Sort.Direction.ASC, "submittedAt", "id")
                : Sort.by(Sort.Direction.DESC, "submittedAt", "id");

        Pageable pageable = PageRequest.of(page, size, sort);

        Instant fromInstant = parseDateStart(condition.from(), KST);
        Instant toExclusive = parseDateEndExclusive(condition.to(), KST);

        QuestionType qType = parseQuestionTypeOrNull(condition.typeUpper());
        var diff = condition.difficultyLevel();
        var unresolvedOnly = condition.unresolvedOnly();
        var q = condition.q();

        Page<QuizWrongNote> result =
                quizWrongNoteRepository.searchWrongNotes(accountId, qType, diff, unresolvedOnly, WrongNoteStatus.UNRESOLVED, q, condition.sessionId(), fromInstant, toExclusive, pageable);

        List<QuizWrongNote> content = result.getContent();
        if (content.isEmpty()) {
            return WrongNoteListResponseForm.of(result.getNumber(), result.getSize(), result.getTotalElements(), List.of());
        }

        // 1) questionIds 수집
        List<Long> questionIds = content.stream()
                .map(w -> w.getQuizQuestion().getId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // 2) choices 전체 조회 후 questionId로 그룹핑
        final Map<Long, List<QuizChoice>> choicesByQuestionId =
                questionIds.isEmpty()
                        ? Map.of()
                        : quizChoiceRepository.findByQuestionIds(questionIds).stream()
                        .collect(Collectors.groupingBy(
                                c -> c.getQuizQuestion().getId(),
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));

        // 3) textAnswer 조회(questionId -> answerText)
        final Map<Long, String> correctTextAnswerByQuestionId =
                (!includeAnswers || questionIds.isEmpty())
                        ? Map.of()
                        : quizTextAnswerRepository.findByQuizQuestion_IdIn(questionIds).stream()
                        .collect(Collectors.toMap(
                                a -> a.getQuizQuestion().getId(),
                                QuizTextAnswer::getAnswerText,
                                (a, b) -> a
                        ));

        // 4) submittedChoiceText 없는 부분 보강: submittedChoiceId -> choiceText
        final Set<Long> submittedChoiceIds = content.stream()
                .map(QuizWrongNote::getSubmittedChoiceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        final Map<Long, String> choiceTextByChoiceId =
                submittedChoiceIds.isEmpty()
                        ? Map.of()
                        : quizChoiceRepository.findAllById(submittedChoiceIds).stream()
                        .collect(Collectors.toMap(
                                QuizChoice::getId,
                                QuizChoice::getChoiceText,
                                (a, b) -> a
                        ));

        // 5) 응답 매핑
        List<WrongNoteItemResponseForm> items = content.stream()
                .map(wn -> {
                    Long qid = wn.getQuizQuestion().getId();
                    List<QuizChoice> choices = choicesByQuestionId.getOrDefault(qid, List.of());

                    String myAnswer = resolveMyAnswer(wn, choiceTextByChoiceId);

                    String correctAnswer = null;
                    if (includeAnswers) {
                        // 선택형이면 choices에서 정답 찾기
                        Optional<QuizChoice> correctChoice = choices.stream().filter(QuizChoice::isAnswer).findFirst();
                        if (correctChoice.isPresent()) {
                            correctAnswer = correctChoice.get().getChoiceText();
                        } else {
                            // 텍스트형 정답
                            correctAnswer = correctTextAnswerByQuestionId.get(qid);
                        }
                    }

                    return WrongNoteItemResponseForm.from(wn, choices, myAnswer, correctAnswer, includeAnswers);
                })
                .toList();

        return WrongNoteListResponseForm.of(
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                items
        );
    }

    @Override
    @Transactional
    public void updateResolved(Long accountId, Long wrongNoteId, boolean resolved) {
        QuizWrongNote wn = quizWrongNoteRepository.findByIdAndAccount_Id(wrongNoteId, accountId)
                .orElseThrow(() -> new NoSuchElementException("Wrong note id " + wrongNoteId));

        wn.changeStatus(resolved ? WrongNoteStatus.RESOLVED : WrongNoteStatus.UNRESOLVED);
    }

    private String resolveMyAnswer(QuizWrongNote wn, Map<Long, String> choiceTextByChoiceId) {
        if (wn.getSubmittedText() != null && !wn.getSubmittedText().isBlank()) {
            return wn.getSubmittedText();
        }
        if (wn.getSubmittedChoiceText() != null && !wn.getSubmittedChoiceText().isBlank()) {
            return wn.getSubmittedChoiceText();
        }
        if (wn.getSubmittedChoiceId() != null) {
            return choiceTextByChoiceId.getOrDefault(wn.getSubmittedChoiceId(), null);
        }
        return null;
    }

    private Instant parseDateStart(LocalDate d, ZoneId zone) {
        if (d == null) return null;
        return d.atStartOfDay(zone).toInstant();
    }

    private Instant parseDateEndExclusive(LocalDate d, ZoneId zone) {
        if (d == null) return null;
        return d.plusDays(1).atStartOfDay(zone).toInstant();
    }

    private QuestionType parseQuestionTypeOrNull(String type) {
        if (type == null || type.isBlank()) return null;
        try {
            return QuestionType.valueOf(type.trim().toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }
}