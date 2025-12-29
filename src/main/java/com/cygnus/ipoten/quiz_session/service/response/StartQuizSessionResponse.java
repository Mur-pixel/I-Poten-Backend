package com.cygnus.ipoten.quiz_session.service.response;

import com.cygnus.ipoten.quiz_question.entity.enums.QuestionType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class StartQuizSessionResponse {
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
        private final String answerText;        // 초성 문제(INITIALS)의 경우 필수, 나머지는 null 허용
    }

    @Getter @RequiredArgsConstructor
    public static class Option {
        private final Long choiceId;
        private final String text;
    }

    public static StartQuizSessionResponse fromExisting(Long sessionId) {
        return new StartQuizSessionResponse(sessionId, null, List.of(), List.of());
    }

    public static StartQuizSessionResponse fromExistingWithItems(
            Long sessionId,
            Long quizSetId,
            List<Long> questionIds,
            List<Item> items
    ) {
        return new StartQuizSessionResponse(sessionId, quizSetId, questionIds, items);
    }
}
