package com.cygnus.ipoten.quiz_question.repository;

import com.cygnus.ipoten.quiz_question.entity.QuizChoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface QuizChoiceRepository extends JpaRepository<QuizChoice, Long> {
    List<QuizChoice> findByQuizQuestionIdIn(List<Long> questionIds);
    List<QuizChoice> findByQuizQuestionIdInOrderByQuizQuestionIdAscIdAsc(List<Long> quizQuestionIds);

    @Query("""
        select c
        from QuizChoice c
        where c.quizQuestion.id in :questionIds
        order by c.quizQuestion.id asc, c.id asc
        """)
    List<QuizChoice> findByQuestionIds(@Param("questionIds") List<Long> questionIds);

    // 정답 보기만 가져오는 메서드
    @Query("""
    select c
    from QuizChoice c
    where c.quizQuestion.id in :questionIds
      and c.isAnswer = true
""")
    List<QuizChoice> findCorrectChoices(@Param("questionIds") List<Long> questionIds);

    @Transactional
    void deleteByQuizQuestion_Id(Long quizQuestionId);
}