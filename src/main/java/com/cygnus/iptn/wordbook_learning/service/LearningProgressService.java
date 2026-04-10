package com.cygnus.iptn.wordbook_learning.service;

import com.cygnus.iptn.wordbook_learning.service.request.UpdateLearningProgressRequest;
import com.cygnus.iptn.wordbook_learning.service.response.UpdateLearningProgressResponse;

public interface LearningProgressService {
    UpdateLearningProgressResponse updateMemorization(UpdateLearningProgressRequest request);
}
