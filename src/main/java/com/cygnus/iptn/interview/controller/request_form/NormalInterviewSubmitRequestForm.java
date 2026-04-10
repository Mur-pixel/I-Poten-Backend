package com.cygnus.iptn.interview.controller.request_form;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class NormalInterviewSubmitRequestForm {

    private Long interviewId;
    private List<QAItem> qaList;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class QAItem {
        private String question;
        private String answer;
    }
}
