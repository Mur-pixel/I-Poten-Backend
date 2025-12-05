package com.cygnus.ipoten.quiz_session.controller.response_form;

import com.cygnus.ipoten.quiz.entity.enums.QuestionType;
import com.cygnus.ipoten.quiz_session.service.response.StartUserQuizSessionResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class CreateQuizSessionResponseForm {
    private final Long sessionId;
    private final Long quizSetId;
    private final List<Long> questionIds;
    private final List<Item> items;

    @Getter
    @RequiredArgsConstructor
    public static class Item {
        private final Long questionId;
        private final QuestionType questionType;
        private final String questionText;
        private final String explanation;
        private final Long correctChoiceId;
        private final List<Option> options;
    }

    @Getter
    @RequiredArgsConstructor
    public static class Option {
        private final Long choiceId;
        private final String text;
    }

    public static CreateQuizSessionResponseForm from(StartUserQuizSessionResponse s) {
        List<StartUserQuizSessionResponse.Item> safeItems =
                s.getItems() == null ? List.of() : s.getItems();

        List<Item> mapped = safeItems.stream()
                .map(it -> {
                    List<StartUserQuizSessionResponse.Option> safeOps =
                            it.getOptions() == null ? List.of() : it.getOptions();

                    List<Option> mappedOps = safeOps.stream()
                            .map(op -> new Option(op.getChoiceId(), op.getText()))
                            .toList();

                    return new Item(
                            it.getQuestionId(),
                            it.getQuestionType(),
                            it.getQuestionText(),
                            it.getExplanation(),
                            it.getCorrectChoiceId(),
                            mappedOps
                    );
                })
                .toList();

        return new CreateQuizSessionResponseForm(
                s.getSessionId(),
                s.getQuizSetId(),
                s.getQuestionIds(),
                mapped
        );
    }

}
