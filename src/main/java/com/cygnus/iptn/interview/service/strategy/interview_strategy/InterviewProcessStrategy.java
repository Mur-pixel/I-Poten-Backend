package com.cygnus.iptn.interview.service.strategy.interview_strategy;

import com.cygnus.iptn.interview.controller.request_form.InterviewProgressRequestForm;
import com.cygnus.iptn.interview.controller.request_form.NormalInterviewCreateRequestForm;
import com.cygnus.iptn.interview.service.response.InterviewProgressResponse;
import com.cygnus.iptn.interview.service.response.NormalInterviewProgressResponse;

public interface InterviewProcessStrategy {
    InterviewProgressResponse process(InterviewProgressRequestForm interviewProgressRequestForm, String userToken);

}
