package com.cygnus.iptn.interview.controller.response_form;


import com.cygnus.iptn.interview.service.response.InterviewCreateResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class InterviewCreateResponseForm {

    private final Long interviewId;
    private final Long interviewQAId;
    private final String interviewQuestion;
    private final String interviewQuestionText;




    public static InterviewCreateResponseForm of(InterviewCreateResponse interviewCreateResponse) {
        return new InterviewCreateResponseForm(
                interviewCreateResponse.getInterviewQAId(),
                interviewCreateResponse.getInterviewId(),
                interviewCreateResponse.getInterviewQuestion(),
                interviewCreateResponse.getInterviewQuestionText()
        );
    }

}
