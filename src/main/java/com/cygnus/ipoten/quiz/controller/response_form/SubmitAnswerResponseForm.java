package com.cygnus.ipoten.quiz.controller.response_form;

import com.cygnus.ipoten.quiz.entity.SessionAnswer;
import com.cygnus.ipoten.quiz.service.response.SubmitAnswerResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class SubmitAnswerResponseForm {
    private final int savedCount;      // 저장된 응답 개수
    private final int correctCount;    // 저장 처리된 응답 개수

    public static SubmitAnswerResponseForm from(List<SessionAnswer> answerList) {
        List<SubmitAnswerResponse> detailList = answerList.stream()
                .map(SubmitAnswerResponse::from)
                .toList();

        int savedCount = answerList.size();
        int correctCount = (int) detailList.stream().filter(SubmitAnswerResponse::isCorrect).count();
        return new SubmitAnswerResponseForm(savedCount, correctCount);
    }
}
