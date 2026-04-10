package com.cygnus.ipoten.interview.controller.request;

import com.cygnus.ipoten.interview.entity.Interview;
import lombok.Getter;

@Getter
public class InterviewQARequest {

    private String firstQuestion;
    private String firstAnswer;
    private Interview interview;

    public InterviewQARequest(Interview interview,String firstQuestion, String firstAnswer) {
        this.interview = interview;
        this.firstQuestion = firstQuestion;
        this.firstAnswer = firstAnswer;
    }

    public InterviewQARequest() {
    }
}
