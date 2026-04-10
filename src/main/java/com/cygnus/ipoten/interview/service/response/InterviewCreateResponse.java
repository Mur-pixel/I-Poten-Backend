package com.cygnus.ipoten.interview.service.response;

import lombok.Getter;

@Getter
public class InterviewCreateResponse {

    private Long interviewId;
    private Long interviewQAId;
    private String interviewQuestion;
    private String interviewQuestionText;

    public InterviewCreateResponse(String interviewQuestion, Long interviewQAId, Long interviewId,  String interviewQuestionText) {
        this.interviewQuestion = interviewQuestion;
        this.interviewQAId = interviewQAId;
        this.interviewId = interviewId;
        this.interviewQuestionText = interviewQuestionText;
    }

    public InterviewCreateResponse() {
    }


}
