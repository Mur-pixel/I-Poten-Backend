package com.cygnus.ipoten.interview.service.strategy.sequence_strategy;

import com.cygnus.ipoten.interview.service.request.InterviewSequenceRequest;
import com.cygnus.ipoten.interview.service.response.InterviewProgressResponse;

public interface InterviewSequenceStrategy {
    InterviewProgressResponse getQuestionByCompany(InterviewSequenceRequest interviewSequenceRequest, String userToken);
}
