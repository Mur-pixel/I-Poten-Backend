package com.cygnus.ipoten.quiz.service.response;

import com.cygnus.ipoten.quiz.entity.enums.QuestionType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class StartUserQuizSessionResponse {
    private final Long sessionId;
    private final Long quizSetId;
    private final List<Long> questionIds;
    private final List<Item> items;

    @Getter @RequiredArgsConstructor
    public static class Item {
        private final Long questionId;
        private final QuestionType questionType;
        private final String questionText;
        private final String explanation;       // null 허용
        private final Long correctChoiceId;     // null 허용
        private final List<Option> options;
    }

    @Getter @RequiredArgsConstructor
    public static class Option {
        private final Long choiceId;
        private final String text;
    }
}
