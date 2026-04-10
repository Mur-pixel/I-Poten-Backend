package com.cygnus.iptn.interview.controller.response_form;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PersonalityInterviewResultResponseForm {

    private Long interviewId;
    private List<QAItem> qaList;

    @Getter
    @Builder
    public static class QAItem {
        private String question;
        private String answer;
    }
}
