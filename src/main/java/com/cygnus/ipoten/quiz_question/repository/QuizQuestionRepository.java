package com.cygnus.ipoten.quiz_question.repository;

import com.cygnus.ipoten.quiz_question.entity.QuizQuestion;
import com.cygnus.ipoten.quiz_question.entity.enums.DifficultyLevel;
import com.cygnus.ipoten.quiz_question.entity.enums.QuestionType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    @Query("""
        select q
        from QuizSetQuestion link
        join link.quizQuestion q
        where link.quizSet.id = :setId
          and q.questionType in :types
        order by link.id asc
    """)
    List<QuizQuestion> findBySetIdAndQuestionTypeInOrderByOrderNoAsc(
            @Param("setId") Long setId,
            @Param("types") Collection<QuestionType> types
    );

    @Query("""
        select q
        from QuizSetQuestion link
        join link.quizQuestion q
        where link.quizSet.id = :setId
          and q.questionType = :type
        order by link.id asc
    """)
    List<QuizQuestion> findBySetIdAndQuestionTypeOrderByOrderNoAsc(
            @Param("setId") Long setId,
            @Param("type") QuestionType type
    );

    @Query("""
        select distinct q.id
        from QuizSetQuestion link
        join link.quizQuestion q
        where link.quizSet.id = :setId
          and (:dl is null or q.difficulty = :dl)
          and (:allTypes = true or q.questionType in :types)
        order by link.id asc
    """)
    List<Long> findIdsBySetFilters(
            @Param("setId") Long setId,
            @Param("dl") DifficultyLevel dl,
            @Param("allTypes") boolean allTypes,
            @Param("types") List<QuestionType> types
    );

    @Query("""
        select distinct q.id
        from QuizQuestion q
        where q.termCategory.id = :categoryId
          and (:dl is null or q.difficulty = :dl)
          and (:type is null or q.questionType = :type)
        order by q.id asc
    """)
    List<Long> findIdsByCategoryFilters(
            @Param("categoryId") Long categoryId,
            @Param("dl") DifficultyLevel dl,
            @Param("type") QuestionType type
    );

    @Query("""
        select distinct q.id
        from QuizQuestion q
        where q.termCategory.id = :categoryId
          and (:dl is null or q.difficulty = :dl)
          and (:type is null or q.questionType = :type)
          and (:hasLabels = false or exists (
                select 1
                from QuizQuestionLabel qql
                join qql.quizLabel l
                where qql.quizQuestion = q
                  and l.key in :labelKeys
          ))
        order by q.id asc
    """)
    List<Long> findIdsByCategoryFiltersAndLabels(
            @Param("categoryId") Long categoryId,
            @Param("dl") DifficultyLevel dl,
            @Param("type") QuestionType type,
            @Param("hasLabels") boolean hasLabels,
            @Param("labelKeys") List<String> labelKeys
    );

    @Query("""
        select link.quizSet.id
        from QuizSetQuestion link
        join link.quizQuestion q
        where q.termCategory.id = :categoryId
          and (:type is null or q.questionType = :type)
          and (:dl is null or q.difficulty = :dl)
        group by link.quizSet.id
        having count(distinct q.id) >= :minCount
        order by link.quizSet.id desc
    """)
    List<Long> findEligibleSetIdsByCategory(
            @Param("categoryId") Long categoryId,
            @Param("type") QuestionType type,
            @Param("dl") DifficultyLevel dl,
            @Param("minCount") long minCount,
            Pageable pageable
    );

    @Query("""
        select link.quizSet.id
        from QuizSetQuestion link
        join link.quizQuestion q
        where q.termCategory.id = :categoryId
          and (:type is null or q.questionType = :type)
          and (:dl is null or q.difficulty = :dl)
          and (:hasLabels = false or exists (
                select 1
                from QuizQuestionLabel qql
                join qql.quizLabel l
                where qql.quizQuestion = q
                  and l.key in :labelKeys
          ))
        group by link.quizSet.id
        having count(distinct q.id) >= :minCount
        order by link.quizSet.id desc
    """)
    List<Long> findEligibleSetIdsByCategoryAndLabels(
            @Param("categoryId") Long categoryId,
            @Param("type") QuestionType type,
            @Param("dl") DifficultyLevel dl,
            @Param("hasLabels") boolean hasLabels,
            @Param("labelKeys") List<String> labelKeys,
            @Param("minCount") long minCount,
            Pageable pageable
    );

    @Query("""
        select case when count(link.id) > 0 then true else false end
        from QuizSetQuestion link
        join link.quizQuestion q
        where link.quizSet.id = :quizSetId
          and q.questionType = :questionType
          and q.questionText = :questionText
          and q.term.id = :termId
    """)
    boolean existsBySetAndQuestionTypeAndQuestionTextAndTermId(
            @Param("quizSetId") Long quizSetId,
            @Param("questionType") QuestionType questionType,
            @Param("questionText") String questionText,
            @Param("termId") Long termId
    );

    boolean existsByQuestionTypeAndQuestionTextAndTerm_Id(
            QuestionType questionType,
            String questionText,
            Long termId
    );

    @Query("""
        select q.questionType, count(q)
        from QuizQuestion q
        where q.id in :ids
        group by q.questionType
    """)
    List<Object[]> countTypesByIds(@Param("ids") List<Long> ids);
}
