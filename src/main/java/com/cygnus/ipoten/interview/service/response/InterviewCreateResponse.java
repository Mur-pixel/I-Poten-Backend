package com.cygnus.ipoten.interview.service.response;

import lombok.Getter;

@Getter
public class InterviewCreateResponse {

    private Long interviewId;
    private Long interviewQAId;
    private String interviewQuestion;

    public InterviewCreateResponse(String interviewQuestion, Long interviewQAId, Long interviewId) {
        this.interviewQuestion = interviewQuestion;
        this.interviewQAId = interviewQAId;
        this.interviewId = interviewId;
    }

    public InterviewCreateResponse() {
    }


}
