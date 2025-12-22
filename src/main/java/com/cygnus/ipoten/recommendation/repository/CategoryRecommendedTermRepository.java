package com.cygnus.ipoten.recommendation.repository;

import com.cygnus.ipoten.recommendation.entity.CategoryRecommendedTerm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRecommendedTermRepository extends JpaRepository<CategoryRecommendedTerm, Long> {
    List<CategoryRecommendedTerm> findByTermCategoryIdOrderByRankNoAsc(Long termCategoryId);
}
