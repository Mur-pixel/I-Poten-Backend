package com.cygnus.iptn.quiz_daily.controller.response_form;

import com.cygnus.iptn.quiz_question.entity.enums.QuestionType;

public record CheckDailyQuestionResponseForm (
        Long sessionId,
        Long questionId,
        QuestionType questionType,
        boolean correct,
        Long correctChoiceId,
        String answer,          // 정답 텍스트 (예: choice text, O/X, 초성 정답 단어)
        String explanation,     // 해설
        Long nextQuestionId,    // 마지막 문제면 null
        boolean lastQuestion
) {
}
