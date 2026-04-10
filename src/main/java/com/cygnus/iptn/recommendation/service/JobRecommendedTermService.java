package com.cygnus.iptn.recommendation.service;

import com.cygnus.iptn.recommendation.entity.enums.JobKey;
import com.cygnus.iptn.recommendation.service.response.JobRecommendedTermView;
import com.cygnus.iptn.wordbook.service.response.AttachTermsBulkResponse;

import java.util.List;

public interface JobRecommendedTermService {
    AttachTermsBulkResponse attachJobRecommendationsToWordbook(Long accountId, Long wordbookId, JobKey jobKey);
    List<JobRecommendedTermView> getJobRecommendations(JobKey jobKey);
}
