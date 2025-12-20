package com.cygnus.ipoten.job_recommendation.service;

import com.cygnus.ipoten.job.enums.JobRole;
import com.cygnus.ipoten.wordbook.service.response.AttachTermsBulkResponse;

public interface JobRoleRecommendationService {
    AttachTermsBulkResponse attachJobRoleRecommendationsToWordbook(Long accountId, Long wordbookId, JobRole jobRole);
}
