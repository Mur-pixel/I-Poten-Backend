package com.cygnus.iptn.interview_score.repository;

import com.cygnus.iptn.interview_score.entity.InterviewScore;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewScoreRepository extends JpaRepository<InterviewScore, Long> {
    InterviewScore findByInterviewId(Long interviewId);
}
