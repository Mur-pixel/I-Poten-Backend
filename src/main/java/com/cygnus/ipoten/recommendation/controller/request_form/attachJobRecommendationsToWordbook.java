package com.cygnus.ipoten.recommendation.controller.request_form;

import com.cygnus.ipoten.recommendation.entity.enums.JobKey;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class attachJobRecommendationsToWordbook {

    @NotNull(message = "jobKey는 필수입니다.")
    private JobKey jobKey;
}
