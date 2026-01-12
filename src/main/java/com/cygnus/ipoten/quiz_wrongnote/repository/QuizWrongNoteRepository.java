package com.cygnus.ipoten.quiz_wrongnote.repository;

import com.cygnus.ipoten.quiz_question.entity.enums.DifficultyLevel;
import com.cygnus.ipoten.quiz_question.entity.enums.QuestionType;
import com.cygnus.ipoten.quiz_wrongnote.entity.QuizWrongNote;
import com.cygnus.ipoten.quiz_wrongnote.entity.enums.WrongNoteStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface QuizWrongNoteRepository extends JpaRepository<QuizWrongNote, Long> {

    boolean existsByAccount_IdAndQuizQuestion_Id(Long accountId, Long quizQuestionId);
    Optional<QuizWrongNote> findByAccount_IdAndQuizQuestion_Id(Long accountId, Long quizQuestionId);

    @Query(
            value = """
            select wn
            from QuizWrongNote wn
            join fetch wn.quizQuestion q
            left join fetch q.quizTextAnswer ta
            left join fetch q.term t
            left join fetch t.termCategory c
            left join fetch wn.quizSession s
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
            """,
            countQuery = """
            select count(wn)
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
    Page<QuizWrongNote> searchWrongNotes(
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

    Optional<QuizWrongNote> findByIdAndAccount_Id(Long id, Long accountId);
}
