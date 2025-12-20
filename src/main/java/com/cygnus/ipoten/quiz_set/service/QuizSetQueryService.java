package com.cygnus.ipoten.quiz_set.service;

import com.cygnus.ipoten.quiz_question.entity.QuizQuestion;
import com.cygnus.ipoten.quiz_question.entity.enums.DifficultyLevel;
import com.cygnus.ipoten.quiz_set.entity.enums.QuizSetType;
import com.cygnus.ipoten.quiz_question.service.response.ChoiceQuestionRead;
import com.cygnus.ipoten.quiz_set.service.response.ResolveQuizSetResult;

import java.util.List;
import java.util.Optional;

public interface QuizSetQueryService {
    List<ChoiceQuestionRead> findChoiceQuestionsBySetId(Long setId);
    List<Long> findQuestionIdsBySetId(Long setId);
    Optional<QuizSetType> findPartTypeBySetId(Long setId);
    List<QuizQuestion> findInitialsQuestionsBySetId(Long setId);
    ResolveQuizSetResult resolve(Long termCategoryId, QuizSetType type, DifficultyLevel level, int count);
}
