package com.cygnus.ipoten.quiz_session_answer.repository;

import com.cygnus.ipoten.quiz_session_answer.entity.QuizSessionAnswer;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
