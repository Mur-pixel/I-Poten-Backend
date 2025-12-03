package com.cygnus.ipoten.quiz.repository;

import com.cygnus.ipoten.quiz.entity.QuizSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuizSetRepository extends JpaRepository<QuizSet, Long> {

    Optional<QuizSet> findFirstByTitle(String title);

    @Query("""
           select q.id
           from QuizQuestion q
           where q.quizSet.id = :setId
           order by q.id asc
           """)
    List<Long> findQuestionIdsBySetId(@Param("setId") Long setId);
}
