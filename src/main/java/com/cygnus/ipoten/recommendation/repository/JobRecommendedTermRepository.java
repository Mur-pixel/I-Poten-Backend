package com.cygnus.ipoten.recommendation.repository;

import com.cygnus.ipoten.recommendation.entity.JobRecommendedTerm;
import com.cygnus.ipoten.recommendation.entity.enums.JobKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JobRecommendedTermRepository extends JpaRepository<JobRecommendedTerm, Long> {

    int deleteByJobKey(JobKey jobKey);

    @Query("""
        select jrt
        from JobRecommendedTerm jrt
        join fetch jrt.term t
        where jrt.jobKey = :jobKey
        order by jrt.rankNo asc
    """)
    List<JobRecommendedTerm> findAllWithTermByJobKeyOrderByRankNo(@Param("jobKey") JobKey jobKey);

    @Query("""
        select jrt
        from JobRecommendedTerm jrt
        join fetch jrt.term t
        where jrt.jobKey = :jobKey
          and t.termCategory.id = :categoryId
        order by jrt.rankNo asc
    """)
    List<JobRecommendedTerm> findAllWithTermByJobKeyAndCategoryIdOrderByRankNo(
            @Param("jobKey") JobKey jobKey,
            @Param("categoryId") Long categoryId
    );
}
