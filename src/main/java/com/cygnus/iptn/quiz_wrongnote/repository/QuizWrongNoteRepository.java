package com.cygnus.iptn.quiz_wrongnote.repository;

import com.cygnus.iptn.account.entity.Account;
import com.cygnus.iptn.quiz_question.entity.enums.DifficultyLevel;
import com.cygnus.iptn.quiz_question.entity.enums.QuestionType;
import com.cygnus.iptn.quiz_wrongnote.entity.QuizWrongNote;
import com.cygnus.iptn.quiz_wrongnote.entity.enums.WrongNoteStatus;
import com.cygnus.iptn.quiz_wrongnote.repository.projection.WrongNoteQuestionSummaryView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface QuizWrongNoteRepository extends JpaRepository<QuizWrongNote, Long> {

    @Query("""
        select max(wn.updatedAt)
        from QuizWrongNote wn
        where wn.account.id = :accountId
    """)
    Optional<Instant> findLatestUpdatedAtByAccountId(@Param("accountId") Long accountId);

    boolean existsByAccount_IdAndQuizQuestion_Id(Long accountId, Long quizQuestionId);
    Optional<QuizWrongNote> findByAccount_IdAndQuizQuestion_Id(Long accountId, Long quizQuestionId);

    long deleteByIdAndAccount_Id(Long id, Long accountId);
    long deleteByAccount_IdAndIdIn(Long accountId, List<Long> ids);
    long deleteByAccount_IdAndQuizQuestion_Id(Long accountId, Long quizQuestionId);
    long deleteByAccount_IdAndQuizQuestion_IdIn(Long accountId, List<Long> quizQuestionIds);

    @Query(
            value = """
            select
                max(wn.id) as representativeWrongNoteId,
                q.id as questionId,
                count(wn.id) as wrongCount,
                sum(case when wn.status = :unresolvedStatus then 1 else 0 end) as unresolvedCount,
                max(wn.submittedAt) as latestWrongAt
            from QuizWrongNote wn
            join wn.quizQuestion q
            left join q.term t
            left join t.termCategory c
            left join wn.quizSession s
            where wn.account.id = :accountId
              and (:questionType is null or q.questionType = :questionType)
              and (:difficulty is null or q.difficulty = :difficulty)
              and (:sessionId is null or wn.quizSessionId = :sessionId)
              and (:fromAt is null or wn.submittedAt >= :fromAt)
              and (:toAt is null or wn.submittedAt < :toAt)
              and (:unresolvedOnly = false or wn.status = :unresolvedStatus)
              and (
                    :q is null
                    or lower(q.questionText) like concat('%', lower(:q), '%')
                    or lower(coalesce(q.explanation, '')) like concat('%', lower(:q), '%')
                    or lower(coalesce(t.title, '')) like concat('%', lower(:q), '%')
                    or lower(coalesce(t.description, '')) like concat('%', lower(:q), '%')
                    or lower(coalesce(c.name, '')) like concat('%', lower(:q), '%')
                    or lower(coalesce(c.groupName, '')) like concat('%', lower(:q), '%')
                    or lower(coalesce(s.title, '')) like concat('%', lower(:q), '%')
              )
            group by q.id
            order by max(wn.submittedAt) desc, count(wn.id) desc, max(wn.id) desc
            """,
            countQuery = """
            select count(distinct q.id)
            from QuizWrongNote wn
            join wn.quizQuestion q
            left join q.term t
            left join t.termCategory c
            left join wn.quizSession s
            where wn.account.id = :accountId
              and (:questionType is null or q.questionType = :questionType)
              and (:difficulty is null or q.difficulty = :difficulty)
              and (:sessionId is null or wn.quizSessionId = :sessionId)
              and (:fromAt is null or wn.submittedAt >= :fromAt)
              and (:toAt is null or wn.submittedAt < :toAt)
              and (:unresolvedOnly = false or wn.status = :unresolvedStatus)
              and (
                    :q is null
                    or lower(q.questionText) like concat('%', lower(:q), '%')
                    or lower(coalesce(q.explanation, '')) like concat('%', lower(:q), '%')
                    or lower(coalesce(t.title, '')) like concat('%', lower(:q), '%')
                    or lower(coalesce(t.description, '')) like concat('%', lower(:q), '%')
                    or lower(coalesce(c.name, '')) like concat('%', lower(:q), '%')
                    or lower(coalesce(c.groupName, '')) like concat('%', lower(:q), '%')
                    or lower(coalesce(s.title, '')) like concat('%', lower(:q), '%')
              )
            """
    )
    Page<WrongNoteQuestionSummaryView> searchWrongNoteQuestionSummariesRecent(
            @Param("accountId") Long accountId,
            @Param("questionType") QuestionType questionType,
            @Param("difficulty") DifficultyLevel difficultyLevel,
            @Param("unresolvedOnly") boolean unresolvedOnly,
            @Param("unresolvedStatus") WrongNoteStatus unresolvedStatus,
            @Param("q") String q,
            @Param("sessionId") Long sessionId,
            @Param("fromAt") Instant from,
            @Param("toAt") Instant toExclusive,
            Pageable pageable
    );

    @Query(
            value = """
            select
                max(wn.id) as representativeWrongNoteId,
                q.id as questionId,
                count(wn.id) as wrongCount,
                sum(case when wn.status = :unresolvedStatus then 1 else 0 end) as unresolvedCount,
                max(wn.submittedAt) as latestWrongAt
            from QuizWrongNote wn
            join wn.quizQuestion q
            left join q.term t
            left join t.termCategory c
            left join wn.quizSession s
            where wn.account.id = :accountId
              and (:questionType is null or q.questionType = :questionType)
              and (:difficulty is null or q.difficulty = :difficulty)
              and (:sessionId is null or wn.quizSessionId = :sessionId)
              and (:fromAt is null or wn.submittedAt >= :fromAt)
              and (:toAt is null or wn.submittedAt < :toAt)
              and (:unresolvedOnly = false or wn.status = :unresolvedStatus)
              and (
                    :q is null
                    or lower(q.questionText) like concat('%', lower(:q), '%')
                    or lower(coalesce(q.explanation, '')) like concat('%', lower(:q), '%')
                    or lower(coalesce(t.title, '')) like concat('%', lower(:q), '%')
                    or lower(coalesce(t.description, '')) like concat('%', lower(:q), '%')
                    or lower(coalesce(c.name, '')) like concat('%', lower(:q), '%')
                    or lower(coalesce(c.groupName, '')) like concat('%', lower(:q), '%')
                    or lower(coalesce(s.title, '')) like concat('%', lower(:q), '%')
              )
            group by q.id
            order by max(wn.submittedAt) asc, count(wn.id) desc, max(wn.id) asc
            """,
            countQuery = """
            select count(distinct q.id)
            from QuizWrongNote wn
            join wn.quizQuestion q
            left join q.term t
            left join t.termCategory c
            left join wn.quizSession s
            where wn.account.id = :accountId
              and (:questionType is null or q.questionType = :questionType)
              and (:difficulty is null or q.difficulty = :difficulty)
              and (:sessionId is null or wn.quizSessionId = :sessionId)
              and (:fromAt is null or wn.submittedAt >= :fromAt)
              and (:toAt is null or wn.submittedAt < :toAt)
              and (:unresolvedOnly = false or wn.status = :unresolvedStatus)
              and (
                    :q is null
                    or lower(q.questionText) like concat('%', lower(:q), '%')
                    or lower(coalesce(q.explanation, '')) like concat('%', lower(:q), '%')
                    or lower(coalesce(t.title, '')) like concat('%', lower(:q), '%')
                    or lower(coalesce(t.description, '')) like concat('%', lower(:q), '%')
                    or lower(coalesce(c.name, '')) like concat('%', lower(:q), '%')
                    or lower(coalesce(c.groupName, '')) like concat('%', lower(:q), '%')
                    or lower(coalesce(s.title, '')) like concat('%', lower(:q), '%')
              )
            """
    )
    Page<WrongNoteQuestionSummaryView> searchWrongNoteQuestionSummariesOldest(
            @Param("accountId") Long accountId,
            @Param("questionType") QuestionType questionType,
            @Param("difficulty") DifficultyLevel difficultyLevel,
            @Param("unresolvedOnly") boolean unresolvedOnly,
            @Param("unresolvedStatus") WrongNoteStatus unresolvedStatus,
            @Param("q") String q,
            @Param("sessionId") Long sessionId,
            @Param("fromAt") Instant from,
            @Param("toAt") Instant toExclusive,
            Pageable pageable
    );

    @Query(
            value = """
            select
                max(wn.id) as representativeWrongNoteId,
                q.id as questionId,
                count(wn.id) as wrongCount,
                sum(case when wn.status = :unresolvedStatus then 1 else 0 end) as unresolvedCount,
                max(wn.submittedAt) as latestWrongAt
            from QuizWrongNote wn
            join wn.quizQuestion q
            left join q.term t
            left join t.termCategory c
            left join wn.quizSession s
            where wn.account.id = :accountId
              and (:questionType is null or q.questionType = :questionType)
              and (:difficulty is null or q.difficulty = :difficulty)
              and (:sessionId is null or wn.quizSessionId = :sessionId)
              and (:fromAt is null or wn.submittedAt >= :fromAt)
              and (:toAt is null or wn.submittedAt < :toAt)
              and (:unresolvedOnly = false or wn.status = :unresolvedStatus)
              and (
                    :q is null
                    or lower(q.questionText) like concat('%', lower(:q), '%')
                    or lower(coalesce(q.explanation, '')) like concat('%', lower(:q), '%')
                    or lower(coalesce(t.title, '')) like concat('%', lower(:q), '%')
                    or lower(coalesce(t.description, '')) like concat('%', lower(:q), '%')
                    or lower(coalesce(c.name, '')) like concat('%', lower(:q), '%')
                    or lower(coalesce(c.groupName, '')) like concat('%', lower(:q), '%')
                    or lower(coalesce(s.title, '')) like concat('%', lower(:q), '%')
              )
            group by q.id
            order by count(wn.id) desc, max(wn.submittedAt) desc, max(wn.id) desc
            """,
            countQuery = """
            select count(distinct q.id)
            from QuizWrongNote wn
            join wn.quizQuestion q
            left join q.term t
            left join t.termCategory c
            left join wn.quizSession s
            where wn.account.id = :accountId
              and (:questionType is null or q.questionType = :questionType)
              and (:difficulty is null or q.difficulty = :difficulty)
              and (:sessionId is null or wn.quizSessionId = :sessionId)
              and (:fromAt is null or wn.submittedAt >= :fromAt)
              and (:toAt is null or wn.submittedAt < :toAt)
              and (:unresolvedOnly = false or wn.status = :unresolvedStatus)
              and (
                    :q is null
                    or lower(q.questionText) like concat('%', lower(:q), '%')
                    or lower(coalesce(q.explanation, '')) like concat('%', lower(:q), '%')
                    or lower(coalesce(t.title, '')) like concat('%', lower(:q), '%')
                    or lower(coalesce(t.description, '')) like concat('%', lower(:q), '%')
                    or lower(coalesce(c.name, '')) like concat('%', lower(:q), '%')
                    or lower(coalesce(c.groupName, '')) like concat('%', lower(:q), '%')
                    or lower(coalesce(s.title, '')) like concat('%', lower(:q), '%')
              )
            """
    )
    Page<WrongNoteQuestionSummaryView> searchWrongNoteQuestionSummariesMostWrong(
            @Param("accountId") Long accountId,
            @Param("questionType") QuestionType questionType,
            @Param("difficulty") DifficultyLevel difficultyLevel,
            @Param("unresolvedOnly") boolean unresolvedOnly,
            @Param("unresolvedStatus") WrongNoteStatus unresolvedStatus,
            @Param("q") String q,
            @Param("sessionId") Long sessionId,
            @Param("fromAt") Instant from,
            @Param("toAt") Instant toExclusive,
            Pageable pageable
    );

    @Query("""
        select wn
        from QuizWrongNote wn
        join fetch wn.quizQuestion q
        left join fetch q.quizTextAnswer ta
        left join fetch q.term t
        left join fetch t.termCategory c
        left join fetch wn.quizSession s
        where wn.id in :ids
    """)
    List<QuizWrongNote> findAllWithDetailsByIdIn(@Param("ids") List<Long> ids);

    List<QuizWrongNote> findByIdInAndAccount_Id(List<Long> ids, Long accountId);
    Optional<QuizWrongNote> findByIdAndAccount_Id(Long id, Long accountId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update QuizWrongNote wn
        set wn.status = :status
        where wn.account.id = :accountId
          and wn.quizQuestion.id = :questionId
    """)
    int updateStatusByAccountIdAndQuestionId(
            @Param("accountId") Long accountId,
            @Param("questionId") Long questionId,
            @Param("status") WrongNoteStatus status
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
    delete from QuizWrongNote wn
     where wn.account.id = :accountId
       and wn.quizSession.id = :sessionId
    """)
    int deleteByAccountIdAndSessionId(@Param("accountId") Long accountId, @Param("sessionId") Long sessionId);

    List<QuizWrongNote> account(Account account);
}
