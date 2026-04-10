package com.cygnus.iptn.iptn_review.controller;

import com.cygnus.iptn.common.annotation.LoginToken;
import com.cygnus.iptn.iptn_review.controller.request_form.IptnReviewRequestForm;
import com.cygnus.iptn.iptn_review.service.IptnReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/review/iptn")
public class IptnReviewController {

    private final IptnReviewService iptnReviewService;

    @PostMapping
    public ResponseEntity<Void> review(
            @RequestBody IptnReviewRequestForm iptnReviewRequestForm,
            @LoginToken String userToken) {

        try {
            iptnReviewService.registerInterviewReview(userToken, iptnReviewRequestForm);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
