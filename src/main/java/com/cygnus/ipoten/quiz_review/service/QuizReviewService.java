package com.cygnus.ipoten.quiz_review.service;

import com.cygnus.ipoten.quiz_session_answer.entity.QuizSessionAnswer;

import java.util.List;

public interface QuizReviewService {
    void saveWrongNotes(List<QuizSessionAnswer> answers, Long accountId);
}
