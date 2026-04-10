package com.cygnus.iptn.interview_score.service;

import com.cygnus.iptn.interview.controller.request_form.InterviewResultRequestForm;
import com.cygnus.iptn.interview_score.entity.InterviewScore;

public interface InterviewScoreService {

    InterviewScore createInterviewScore(InterviewResultRequestForm  interviewResultRequestForm);
    InterviewScore findByInterviewId(Long interviewId);

}
