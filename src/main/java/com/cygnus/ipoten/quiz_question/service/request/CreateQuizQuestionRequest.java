package com.cygnus.ipoten.quiz_question.service.request;

import com.cygnus.ipoten.quiz_question.entity.enums.DifficultyLevel;
import com.cygnus.ipoten.quiz_set.entity.QuizSet;
import com.cygnus.ipoten.quiz_question.entity.enums.QuestionType;
import com.cygnus.ipoten.quiz_question.entity.QuizQuestion;
import com.cygnus.ipoten.term_category.entity.TermCategory;
import com.cygnus.ipoten.term.entity.Term;
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
        QuizQuestion q = new QuizQuestion(term, termCategory, questionType, DifficultyLevel.MEDIUM, questionText, (QuizSet) null);
        return q;
    }
}
