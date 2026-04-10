package com.cygnus.iptn.iptn_review.controller.request_form;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class IptnReviewRequestForm {

    private final int rating;
    private final String comment;

}
