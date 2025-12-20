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

    List<QuizQuestion> findByQuizSet_IdAndQuestionTypeInOrderByIdAsc(Long setId, Collection<QuestionType> types);
    List<QuizQuestion> findByQuizSet_IdAndQuestionTypeOrderByIdAsc(Long setId, QuestionType type);

    /* =========================
     *  SET: no-tags / with-tags
     * ========================= */

    @Query("""
        select distinct q.id
        from QuizQuestion q
        where q.quizSet.id = :setId
          and (:dl is null or q.difficulty = :dl)
          and (:allTypes = true or q.questionType in :types)
        order by q.id asc
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
        left join q.term t
        left join t.topicTags tag
        where q.quizSet.id = :setId
          and (:dl is null or q.difficulty = :dl)
          and (:allTypes = true or q.questionType in :types)
          and (:hasTags = false or lower(tag.key) in :tagKeys)
        order by q.id asc
    """)
    List<Long> findIdsBySetFiltersAndTopicTags(
            @Param("setId") Long setId,
            @Param("dl") DifficultyLevel dl,
            @Param("allTypes") boolean allTypes,
            @Param("types") List<QuestionType> types,
            @Param("hasTags") boolean hasTags,
            @Param("tagKeys") List<String> tagKeys
    );

    /* =========================
     *  CATEGORY: no-tags / with-tags
     * ========================= */

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
        left join q.term t
        left join t.topicTags tag
        where q.termCategory.id = :categoryId
          and (:dl is null or q.difficulty = :dl)
          and (:type is null or q.questionType = :type)
          and (:hasTags = false or lower(tag.key) in :tagKeys)
        order by q.id asc
    """)
    List<Long> findIdsByCategoryFiltersAndTopicTags(
            @Param("categoryId") Long categoryId,
            @Param("dl") DifficultyLevel dl,
            @Param("type") QuestionType type,
            @Param("hasTags") boolean hasTags,
            @Param("tagKeys") List<String> tagKeys
    );

    /* =========================
     *  CATEGORY: eligible set pick (no-tags / with-tags)
     * ========================= */

    @Query("""
        select q.quizSet.id
        from QuizQuestion q
        where q.termCategory.id = :categoryId
          and (:type is null or q.questionType = :type)
          and (:dl is null or q.difficulty = :dl)
        group by q.quizSet.id
        having count(distinct q.id) >= :minCount
        order by q.quizSet.id desc
    """)
    List<Long> findEligibleSetIdsByCategory(
            @Param("categoryId") Long categoryId,
            @Param("type") QuestionType type,
            @Param("dl") DifficultyLevel dl,
            @Param("minCount") long minCount,
            Pageable pageable
    );

    @Query("""
        select q.quizSet.id
        from QuizQuestion q
        left join q.term t
        left join t.topicTags tag
        where q.termCategory.id = :categoryId
          and (:type is null or q.questionType = :type)
          and (:dl is null or q.difficulty = :dl)
          and (:hasTags = false or lower(tag.key) in :tagKeys)
        group by q.quizSet.id
        having count(distinct q.id) >= :minCount
        order by q.quizSet.id desc
    """)
    List<Long> findEligibleSetIdsByCategoryAndTopicTags(
            @Param("categoryId") Long categoryId,
            @Param("type") QuestionType type,
            @Param("dl") DifficultyLevel dl,
            @Param("hasTags") boolean hasTags,
            @Param("tagKeys") List<String> tagKeys,
            @Param("minCount") long minCount,
            Pageable pageable
    );

    /* =========================
     *  MISC
     * ========================= */

    boolean existsByQuizSet_IdAndQuestionTypeAndQuestionTextAndTerm_Id(
            Long quizSetId,
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
