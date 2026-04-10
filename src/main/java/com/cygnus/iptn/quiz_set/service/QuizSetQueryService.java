package com.cygnus.iptn.quiz_set.service;

import com.cygnus.iptn.quiz_question.entity.QuizQuestion;
import com.cygnus.iptn.quiz_question.entity.enums.DifficultyLevel;
import com.cygnus.iptn.quiz_set.entity.enums.QuizSetType;
import com.cygnus.iptn.quiz_question.service.response.ChoiceQuestionRead;
import com.cygnus.iptn.quiz_set.service.response.ResolveQuizSetResult;

import java.util.List;
import java.util.Optional;

public interface QuizSetQueryService {
    List<ChoiceQuestionRead> findChoiceQuestionsBySetId(Long setId);
    Optional<QuizSetType> findPartTypeBySetId(Long setId);
    List<QuizQuestion> findInitialsQuestionsBySetId(Long setId);
    ResolveQuizSetResult resolve(Long termCategoryId, QuizSetType type, DifficultyLevel level, int count);
}
