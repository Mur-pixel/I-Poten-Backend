package com.cygnus.iptn.quiz_session_answer.repository;

import com.cygnus.iptn.quiz_session_answer.entity.QuizSessionAnswer;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface QuizSessionAnswerRepository extends CrudRepository<QuizSessionAnswer, Long> {

    // 원본 세션에서 오답만 저장
    List<QuizSessionAnswer> findByQuizSession_IdAndIsCorrectFalse(Long sessionId);

    // 성능/중복 제거 위해 questionId만 뽑는 버전
    @Query("""
        select distinct sa.quizQuestion.id
        from QuizSessionAnswer sa
        where sa.quizSession.id = :sessionId
          and sa.isCorrect = false
    """)
    List<Long> findWrongQuestionIds(@Param("sessionId") Long sessionId);

    // 여러 세션 오답 row 조회
    interface WrongRow {
        Long getSessionId();
        Long getQuestionId();
    }

    @Query("""
        select a.quizSession.id as sessionId, a.quizQuestion.id as questionId
        from QuizSessionAnswer a
        where a.quizSession.id in :sessionIds
          and a.isCorrect = false
        order by a.quizSession.submittedAt desc, a.id asc
    """)
    List<WrongRow> findWrongRowsOrdered(@Param("sessionIds") List<Long> sessionIds);

    @Query("""
        select distinct q.term.id
        from QuizSessionAnswer sa
        join sa.quizQuestion q
        where sa.quizSession.account.id = :accountId
          and sa.submittedAt >= :since
    """)
    List<Long> findRecentTermIdsByAccountSince(
            @Param("accountId") Long accountId,
            @Param("since") Instant since
    );

    @Query("""
        select distinct sa.submittedChoiceText
        from QuizSessionAnswer sa
        where sa.quizSession.account.id = :accountId
          and sa.submittedAt >= :since
          and sa.submittedChoiceText is not null
    """)
    List<String> findRecentChoiceTextsByAccountSince(
            @Param("accountId") Long accountId,
            @Param("since") Instant since
    );
    List<QuizSessionAnswer> findByQuizSession_Id(Long sessionId);
}
