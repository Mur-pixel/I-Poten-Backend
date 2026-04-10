package com.cygnus.iptn.wordbook_learning.service.request;

import com.cygnus.iptn.wordbook_learning.entity.enums.LearningStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UpdateLearningProgressRequest {
    private final Long accountId;
    private final Long termId;
    private final LearningStatus status;
}
