package com.cygnus.ipoten.interview.repository;

import com.cygnus.ipoten.interview.entity.Interview;
import com.cygnus.ipoten.interview.entity.InterviewType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 어드민 면접 조회 전용 쿼리.
 * 파라미터는 모두 non-null 로 들어와야 한다. (필터 미지정은 전체 범위로 치환해서 호출)
 */
public interface AdminInterviewQueryRepository extends JpaRepository<Interview, Long> {

    /** Level 1 — 필터에 부합하는 회원 id 페이지 (accountId asc 커서) */
    @Query("""
            SELECT DISTINCT ap.account.id
            FROM com.cygnus.ipoten.accountProfile.entity.AccountProfile ap
            WHERE EXISTS (
                SELECT 1 FROM Interview i
                WHERE i.account.id = ap.account.id
                  AND i.deletedAt IS NULL
                  AND i.interviewType IN :types
                  AND i.createdAt >= :startDate
                  AND i.createdAt <= :endDate
            )
            AND (LOWER(ap.email) LIKE :qLike OR LOWER(ap.nickname) LIKE :qLike)
            AND (:lastAccountId IS NULL OR ap.account.id > :lastAccountId)
            ORDER BY ap.account.id ASC
            """)
    List<Long> findCandidateAccountIds(
            @Param("lastAccountId") Long lastAccountId,
            @Param("qLike") String qLike,
            @Param("types") List<InterviewType> types,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    /** Level 1 — 주어진 회원 집합에 대한 건수/최근 면접시각 집계 */
    @Query("""
            SELECT i.account.id AS accountId,
                   COUNT(i)    AS cnt,
                   MAX(i.createdAt) AS last
            FROM Interview i
            WHERE i.account.id IN :accountIds
              AND i.deletedAt IS NULL
              AND i.interviewType IN :types
              AND i.createdAt >= :startDate
              AND i.createdAt <= :endDate
            GROUP BY i.account.id
            """)
    List<InterviewAggregateRow> aggregateByAccountIds(
            @Param("accountIds") List<Long> accountIds,
            @Param("types") List<InterviewType> types,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /** Level 1 — 주어진 회원 집합의 면접 유형 distinct 목록 */
    @Query("""
            SELECT DISTINCT i.account.id AS accountId, i.interviewType AS type
            FROM Interview i
            WHERE i.account.id IN :accountIds
              AND i.deletedAt IS NULL
              AND i.interviewType IN :types
              AND i.createdAt >= :startDate
              AND i.createdAt <= :endDate
            """)
    List<InterviewTypeRow> typesByAccountIds(
            @Param("accountIds") List<Long> accountIds,
            @Param("types") List<InterviewType> types,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /** Level 2 — 한 회원의 면접 이력 (id desc 커서) */
    @Query("""
            SELECT i FROM Interview i
            LEFT JOIN FETCH i.intervieweeProfile p
            WHERE i.account.id = :accountId
              AND i.deletedAt IS NULL
              AND i.interviewType IN :types
              AND i.createdAt >= :startDate
              AND i.createdAt <= :endDate
              AND (:lastInterviewId IS NULL OR i.id < :lastInterviewId)
            ORDER BY i.id DESC
            """)
    List<Interview> findInterviewHistoryPage(
            @Param("accountId") Long accountId,
            @Param("lastInterviewId") Long lastInterviewId,
            @Param("types") List<InterviewType> types,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    /** Level 2 — 요약 스트립용 total/last (필터 반영) */
    @Query("""
            SELECT COUNT(i) AS cnt, MAX(i.createdAt) AS last
            FROM Interview i
            WHERE i.account.id = :accountId
              AND i.deletedAt IS NULL
              AND i.interviewType IN :types
              AND i.createdAt >= :startDate
              AND i.createdAt <= :endDate
            """)
    InterviewSummaryRow summaryForAccount(
            @Param("accountId") Long accountId,
            @Param("types") List<InterviewType> types,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /** Level 3 — interview + profile 로딩 */
    @Query("""
            SELECT i FROM Interview i
            LEFT JOIN FETCH i.intervieweeProfile p
            LEFT JOIN FETCH i.account a
            WHERE i.id = :interviewId
              AND i.deletedAt IS NULL
            """)
    Interview findDetail(@Param("interviewId") Long interviewId);

    interface InterviewAggregateRow {
        Long getAccountId();
        Long getCnt();
        LocalDateTime getLast();
    }

    interface InterviewTypeRow {
        Long getAccountId();
        InterviewType getType();
    }

    interface InterviewSummaryRow {
        Long getCnt();
        LocalDateTime getLast();
    }
}
