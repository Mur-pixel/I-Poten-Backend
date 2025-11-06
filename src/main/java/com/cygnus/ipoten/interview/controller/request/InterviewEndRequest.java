package com.cygnus.ipoten.interview.controller.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.*;

@Getter
@NoArgsConstructor
public class InterviewEndRequest {

    private String userToken;
    private Long interviewId;
    private List<Integer> questionId = List.of(1, 2, 3, 4, 5, 6);
    private Map<String, String> context = new HashMap<>();;
    private List<String> questions;
    private List<String> answers;
    private String callbackUrl;


    public InterviewEndRequest(String userToken, Long interviewId, List<String> questions, List<String> answers, String callbackUrl) {
        this.userToken = userToken;
        this.interviewId = interviewId;
        this.questions = questions;
        this.answers = answers;
        this.callbackUrl = callbackUrl;
    }
}
