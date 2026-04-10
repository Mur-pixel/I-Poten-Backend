package com.cygnus.iptn.interview_result.service;

import com.cygnus.iptn.interview.controller.request_form.InterviewResultRequestForm;
import com.cygnus.iptn.interview.controller.response_form.InterviewResultResponseForm;
import com.cygnus.iptn.interview_result.entity.InterviewResult;
import com.cygnus.iptn.interview_result.entity.InterviewResultDetail;

import java.util.List;

public interface InterviewResultService {

    InterviewResult createInterviewResult(InterviewResultRequestForm interviewResultRequestForm);
    InterviewResultResponseForm getInterviewResult(Long interviewId);
    boolean checkInterviewOwnership(Long accountId, Long interviewId);
    List<InterviewResultResponseForm.Qa> convertInterviewResultDetailToResponseFormList(List<InterviewResultDetail> interviewResultDetail);

}
