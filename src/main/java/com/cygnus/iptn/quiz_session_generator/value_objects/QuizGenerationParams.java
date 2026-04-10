package com.cygnus.iptn.quiz_session_generator.value_objects;

import com.cygnus.iptn.quiz_session.entity.enums.SeedMode;
import com.cygnus.iptn.quiz_question.entity.enums.DifficultyLevel;
import com.cygnus.iptn.quiz_question.entity.enums.QuestionType;
import com.cygnus.iptn.term.entity.Term;

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
