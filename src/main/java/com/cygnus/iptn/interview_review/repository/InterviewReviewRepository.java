package com.cygnus.iptn.interview_review.repository;

import com.cygnus.iptn.interview_review.entity.InterviewReview;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewReviewRepository extends JpaRepository<InterviewReview, Long> {
}
