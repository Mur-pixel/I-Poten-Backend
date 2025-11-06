package com.cygnus.ipoten.quiz.service.response;

import com.cygnus.ipoten.quiz.entity.QuizChoice;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class CreateQuizChoiceListResponse {
    private final String message;
    private final List<CreateQuizChoiceResponse> choices;

    public static CreateQuizChoiceListResponse from(List<QuizChoice> quizChoices) {

        List<CreateQuizChoiceResponse> responses = quizChoices.stream()
                .map(CreateQuizChoiceResponse::from)
                .toList();

        return new CreateQuizChoiceListResponse(
                "문제의 보기와 답이 정상적으로 등록되었습니다.",
                responses
        );
    }
}
