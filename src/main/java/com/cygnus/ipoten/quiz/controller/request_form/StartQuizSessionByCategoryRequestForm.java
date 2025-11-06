package com.cygnus.ipoten.quiz.controller.request_form;

import com.cygnus.ipoten.quiz.service.request.CreateQuizSetByCategoryRequest;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 카테고리 기반으로 바로 '세션'을 만들기 위한 폼 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StartQuizSessionByCategoryRequestForm {

    @NotNull
    private Long categoryId;

    // 프런트에서 고르는 값
    @Min(1) @Max(100)
    private int count;

    @NotBlank
    @Pattern(regexp = "(?i)mix|choice|ox|initials",
            message = "questionType은 MIX/CHOICE/OX/INITIALS 중 하나여야 합니다.")
    private String questionType;

    @NotBlank
    @Pattern(regexp = "(?i)mix|easy|medium|hard",
            message = "difficulty는 MIX/EASY/MEDIUM/HARD 중 하나여야 합니다.")
    private String difficulty;

    // 시드 전략
    @NotBlank
    @Pattern(regexp = "(?i)auto|daily|fixed",
            message = "seedMode는 AUTO/DAILY/FIXED 중 하나여야 합니다.")
    private String seedMode;

    private Long fixedSeed; // seedMode=FIXED 일 때만 사용

    public CreateQuizSetByCategoryRequest toCategoryBasedRequest() {
        final String qt = (questionType == null ? "MIX" : questionType.trim().toUpperCase());
        final String dl = (difficulty   == null ? "MIX" : difficulty.trim().toUpperCase());
        final boolean isRandom = !"FIXED".equalsIgnoreCase(seedMode);

        return new CreateQuizSetByCategoryRequest(
                null,
                categoryId,
                isRandom,
                count,
                CreateQuizSetByCategoryRequest.QuestionType.valueOf(qt),
                CreateQuizSetByCategoryRequest.DifficultyLevel.valueOf(dl)
        );
    }
}
