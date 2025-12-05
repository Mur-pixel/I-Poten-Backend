package com.cygnus.ipoten.custom_term_learning.service.response;

import com.cygnus.ipoten.custom_term_learning.entity.enums.LearningStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class UpdateLearningProgressResponse {
    private final Long termId;
    private final LearningStatus status;
    private final Instant completedAt;
    private final Instant lastStudiedAt;
    private final boolean changed;
}
