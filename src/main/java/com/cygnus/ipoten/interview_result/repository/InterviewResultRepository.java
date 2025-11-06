package com.cygnus.ipoten.interview_result.repository;

import com.cygnus.ipoten.interview_result.entity.InterviewResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InterviewResultRepository extends JpaRepository<InterviewResult, Long> {
    Optional<Object> findByInterview_Id(Long interviewId);

    InterviewResult findByInterviewId(Long interviewId);
}
