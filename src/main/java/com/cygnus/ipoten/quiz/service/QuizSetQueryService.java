package com.cygnus.ipoten.quiz.service;

import com.cygnus.ipoten.quiz.entity.QuizQuestion;
import com.cygnus.ipoten.quiz.entity.enums.QuizPartType;
import com.cygnus.ipoten.quiz.service.response.ChoiceQuestionRead;

import java.util.List;
import java.util.Optional;

public interface QuizSetQueryService {
    List<ChoiceQuestionRead> findChoiceQuestionsBySetId(Long setId);
    List<Long> findQuestionIdsBySetId(Long setId);
    Optional<QuizPartType> findPartTypeBySetId(Long setId);
    List<QuizQuestion> findInitialsQuestionsBySetId(Long setId);
}
