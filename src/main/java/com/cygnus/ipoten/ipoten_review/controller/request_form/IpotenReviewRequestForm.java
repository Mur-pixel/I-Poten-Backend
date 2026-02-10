package com.cygnus.ipoten.ipoten_review.controller.request_form;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class IpotenReviewRequestForm {

    private final int rating;
    private final String comment;

}
