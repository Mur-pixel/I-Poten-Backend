package com.cygnus.ipoten.quiz_wrongnote.service;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.quiz_question.entity.QuizChoice;
import com.cygnus.ipoten.quiz_question.entity.QuizQuestion;
import com.cygnus.ipoten.quiz_question.entity.QuizTextAnswer;
import com.cygnus.ipoten.quiz_question.entity.enums.QuestionType;
import com.cygnus.ipoten.quiz_question.repository.QuizChoiceRepository;
import com.cygnus.ipoten.quiz_question.repository.QuizTextAnswerRepository;
import com.cygnus.ipoten.quiz_session_answer.entity.QuizSessionAnswer;
import com.cygnus.ipoten.quiz_wrongnote.controller.response_form.WrongNoteItemResponseForm;
import com.cygnus.ipoten.quiz_wrongnote.controller.response_form.WrongNoteListResponseForm;
import com.cygnus.ipoten.quiz_wrongnote.entity.QuizWrongNote;
import com.cygnus.ipoten.quiz_wrongnote.entity.enums.WrongNoteStatus;
import com.cygnus.ipoten.quiz_wrongnote.repository.QuizWrongNoteRepository;
import com.cygnus.ipoten.quiz_wrongnote.repository.projection.WrongNoteQuestionSummaryView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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

        Pageable pageable = PageRequest.of(page, size);

        Instant fromInstant = parseDateStart(condition.from(), KST);
        Instant toExclusive = parseDateEndExclusive(condition.to(), KST);

        QuestionType qType = parseQuestionTypeOrNull(condition.typeUpper());
        var diff = condition.difficultyLevel();
        var unresolvedOnly = condition.unresolvedOnly();
        var q = condition.q();

        Page<WrongNoteQuestionSummaryView> result = switch (condition.sortKey()) {
            case OLDEST -> quizWrongNoteRepository.searchWrongNoteQuestionSummariesOldest(
                    accountId, qType, diff, unresolvedOnly, WrongNoteStatus.UNRESOLVED, q, condition.sessionId(), fromInstant, toExclusive, pageable);
            case MOST_WRONG -> quizWrongNoteRepository.searchWrongNoteQuestionSummariesMostWrong(
                    accountId, qType, diff, unresolvedOnly, WrongNoteStatus.UNRESOLVED, q, condition.sessionId(), fromInstant, toExclusive, pageable);
            case RECENT -> quizWrongNoteRepository.searchWrongNoteQuestionSummariesRecent(
                    accountId, qType, diff, unresolvedOnly, WrongNoteStatus.UNRESOLVED, q, condition.sessionId(), fromInstant, toExclusive, pageable);
        };

        List<WrongNoteQuestionSummaryView> summaries = result.getContent();
        if (summaries.isEmpty()) {
            return WrongNoteListResponseForm.of(result.getNumber(), result.getSize(), result.getTotalElements(), List.of());
        }

        List<Long> representativeIds = summaries.stream()
                .map(WrongNoteQuestionSummaryView::getRepresentativeWrongNoteId)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, WrongNoteQuestionSummaryView> summaryByRepresentativeId = summaries.stream()
                .filter(summary -> summary.getRepresentativeWrongNoteId() != null)
                .collect(Collectors.toMap(
                        WrongNoteQuestionSummaryView::getRepresentativeWrongNoteId,
                        summary -> summary,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        Map<Long, QuizWrongNote> wrongNoteById = quizWrongNoteRepository.findAllWithDetailsByIdIn(representativeIds).stream()
                .collect(Collectors.toMap(QuizWrongNote::getId, wrongNote -> wrongNote));

        List<QuizWrongNote> content = representativeIds.stream()
                .map(wrongNoteById::get)
                .filter(Objects::nonNull)
                .toList();

        List<Long> questionIds = content.stream()
                .map(wrongNote -> wrongNote.getQuizQuestion().getId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        final Map<Long, List<QuizChoice>> choicesByQuestionId =
                questionIds.isEmpty()
                        ? Map.of()
                        : quizChoiceRepository.findByQuestionIds(questionIds).stream()
                        .collect(Collectors.groupingBy(
                                choice -> choice.getQuizQuestion().getId(),
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));

        final Map<Long, String> correctTextAnswerByQuestionId =
                (!includeAnswers || questionIds.isEmpty())
                        ? Map.of()
                        : quizTextAnswerRepository.findByQuizQuestion_IdIn(questionIds).stream()
                        .collect(Collectors.toMap(
                                answer -> answer.getQuizQuestion().getId(),
                                QuizTextAnswer::getAnswerText,
                                (left, right) -> left
                        ));

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
                                (left, right) -> left
                        ));

        List<WrongNoteItemResponseForm> items = content.stream()
                .map(wrongNote -> {
                    Long questionId = wrongNote.getQuizQuestion().getId();
                    List<QuizChoice> choices = choicesByQuestionId.getOrDefault(questionId, List.of());
                    WrongNoteQuestionSummaryView summary = summaryByRepresentativeId.get(wrongNote.getId());

                    String myAnswer = resolveMyAnswer(wrongNote, choiceTextByChoiceId);
                    String correctAnswer = null;

                    if (includeAnswers) {
                        Optional<QuizChoice> correctChoice = choices.stream().filter(QuizChoice::isAnswer).findFirst();
                        if (correctChoice.isPresent()) {
                            correctAnswer = correctChoice.get().getChoiceText();
                        } else {
                            correctAnswer = correctTextAnswerByQuestionId.get(questionId);
                        }
                    }

                    boolean resolved = summary == null || summary.getUnresolvedCount() == null || summary.getUnresolvedCount() == 0L;
                    Long wrongCount = summary == null || summary.getWrongCount() == null ? 1L : summary.getWrongCount();

                    return WrongNoteItemResponseForm.from(
                            wrongNote,
                            choices,
                            myAnswer,
                            correctAnswer,
                            wrongCount,
                            resolved,
                            includeAnswers
                    );
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
        QuizWrongNote wrongNote = quizWrongNoteRepository.findByIdAndAccount_Id(wrongNoteId, accountId)
                .orElseThrow(() -> new NoSuchElementException("Wrong note id " + wrongNoteId));

        quizWrongNoteRepository.updateStatusByAccountIdAndQuestionId(
                accountId,
                wrongNote.getQuizQuestion().getId(),
                resolved ? WrongNoteStatus.RESOLVED : WrongNoteStatus.UNRESOLVED
        );
    }

    @Override
    @Transactional
    public void deleteWrongNote(Long accountId, Long wrongNoteId) {
        QuizWrongNote wrongNote = quizWrongNoteRepository.findByIdAndAccount_Id(wrongNoteId, accountId)
                .orElseThrow(() -> new NoSuchElementException("Wrong note id " + wrongNoteId));

        long deleted = quizWrongNoteRepository.deleteByAccount_IdAndQuizQuestion_Id(accountId, wrongNote.getQuizQuestion().getId());
        if (deleted == 0) {
            throw new NoSuchElementException("Wrong note id " + wrongNoteId);
        }
    }

    @Override
    @Transactional
    public void deleteWrongNotesBulk(Long accountId, List<Long> wrongNoteIds) {
        List<Long> ids = wrongNoteIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (ids.isEmpty()) return;

        List<Long> questionIds = quizWrongNoteRepository.findByIdInAndAccount_Id(ids, accountId).stream()
                .map(quizWrongNote -> quizWrongNote.getQuizQuestion().getId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (questionIds.isEmpty()) return;

        quizWrongNoteRepository.deleteByAccount_IdAndQuizQuestion_IdIn(accountId, questionIds);
    }

    private String resolveMyAnswer(QuizWrongNote wrongNote, Map<Long, String> choiceTextByChoiceId) {
        if (wrongNote.getSubmittedText() != null && !wrongNote.getSubmittedText().isBlank()) {
            return wrongNote.getSubmittedText();
        }
        if (wrongNote.getSubmittedChoiceText() != null && !wrongNote.getSubmittedChoiceText().isBlank()) {
            return wrongNote.getSubmittedChoiceText();
        }
        if (wrongNote.getSubmittedChoiceId() != null) {
            return choiceTextByChoiceId.getOrDefault(wrongNote.getSubmittedChoiceId(), null);
        }
        return null;
    }

    private Instant parseDateStart(LocalDate date, ZoneId zone) {
        if (date == null) return null;
        return date.atStartOfDay(zone).toInstant();
    }

    private Instant parseDateEndExclusive(LocalDate date, ZoneId zone) {
        if (date == null) return null;
        return date.plusDays(1).atStartOfDay(zone).toInstant();
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
