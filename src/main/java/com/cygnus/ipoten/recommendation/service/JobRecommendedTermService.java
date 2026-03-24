package com.cygnus.ipoten.recommendation.service;

import com.cygnus.ipoten.recommendation.entity.JobRecommendedTerm;
import com.cygnus.ipoten.recommendation.entity.enums.JobKey;
import com.cygnus.ipoten.wordbook.service.response.AttachTermsBulkResponse;

import java.util.List;

public interface JobRecommendedTermService {
    AttachTermsBulkResponse attachJobRecommendationsToWordbook(Long accountId, Long wordbookId, JobKey jobKey);
    List<JobRecommendedTerm> getJobRecommendations(JobKey jobKey);
}
