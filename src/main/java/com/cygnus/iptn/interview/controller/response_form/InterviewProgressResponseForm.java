package com.cygnus.iptn.interview.controller.response_form;

import lombok.Getter;

@Getter
public class InterviewProgressResponseForm {

    private Long interviewQAId;
    private Long interviewId;
    private String interviewQuestion;
    private String interviewQuestionText;

    public InterviewProgressResponseForm(Long interviewQAId, Long interviewId, String interviewQuestion, String interviewQuestionText) {
        this.interviewQAId = interviewQAId;
        this.interviewId = interviewId;
        this.interviewQuestion = interviewQuestion;
        this.interviewQuestionText = interviewQuestionText;
    }
}
