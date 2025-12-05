package com.cygnus.ipoten.quiz_session.repository;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.quiz_session.entity.QuizSession;
import com.cygnus.ipoten.quiz.entity.enums.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    Long account(Account account);

    Page<QuizSession> findByAccount_Id(Long accountId, Pageable pageable);
    Page<QuizSession> findByAccount_IdAndSessionStatus(Long accountId, SessionStatus sessionStatus, Pageable pageable);
}