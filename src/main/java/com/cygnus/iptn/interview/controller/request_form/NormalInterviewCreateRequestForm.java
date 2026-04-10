package com.cygnus.iptn.interview.controller.request_form;


import com.cygnus.iptn.interview.entity.CandidateStatus;
import com.cygnus.iptn.interview.entity.InterviewType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NormalInterviewCreateRequestForm {

    private InterviewType interviewType;
    private CandidateStatus candidateStatus;
    private String self_concern;









}
