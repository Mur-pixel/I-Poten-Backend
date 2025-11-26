package com.cygnus.ipoten.quiz.repository;

import com.cygnus.ipoten.quiz.entity.UserQuizSession;
import com.cygnus.ipoten.quiz.entity.enums.QuizSetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;

public interface UserQuizSessionTimelineRepositoryCustom {
    Page<UserQuizSession> findTimelinePage(Long accountId, String q, QuizSetType part, Pageable pageable);
    long countSubmitted(Long accountId);
    long countSubmittedRetry(Long accountId);
    long sumTotalQuestionsOfSubmitted(Long accountId);
    long sumCorrectAnswersOfSubmitted(Long accountId);
    List<Object[]> findRecentRaw(Long accountId, int limit);
    List<Object[]> countCorrectBySessionIds(Collection<Long> sessionIds);
    List<Object[]> countAnswersBySessionIds(Collection<Long> sessionIds);
}
