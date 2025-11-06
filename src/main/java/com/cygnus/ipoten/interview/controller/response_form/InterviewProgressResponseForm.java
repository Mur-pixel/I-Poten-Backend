package com.cygnus.ipoten.interview.controller.response_form;

import lombok.Getter;

@Getter
public class InterviewProgressResponseForm {

    private Long interviewQAId;
    private Long interviewId;
    private String interviewQuestion;

    public InterviewProgressResponseForm(Long interviewQAId, Long interviewId, String interviewQuestion) {
        this.interviewQAId = interviewQAId;
        this.interviewId = interviewId;
        this.interviewQuestion = interviewQuestion;
    }
}
