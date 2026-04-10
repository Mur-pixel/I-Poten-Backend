package com.cygnus.ipoten.interview.service.strategy.normal_interview_strategy;

import com.cygnus.ipoten.interview.controller.request_form.InterviewProgressRequestForm;
import com.cygnus.ipoten.interview.controller.request_form.NormalInterviewCreateRequestForm;
import com.cygnus.ipoten.interview.service.response.InterviewProgressResponse;
import com.cygnus.ipoten.interview.service.response.NormalInterviewProgressResponse;

public interface NormalInterviewProgressStrategy {

    NormalInterviewProgressResponse process(NormalInterviewCreateRequestForm interviewProgressRequestForm, String userToken);
}
