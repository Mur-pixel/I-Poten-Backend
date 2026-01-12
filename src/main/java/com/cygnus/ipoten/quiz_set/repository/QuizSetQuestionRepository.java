package com.cygnus.ipoten.quiz_set.repository;

import com.cygnus.ipoten.quiz_set.entity.QuizSetQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuizSetQuestionRepository extends JpaRepository<QuizSetQuestion, Long> {
    @Query("""
        select sq.quizQuestion.id
        from QuizSetQuestion sq
        where sq.quizSet.id = :setId
        order by sq.id asc
    """)
    List<Long> findQuestionIdsBySetId(@Param("setId") Long setId);
}
