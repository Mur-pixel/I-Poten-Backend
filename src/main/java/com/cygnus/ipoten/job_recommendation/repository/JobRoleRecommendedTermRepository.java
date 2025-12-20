package com.cygnus.ipoten.job_recommendation.repository;

import com.cygnus.ipoten.job.enums.JobRole;
import com.cygnus.ipoten.job_recommendation.entity.JobRoleRecommendedTerm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobRoleRecommendedTermRepository extends JpaRepository<JobRoleRecommendedTerm, Long> {
    List<JobRoleRecommendedTerm> findByJobRoleOrderByRankNoAsc(JobRole jobRole);
}
