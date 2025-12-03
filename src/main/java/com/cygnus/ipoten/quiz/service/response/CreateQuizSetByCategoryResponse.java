package com.cygnus.ipoten.quiz.service.response;

import com.cygnus.ipoten.quiz.entity.QuizSet;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class CreateQuizSetByCategoryResponse {
    private final String message;
    private final Long quizSetId;
    private final String title;

    private final List<Long> questionIds;
    private final int totalQuestions;

    public static CreateQuizSetByCategoryResponse from(QuizSet quizSet, List<Long> questionIds) {
        String message = "퀴즈 세트가 성공적으로 등록되었습니다.";

        List<Long> ids = (questionIds == null) ? List.of() : questionIds;
        return new CreateQuizSetByCategoryResponse(
                message,
                quizSet.getId(),
                quizSet.getTitle(),
                ids,
                ids.size()
        );
    }

    @Deprecated
    public static CreateQuizSetByCategoryResponse from(QuizSet quizSet) {
        return from(quizSet, List.of());
    }
}
