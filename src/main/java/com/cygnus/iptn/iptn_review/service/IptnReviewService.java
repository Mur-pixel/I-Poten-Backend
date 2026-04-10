package com.cygnus.iptn.iptn_review.service;

import com.cygnus.iptn.iptn_review.controller.request_form.IptnReviewRequestForm;

public interface IptnReviewService {

    void registerInterviewReview(String userToken, IptnReviewRequestForm iptnReviewRequestForm);
}
