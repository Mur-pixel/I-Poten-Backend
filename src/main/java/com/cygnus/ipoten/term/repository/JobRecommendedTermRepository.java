package com.cygnus.ipoten.term.repository;

import com.cygnus.ipoten.term.entity.JobRecommendedTerm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobRecommendedTermRepository extends JpaRepository<JobRecommendedTerm, Long> {
    List<JobRecommendedTerm> findByJobKeyOrderByRankNoAsc(String jobKey);
}
