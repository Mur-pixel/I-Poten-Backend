package com.cygnus.ipoten.ipoten_review.controller;

import com.cygnus.ipoten.common.annotation.LoginToken;
import com.cygnus.ipoten.ipoten_review.controller.request_form.IpotenReviewRequestForm;
import com.cygnus.ipoten.ipoten_review.service.IpotenReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/review/ipoten")
public class IpotenReviewController {

    private final IpotenReviewService ipotenReviewService;

    @PostMapping
    public ResponseEntity<Void> review(
            @RequestBody IpotenReviewRequestForm ipotenReviewRequestForm,
            @LoginToken String userToken) {

        try {
            ipotenReviewService.registerInterviewReview(userToken, ipotenReviewRequestForm);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
