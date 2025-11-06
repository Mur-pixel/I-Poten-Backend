package com.cygnus.ipoten.interview_score.service;

import com.cygnus.ipoten.interview.controller.request_form.InterviewResultRequestForm;
import com.cygnus.ipoten.interview_score.entity.InterviewScore;

public interface InterviewScoreService {

    InterviewScore createInterviewScore(InterviewResultRequestForm  interviewResultRequestForm);
    InterviewScore findByInterviewId(Long interviewId);

}
