package com.cygnus.ipoten.recommendation.service;

import com.cygnus.ipoten.recommendation.entity.enums.JobKey;
import com.cygnus.ipoten.wordbook.service.response.AttachTermsBulkResponse;

public interface JobRecommendedTermService {
    AttachTermsBulkResponse attachJobRecommendationsToWordbook(Long accountId, Long wordbookId, JobKey jobKey);
}
