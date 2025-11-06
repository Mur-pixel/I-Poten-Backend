package com.cygnus.ipoten.quiz.repository;

import com.cygnus.ipoten.quiz.entity.QuizQuestion;
import com.cygnus.ipoten.quiz.entity.enums.QuestionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {
    @Query("""
           select q.id
           from QuizQuestion q
           where q.quizSet.id = :quizSetId
           order by coalesce(q.orderIndex, 999999), q.id
           """)
    List<Long> findIdsByQuizSetIdOrder(@Param("quizSetId") Long quizSetId);

    long countByQuizSet_Id(Long quizSetId);

    List<QuizQuestion> findByQuizSetIdAndQuestionTypeOrderByOrderIndexAscIdAsc(Long quizSetId, QuestionType questionType);
    List<QuizQuestion> findByQuizSet_IdOrderByOrderIndexAscIdAsc(Long setId);
}
