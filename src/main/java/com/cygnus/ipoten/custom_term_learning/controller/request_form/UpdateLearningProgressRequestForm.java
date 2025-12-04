package com.cygnus.ipoten.custom_term_learning.controller.request_form;

import com.cygnus.ipoten.custom_term_learning.entity.enums.LearningStatus;
import com.cygnus.ipoten.custom_term_learning.service.request.UpdateLearningProgressRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateLearningProgressRequestForm {

    @NotNull(message = "status는 반드시 필요합니다.")
    private LearningStatus status;

    public UpdateLearningProgressRequest toUpdateMemorizationRequest(Long accountId, Long termId) {
        return new UpdateLearningProgressRequest(accountId, termId, status);
    }

}
