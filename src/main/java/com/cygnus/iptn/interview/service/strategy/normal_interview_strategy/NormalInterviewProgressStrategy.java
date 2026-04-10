package com.cygnus.iptn.interview.service.strategy.normal_interview_strategy;

import com.cygnus.iptn.interview.controller.request_form.InterviewProgressRequestForm;
import com.cygnus.iptn.interview.controller.request_form.NormalInterviewCreateRequestForm;
import com.cygnus.iptn.interview.service.response.InterviewProgressResponse;
import com.cygnus.iptn.interview.service.response.NormalInterviewProgressResponse;

public interface NormalInterviewProgressStrategy {

    NormalInterviewProgressResponse process(NormalInterviewCreateRequestForm interviewProgressRequestForm, String userToken);
}
