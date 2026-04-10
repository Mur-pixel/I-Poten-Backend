package com.cygnus.iptn.wordbook_learning.service.response;

import com.cygnus.iptn.wordbook_learning.entity.enums.LearningStatus;
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
