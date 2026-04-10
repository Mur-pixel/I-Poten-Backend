package com.cygnus.ipoten.interview_review.repository;

import com.cygnus.ipoten.interview_review.entity.InterviewReview;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewReviewRepository extends JpaRepository<InterviewReview, Long> {
}
