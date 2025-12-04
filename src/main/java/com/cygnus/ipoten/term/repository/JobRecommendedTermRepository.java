package com.cygnus.ipoten.term.repository;

import com.cygnus.ipoten.custom_term_recommendation.entity.JobRecommendedTerm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobRecommendedTermRepository extends JpaRepository<JobRecommendedTerm, Long> {
    List<JobRecommendedTerm> findByJobKeyOrderByRankNoAsc(String jobKey);
}
