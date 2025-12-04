package com.cygnus.ipoten.custom_term_learning.controller.response_form;

import com.cygnus.ipoten.custom_term_learning.entity.enums.LearningStatus;
import com.cygnus.ipoten.custom_term_learning.service.response.UpdateLearningProgressResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class UpdateLearningProgressResponseForm {
    private final Long termId;
    private final LearningStatus status;
    private final Instant completedAt;
    private final Instant lastStudiedAt;
    private final boolean changed;

    public static UpdateLearningProgressResponseForm from(UpdateLearningProgressResponse response) {
        return UpdateLearningProgressResponseForm.builder()
                .termId(response.getTermId())
                .status(response.getStatus())
                .completedAt(response.getCompletedAt())
                .lastStudiedAt(response.getLastStudiedAt())
                .changed(response.isChanged())
                .build();
    }
}