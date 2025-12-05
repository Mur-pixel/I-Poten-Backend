package com.cygnus.ipoten.custom_term_learning.service.request;

import com.cygnus.ipoten.custom_term_learning.entity.enums.LearningStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UpdateLearningProgressRequest {
    private final Long accountId;
    private final Long termId;
    private final LearningStatus status;
}
