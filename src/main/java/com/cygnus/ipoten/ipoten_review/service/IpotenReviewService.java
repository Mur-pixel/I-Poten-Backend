package com.cygnus.ipoten.ipoten_review.service;

import com.cygnus.ipoten.ipoten_review.controller.request_form.IpotenReviewRequestForm;

public interface IpotenReviewService {

    void registerInterviewReview(String userToken, IpotenReviewRequestForm ipotenReviewRequestForm);
}
