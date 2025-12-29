package com.cygnus.ipoten.quiz_wrongnote.service;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.quiz_question.entity.QuizQuestion;
import com.cygnus.ipoten.quiz_wrongnote.entity.QuizWrongNote;
import com.cygnus.ipoten.quiz_wrongnote.repository.QuizWrongNoteRepository;
import com.cygnus.ipoten.quiz_session_answer.entity.QuizSessionAnswer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizWrongNoteServiceImpl implements QuizWrongNoteService {

    private final QuizWrongNoteRepository quizWrongNoteRepository;

    @Override
    @Transactional
    public void saveWrongNotes(List<QuizSessionAnswer> answers, Long accountId) {

        if (answers == null || answers.isEmpty()) return;

        List<QuizWrongNote> toSave = new ArrayList<>();

        for (QuizSessionAnswer answer : answers) {
            if (answer == null || answer.isCorrect()) continue;

            Account account = answer.getQuizSession().getAccount();
            if (account == null) continue;

            if (accountId != null && !account.getId().equals(accountId)) continue;

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
                        quizQuestion,
                        submittedText,
                        submittedAt
                ));
            }
        }

        if (!toSave.isEmpty()) {
            quizWrongNoteRepository.saveAll(toSave);
        }
    }
}