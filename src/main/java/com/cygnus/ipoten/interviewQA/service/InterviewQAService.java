package com.cygnus.ipoten.interviewQA.service;

import com.cygnus.ipoten.interview.controller.request.InterviewQARequest;
import com.cygnus.ipoten.interview.entity.Interview;
import com.cygnus.ipoten.interviewQA.entity.InterviewQA;

import java.util.List;
import java.util.Optional;

public interface InterviewQAService {


    void saveInterviewQAByInterview(Interview interview, InterviewQA interviewQA);
    InterviewQA createInterviewQA(InterviewQARequest interviewQARequest);
    InterviewQA createInterviewQuestion(Interview interview, String interviewQuestion);
    InterviewQA createInterviewQaByInterview(Interview interview);
    InterviewQA saveInterviewAnswer(Long interviewQAId, String interviewAnswer);
    Optional<InterviewQA> findById(Long interviewQAId);
    List<InterviewQA> findAllByInterviewId(Long interviewId);


}
