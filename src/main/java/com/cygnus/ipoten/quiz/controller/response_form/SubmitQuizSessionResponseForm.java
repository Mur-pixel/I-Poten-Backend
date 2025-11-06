package com.cygnus.ipoten.quiz.controller.response_form;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubmitQuizSessionResponseForm {
    private final Long sessionId;
    private final int total;
    private final int correct;
    private final Long elapsedMs;
    private final List<Item> details;

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PUBLIC)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Item {
        private final Long quizQuestionId;
        
        private final Long selectedChoiceId;        // 단일선택 시
        private final List<Long> selectedChoiceIds; // 다답형 대비

        // 정답 보기 (단일/다답 모두 대응)
        private final Long correctChoiceId;         // 정답이 1개인 경우
        private final List<Long> correctChoiceIds;  // 정답이 여러 개인 경우

        private final boolean correct;              // 이 문항에서 사용자가 맞췄는지 여부
    }
}
