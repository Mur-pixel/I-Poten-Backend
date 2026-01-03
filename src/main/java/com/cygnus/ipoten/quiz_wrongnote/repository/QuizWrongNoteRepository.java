package com.cygnus.ipoten.quiz_wrongnote.repository;

import com.cygnus.ipoten.quiz_question.entity.enums.QuestionType;
import com.cygnus.ipoten.quiz_wrongnote.entity.QuizWrongNote;
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
        where wn.account.id = :accountId
          and (:type is null or q.questionType = :type)
          and (:sessionId is null or wn.quizSessionId = :sessionId)
          and (:fromAt is null or wn.submittedAt >= :fromAt)
          and (:toAt is null or wn.submittedAt < :toAt)
        order by wn.submittedAt desc, wn.id desc
        """,
            countQuery = """
        select count(wn)
        from QuizWrongNote wn
        join wn.quizQuestion q
        where wn.account.id = :accountId
          and (:type is null or q.questionType = :type)
          and (:sessionId is null or wn.quizSessionId = :sessionId)
          and (:fromAt is null or wn.submittedAt >= :fromAt)
          and (:toAt is null or wn.submittedAt < :toAt)
        """
    )
    Page<QuizWrongNote> findWrongNotes(
            @Param("accountId") Long accountId,
            @Param("type") QuestionType type,
            @Param("sessionId") Long sessionId,
            @Param("fromAt") Instant fromAt,
            @Param("toAt") Instant toAt,
            Pageable pageable
    );
}
