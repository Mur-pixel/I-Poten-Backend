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
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QuizSessionRepository extends JpaRepository<QuizSession, Long> {

    @Query("""
        select max(q.lastActivityAt)
        from QuizSession q
        where q.account.id = :accountId
          and q.deletedAt is null
    """)
    Optional<Instant> findLatestActivityAt(@Param("accountId") Long accountId);

    @Query("SELECT COUNT(u) FROM QuizSession u " +
            "WHERE u.account.id = :accountId " +
            "AND u.deletedAt is null " +
            "AND u.startedAt BETWEEN :start AND :end")
    long countMonthlyByAccountId(@Param("accountId") Long accountId,
                                 @Param("start") Instant start,
                                 @Param("end") Instant end);

    Optional<QuizSession> findByIdAndAccount_Id(Long id, Long accountId);

    Page<QuizSession> findByAccount_IdAndDeletedAtIsNull(Long accountId, Pageable pageable);

    Page<QuizSession> findByAccount_IdAndSessionStatusAndDeletedAtIsNull(Long accountId, SessionStatus sessionStatus, Pageable pageable);

    /* 1) 일자별 완료 세트 수 (세션 기준) */
    @Query("""
    select function('date', coalesce(s.submittedAt, s.startedAt)) as d,
           count(s) as submittedSets
    from QuizSession s
    where s.account.id = :accountId
      and s.deletedAt is null
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
          and s.deletedAt is null
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
          and s.deletedAt is null
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
          and s.deletedAt is null
          and s.sessionStatus = com.cygnus.ipoten.quiz_session.entity.enums.SessionStatus.SUBMITTED
    """)
    long countSubmittedSets(@Param("accountId") Long accountId);

    // 최대 attemptNo 조회
    @Query("""
      select coalesce(max(qs.attemptNo), 0)
      from QuizSession qs
      where qs.account.id = :accountId
        and qs.sourceKey = :sourceKey
        and qs.deletedAt is null
    """)
    int findMaxAttemptNoBySourceKey(@Param("accountId") Long accountId,
                                    @Param("sourceKey") String sourceKey);

    Optional<QuizSession> findTopByAccount_IdAndDailyIssueTypeAndSessionStatusInAndDeletedAtIsNullOrderByStartedAtDesc(
            Long accountId, String dailyIssueType, List<SessionStatus> statuses
    );

    boolean existsByAccount_IdAndDailyYmdAndDailyIssueTypeAndSessionStatusAndDeletedAtIsNull(
            Long accountId, LocalDate dailyYmd, String dailyIssueType, SessionStatus sessionStatus
    );

    Page<QuizSession> findByAccount_IdAndSessionStatusAndDeletedAtIsNullAndSubmittedAtAfterOrderBySubmittedAtDesc(
            Long accountId,
            SessionStatus sessionStatus,
            Instant submittedAtAfter,
            Pageable pageable
    );

    Optional<QuizSession> findTopByAccount_IdAndDailyYmdAndDailyIssueTypeAndDailyQuestionTypeAndSessionStatusInAndDeletedAtIsNullOrderByStartedAtDesc(
            Long accountId, LocalDate dailyYmd, String dailyIssueType, String dailyQuestionType, Collection<SessionStatus> statuses
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update QuizSession s
           set s.sessionStatus = com.cygnus.ipoten.quiz_session.entity.enums.SessionStatus.EXPIRED
         where s.account.id = :accountId
           and s.deletedAt is null
           and s.dailyYmd = :dailyYmd
           and s.dailyIssueType = :issueType
           and s.dailyQuestionType = :questionType
           and s.sessionStatus = com.cygnus.ipoten.quiz_session.entity.enums.SessionStatus.IN_PROGRESS
    """)
    int expireDailyInProgress(
            @Param("accountId") Long accountId,
            @Param("dailyYmd") LocalDate dailyYmd,
            @Param("issueType") String issueType,
            @Param("questionType") String questionType
    );

    /** 스케줄러용: IN_PROGRESS 중 3시간 이상 무활동 세션 만료 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    update QuizSession s
       set s.sessionStatus = com.cygnus.ipoten.quiz_session.entity.enums.SessionStatus.EXPIRED
     where s.sessionStatus = com.cygnus.ipoten.quiz_session.entity.enums.SessionStatus.IN_PROGRESS
       and coalesce(s.lastActivityAt, s.startedAt) < :cutoff
    """)
    int expireStaleInProgress(@Param("cutoff") Instant cutoff);

    /** 단건 만료: 해당 id 세션이 IN_PROGRESS일 때만 EXPIRED로 전환 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    update QuizSession s
       set s.sessionStatus = com.cygnus.ipoten.quiz_session.entity.enums.SessionStatus.EXPIRED
     where s.id = :id
       and s.sessionStatus = com.cygnus.ipoten.quiz_session.entity.enums.SessionStatus.IN_PROGRESS
    """)
    int expireIfInProgress(@Param("id") Long id);

    Optional<QuizSession> findByIdAndAccount_IdAndDeletedAtIsNull(Long id, Long accountId);

    Optional<QuizSession> findByIdAndDeletedAtIsNull(Long id);
}
