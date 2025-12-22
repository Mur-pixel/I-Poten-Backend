package com.cygnus.ipoten.quiz_session.repository;

import com.cygnus.ipoten.quiz_session.entity.QuizSession;
import com.cygnus.ipoten.quiz_session.entity.enums.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface QuizSessionRepository extends JpaRepository<QuizSession, Long> {
    long countByAccountId(Long accountId);

    @Query("SELECT COUNT(u) FROM QuizSession u " +
            "WHERE u.account.id = :accountId " +
            "AND u.startedAt BETWEEN :start AND :end")
    long countMonthlyByAccountId(@Param("accountId") Long accountId,
                                 @Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);

    Optional<QuizSession> findByIdAndAccount_Id(Long id, Long accountId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update QuizSession s set s.sessionStatus = 'EXPIRED' " +
            "where s.id = :id and s.sessionStatus <> 'SUBMITTED' ")
    int expireIfNotSubmitted(@Param("id") Long id);

    Page<QuizSession> findByAccount_Id(Long accountId, Pageable pageable);
    Page<QuizSession> findByAccount_IdAndSessionStatus(Long accountId, SessionStatus sessionStatus, Pageable pageable);

    /* 1) 일자별 완료 세트 수 (세션 기준) */
    @Query("""
        select function('date', coalesce(s.submittedAt, s.startedAt)) as d,
               count(s) as submittedSets
        from QuizSession s
        where s.account.id = :accountId
          and s.sessionStatus = com.cygnus.ipoten.quiz_session.entity.enums.SessionStatus.SUBMITTED
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
                              or s.sessionMode = com.cygnus.ipoten.quiz_session.entity.enums.SessionMode.WRONG_ONLY)
                        then 1 else 0 end) as retrySubmitted
        from QuizSession s
        where s.account.id = :accountId
          and s.sessionStatus = com.cygnus.ipoten.quiz_session.entity.enums.SessionStatus.SUBMITTED
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
        from QuizSessionAnswer sa
        join sa.quizSession s
        where s.account.id = :accountId
          and s.sessionStatus = com.cygnus.ipoten.quiz_session.entity.enums.SessionStatus.SUBMITTED
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
        from QuizSession s
        where s.account.id = :accountId
          and s.sessionStatus = com.cygnus.ipoten.quiz_session.entity.enums.SessionStatus.SUBMITTED
    """)
    long countSubmittedSets(@Param("accountId") Long accountId);

    // 최대 attemptNo 조회
    @Query("""
      select coalesce(max(qs.attemptNo), 0)
      from QuizSession qs
      where qs.account.id = :accountId
        and qs.sourceKey = :sourceKey
    """)
    int findMaxAttemptNoBySourceKey(@Param("accountId") Long accountId,
                                    @Param("sourceKey") String sourceKey);
}