package com.cygnus.iptn.interviewQA.repository;

import com.cygnus.iptn.interviewQA.entity.InterviewQA;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewQARepository extends JpaRepository<InterviewQA, Long> {
    List<InterviewQA> findByInterview_Id(Long interviewId);
}
