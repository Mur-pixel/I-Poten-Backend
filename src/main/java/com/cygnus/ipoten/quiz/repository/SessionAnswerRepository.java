package com.cygnus.ipoten.quiz.repository;

import com.cygnus.ipoten.quiz.entity.SessionAnswer;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SessionAnswerRepository extends CrudRepository<SessionAnswer, Long> {

    // 원본 세션에서 오답만 저장
    List<SessionAnswer> findByUserQuizSession_IdAndIsCorrectFalse(Long sessionId);

    // 성능/중복 제거 위해 questionId만 뽑는 버전
    @Query("""
        select distinct sa.quizQuestion.id
        from SessionAnswer sa
        where sa.userQuizSession.id = :sessionId
        and sa.isCorrect = false
    """)
    List<Long> findWrongQuestionIds(@Param("sessionId") Long sessionId);

    @Query("""
        select distinct q.term.id
        from SessionAnswer sa
        join sa.quizQuestion q
        where sa.userQuizSession.account.id = :accountId
            and sa.submittedAt >= :since
    """)
    List<Long> findRecentTermIdsByAccountSince(Long accountId, LocalDateTime from);

    @Query("""
        select distinct c.choiceText
        from SessionAnswer sa
        join sa.quizChoice c
        where sa.userQuizSession.account.id = :accountId
            and sa.submittedAt >= :since
            and c.choiceText is not null
    """)
    List<String> findRecentChoiceTextsByAccountSince(Long accountId, LocalDateTime since);
    List<SessionAnswer> findByUserQuizSessionId(Long sessionId);
}
