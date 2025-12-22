package com.cygnus.ipoten.recommendation.service;

import com.cygnus.ipoten.wordbook.service.response.AttachTermsBulkResponse;

public interface CategoryRecommendationService {
    AttachTermsBulkResponse attachCategoryRecommendationsToWordbook(Long accountId, Long wordbookId, Long termCategoryId);
}
