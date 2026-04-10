package com.cygnus.ipoten.interview.controller.request_form;


import com.cygnus.ipoten.interview.entity.CandidateStatus;
import com.cygnus.ipoten.interview.entity.InterviewType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NormalInterviewCreateRequestForm {

    private InterviewType interviewType;
    private CandidateStatus candidateStatus;
    private String self_concern;









}
