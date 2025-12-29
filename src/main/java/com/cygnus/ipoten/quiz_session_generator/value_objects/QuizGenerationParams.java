package com.cygnus.ipoten.quiz_session_generator.value_objects;

import com.cygnus.ipoten.quiz_session.entity.enums.SeedMode;
import com.cygnus.ipoten.quiz_question.entity.enums.DifficultyLevel;
import com.cygnus.ipoten.quiz_question.entity.enums.QuestionType;
import com.cygnus.ipoten.term.entity.Term;

import java.util.List;

public record QuizGenerationParams(
        List<Term> terms,
        List<QuestionType> questionTypes,
        Integer count,
        Integer mcqEach,
        Integer oxEach,
        Integer initialsEach,
        SeedMode seedMode,
        Long accountId,
        Long fixedSeed,
        DifficultyLevel difficulty
) {}
