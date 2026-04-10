package com.cygnus.iptn.interview_review.controller.request_form;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class InterviewReviewRequestForm {

    private final int rating;
    private final String comment;


}
