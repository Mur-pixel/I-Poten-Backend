package com.cygnus.ipoten.custom_term_learning.service;

import com.cygnus.ipoten.custom_term_learning.service.request.UpdateLearningProgressRequest;
import com.cygnus.ipoten.custom_term_learning.service.response.UpdateLearningProgressResponse;

public interface LearningProgressService {
    UpdateLearningProgressResponse updateMemorization(UpdateLearningProgressRequest request);
}
