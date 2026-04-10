package com.cygnus.iptn.recommendation.repository;

import com.cygnus.iptn.recommendation.entity.JobRecommendedTerm;
import com.cygnus.iptn.recommendation.entity.enums.JobKey;
import com.cygnus.iptn.recommendation.repository.projection.JobRecommendedTermViewRow;
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
        join fetch jrt.termCategory c
        where jrt.jobKey = :jobKey
        order by jrt.rankNo asc
    """)
    List<JobRecommendedTerm> findAllWithTermByJobKeyOrderByRankNo(@Param("jobKey") JobKey jobKey);

    @Query("""
        select jrt
        from JobRecommendedTerm jrt
        join fetch jrt.term t
        join fetch jrt.termCategory c
        where jrt.jobKey = :jobKey
          and t.termCategory.id = :categoryId
        order by jrt.rankNo asc
    """)
    List<JobRecommendedTerm> findAllWithTermByJobKeyAndCategoryIdOrderByRankNo(
            @Param("jobKey") JobKey jobKey,
            @Param("categoryId") Long categoryId
    );

    @Query("""
        select new com.cygnus.iptn.recommendation.repository.projection.JobRecommendedTermViewRow(
            t.id,
            t.title,
            t.description,
            jrt.rankNo,
            c.id,
            c.name
        )
        from JobRecommendedTerm jrt
        join jrt.term t
        left join t.termCategory c
        where jrt.jobKey = :jobKey
        order by jrt.rankNo asc
    """)
    List<JobRecommendedTermViewRow> findRecommendationViewsByJobKey(@Param("jobKey") JobKey jobKey);
}
