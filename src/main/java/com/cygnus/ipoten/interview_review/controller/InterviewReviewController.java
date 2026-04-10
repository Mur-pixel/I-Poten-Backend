package com.cygnus.ipoten.interview_review.controller;

import com.cygnus.ipoten.common.annotation.LoginToken;
import com.cygnus.ipoten.interview_review.controller.request_form.InterviewReviewRequestForm;
import com.cygnus.ipoten.interview_review.service.InterviewReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/review/interview")
public class InterviewReviewController {

    private final InterviewReviewService interviewReviewService;

    @PostMapping
    public ResponseEntity<Void> review(
            @RequestBody InterviewReviewRequestForm interviewReviewRequestForm,
            @LoginToken String userToken) {

        try {
            log.info("리뷰 요청옴");
            interviewReviewService.registerInterviewReview(userToken, interviewReviewRequestForm);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
