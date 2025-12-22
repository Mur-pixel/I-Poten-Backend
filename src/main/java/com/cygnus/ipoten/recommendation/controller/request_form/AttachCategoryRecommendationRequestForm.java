package com.cygnus.ipoten.recommendation.controller.request_form;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttachCategoryRecommendationRequestForm {

    @NotNull(message = "termCategoryId는 필수입니다")
    private Long termCategoryId;
}
