package com.cygnus.ipoten.interview_result.repository;

import com.cygnus.ipoten.interview_result.entity.InterviewResultDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewResultDetailRepository extends JpaRepository<InterviewResultDetail, Long> {
    List<InterviewResultDetail> findAllByInterviewResultId(Long interviewResultId);
}
