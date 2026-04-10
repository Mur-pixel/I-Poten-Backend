package com.cygnus.iptn.quiz_daily.controller.request_form;

import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CheckDailyQuestionRequestForm {

    // CHOICE / OX 용
    private Long choiceId;

    // INITIALS 용
    private String answerText;

    /** 둘 다 비어있거나 둘 다 채워진 경우 방지 */
    @AssertTrue(message = "choiceId 또는 answerText 중 하나만 전달해야 합니다.")
    public boolean isValidEitherOne() {
        boolean hasChoice = (choiceId != null);
        boolean hasAnswerText = (answerText != null && !answerText.isBlank());
        return hasChoice ^ hasAnswerText;
    }
}
