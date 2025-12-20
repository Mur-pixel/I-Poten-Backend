package com.cygnus.ipoten.quiz_review.service;

import com.cygnus.ipoten.quiz_review.entity.QuizWrongNote;
import com.cygnus.ipoten.quiz_review.repository.QuizWrongNoteRepository;
import com.cygnus.ipoten.quiz_session_answer.entity.QuizSessionAnswer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizReviewServiceImpl implements QuizReviewService {

    private final QuizWrongNoteRepository quizWrongNoteRepository;

    @Override
    @Transactional
    public void saveWrongNotes(List<QuizSessionAnswer> answers, Long accountId) {
        for (QuizSessionAnswer a : answers) {
            if (a.isCorrect()) continue;

            QuizWrongNote review = QuizWrongNote.create(
                    a.getQuizSession().getAccount(),
                    a.getQuizQuestion(),
                    a.getQuizChoice(),
                    a.getQuizQuestion() != null ? a.getQuizQuestion().getExplanation() : null,
                    a.getSubmittedAt()
            );
            quizWrongNoteRepository.save(review);
        }
    }
}
