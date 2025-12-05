package com.cygnus.ipoten.quiz.service;

import com.cygnus.ipoten.quiz_question.entity.QuizQuestion;
import com.cygnus.ipoten.quiz.entity.enums.QuizSetType;
import com.cygnus.ipoten.quiz_question.service.response.ChoiceQuestionRead;

import java.util.List;
import java.util.Optional;

public interface QuizSetQueryService {
    List<ChoiceQuestionRead> findChoiceQuestionsBySetId(Long setId);
    List<Long> findQuestionIdsBySetId(Long setId);
    Optional<QuizSetType> findPartTypeBySetId(Long setId);
    List<QuizQuestion> findInitialsQuestionsBySetId(Long setId);
}
