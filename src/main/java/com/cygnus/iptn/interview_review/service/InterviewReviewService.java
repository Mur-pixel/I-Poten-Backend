package com.cygnus.iptn.interview_review.service;

import com.cygnus.iptn.interview_review.controller.request_form.InterviewReviewRequestForm;
import com.cygnus.iptn.interview_review.entity.InterviewReview;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


public interface InterviewReviewService {
    void registerInterviewReview(String userToken,InterviewReviewRequestForm interviewReviewRequestForm);
}
