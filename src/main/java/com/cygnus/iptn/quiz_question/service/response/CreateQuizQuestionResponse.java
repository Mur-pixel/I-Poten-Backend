package com.cygnus.iptn.quiz_question.service.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.cygnus.iptn.quiz_question.entity.enums.QuestionType;
import com.cygnus.iptn.quiz_question.entity.QuizQuestion;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateQuizQuestionResponse {

    private final String message;
    private final Long questionId;
    private final QuestionType questionType;
    private final String questionText;
    private final String answerText;

    public static CreateQuizQuestionResponse from(QuizQuestion q) {
        String msg = "문제가 성공적으로 등록되었습니다.";
        String answer = null;
        if (q.getQuestionType() == QuestionType.INITIALS && q.getQuestionText() != null) {
            answer = q.getQuizTextAnswer().getAnswerText();
        }
        return new CreateQuizQuestionResponse(
                msg,
                q.getId(),
                q.getQuestionType(),
                q.getQuestionText(),
                answer
        );
    }
}
