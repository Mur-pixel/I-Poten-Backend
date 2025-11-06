package com.cygnus.ipoten.interview.controller.request_form;

import com.cygnus.ipoten.interview.entity.InterviewType;
import com.cygnus.ipoten.interview.service.request.InterviewSequenceRequest;
import lombok.Getter;

@Getter
public class InterviewProgressRequestForm {

    private Long interviewId;
    private int interviewSequence;
    private InterviewType interviewType;
    private String answer;
    private Long interviewQAId;



    public InterviewSequenceRequest toInterviewSequenceRequest() {
        return new InterviewSequenceRequest(this.interviewId , this.interviewQAId,this.interviewSequence, this.answer);
    }

    public InterviewProgressRequestForm(Long interviewId, int interviewSequence, InterviewType interviewType, String answer, Long interviewQAId) {
        this.interviewId = interviewId;
        this.interviewSequence = interviewSequence;
        this.interviewType = interviewType;
        this.answer = answer;
        this.interviewQAId = interviewQAId;
    }
}
