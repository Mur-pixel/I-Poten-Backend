package com.cygnus.iptn.interview_result.service;

import com.cygnus.iptn.interview.controller.request_form.InterviewResultRequestForm;
import com.cygnus.iptn.interview_result.entity.InterviewResultDetail;

import java.util.List;

public interface InterviewResultDetailService {

    List<InterviewResultDetail> createInterviewResultDetail(InterviewResultRequestForm interviewResultRequestForm, Long interviewResultId);
    List<InterviewResultDetail> findAllByInterviewResultId(Long interviewResultId);

}
