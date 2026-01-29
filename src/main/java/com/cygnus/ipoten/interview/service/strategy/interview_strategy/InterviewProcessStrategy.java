package com.cygnus.ipoten.interview.service.strategy.interview_strategy;

import com.cygnus.ipoten.interview.controller.request_form.InterviewProgressRequestForm;
import com.cygnus.ipoten.interview.controller.request_form.NormalInterviewCreateRequestForm;
import com.cygnus.ipoten.interview.service.response.InterviewProgressResponse;
import com.cygnus.ipoten.interview.service.response.NormalInterviewProgressResponse;

public interface InterviewProcessStrategy {
    InterviewProgressResponse process(InterviewProgressRequestForm interviewProgressRequestForm, String userToken);

}
