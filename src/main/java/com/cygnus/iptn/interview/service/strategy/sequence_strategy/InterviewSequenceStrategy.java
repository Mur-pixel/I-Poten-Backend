package com.cygnus.iptn.interview.service.strategy.sequence_strategy;

import com.cygnus.iptn.interview.service.request.InterviewSequenceRequest;
import com.cygnus.iptn.interview.service.response.InterviewProgressResponse;

public interface InterviewSequenceStrategy {
    InterviewProgressResponse getQuestionByCompany(InterviewSequenceRequest interviewSequenceRequest, String userToken);
}
