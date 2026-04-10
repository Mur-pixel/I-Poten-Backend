package com.cygnus.iptn.quiz_question.service.request;

import com.cygnus.iptn.quiz_question.entity.enums.DifficultyLevel;
import com.cygnus.iptn.quiz_set.entity.QuizSet;
import com.cygnus.iptn.quiz_question.entity.enums.QuestionType;
import com.cygnus.iptn.quiz_question.entity.QuizQuestion;
import com.cygnus.iptn.term_category.entity.TermCategory;
import com.cygnus.iptn.term.entity.Term;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreateQuizQuestionRequest {

    private final Long termId;
    private final Long categoryId;
    private final QuestionType questionType;
    private final String questionText;
    private final Integer questionAnswer;
    private final String answerText;

    public QuizQuestion toQuizQuestion(Term term, TermCategory termCategory) {
        return new QuizQuestion(
                term,
                termCategory,
                questionType,
                DifficultyLevel.MEDIUM,
                questionText,
                null // explanation
        );
    }
}
