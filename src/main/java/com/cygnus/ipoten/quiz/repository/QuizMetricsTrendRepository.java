package com.cygnus.ipoten.quiz.repository;

import com.cygnus.ipoten.quiz.entity.SessionAnswer;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

@org.springframework.stereotype.Repository
public interface QuizMetricsTrendRepository extends Repository<SessionAnswer, Long> {

    /* 1) 일자별 완료 세트 수 (세션 기준) */
    @Query("""
        select function('date', coalesce(s.submittedAt, s.startedAt)) as d,
               count(s) as submittedSets
        from UserQuizSession s
        where s.account.id = :accountId
          and s.sessionStatus = com.cygnus.ipoten.quiz.entity.enums.SessionStatus.SUBMITTED
          and coalesce(s.submittedAt, s.startedAt) between :from and :to
        group by function('date', coalesce(s.submittedAt, s.startedAt))
        order by function('date', coalesce(s.submittedAt, s.startedAt)) asc
    """)
    List<Object[]> countSubmittedSetsByDay(@Param("accountId") Long accountId,
                                           @Param("from") Instant from,
                                           @Param("to") Instant to);

    /* 2) 일자별 [totalSubmitted, retrySubmitted] (세션 기준) */
    @Query("""
        select function('date', coalesce(s.submittedAt, s.startedAt)) as d,
               count(s) as totalSubmitted,
               sum(case when (s.parentSession is not null
                              or s.sessionMode = com.cygnus.ipoten.quiz.entity.enums.SessionMode.WRONG_ONLY)
                        then 1 else 0 end) as retrySubmitted
        from UserQuizSession s
        where s.account.id = :accountId
          and s.sessionStatus = com.cygnus.ipoten.quiz.entity.enums.SessionStatus.SUBMITTED
          and coalesce(s.submittedAt, s.startedAt) between :from and :to
        group by function('date', coalesce(s.submittedAt, s.startedAt))
        order by function('date', coalesce(s.submittedAt, s.startedAt)) asc
    """)
    List<Object[]> countSubmittedAndRetryByDay(@Param("accountId") Long accountId,
                                               @Param("from") Instant from,
                                               @Param("to") Instant to);

    /* 3) 일자별 [sumCorrect, sumTotal] (답안 기준) */
    @Query("""
        select function('date', coalesce(s.submittedAt, s.startedAt)) as d,
               sum(case when sa.isCorrect = true then 1 else 0 end) as sumCorrect,
               count(sa) as sumTotal
        from SessionAnswer sa
        join sa.userQuizSession s
        where s.account.id = :accountId
          and s.sessionStatus = com.cygnus.ipoten.quiz.entity.enums.SessionStatus.SUBMITTED
          and coalesce(s.submittedAt, s.startedAt) between :from and :to
        group by function('date', coalesce(s.submittedAt, s.startedAt))
        order by function('date', coalesce(s.submittedAt, s.startedAt)) asc
    """)
    List<Object[]> sumCorrectAndTotalAnswersByDay(@Param("accountId") Long accountId,
                                                  @Param("from") Instant from,
                                                  @Param("to") Instant to);

    /* 4) 총 완료 세트 수 (세션 기준) */
    @Query("""
        select count(s)
        from UserQuizSession s
        where s.account.id = :accountId
          and s.sessionStatus = com.cygnus.ipoten.quiz.entity.enums.SessionStatus.SUBMITTED
    """)
    long countSubmittedSets(@Param("accountId") Long accountId);
}
