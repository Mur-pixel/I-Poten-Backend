package com.cygnus.ipoten.custom_term_learning.repository;

import com.cygnus.ipoten.custom_term_learning.entity.LearningProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LearningProgressRepository extends JpaRepository<LearningProgress, LearningProgress.Id> {
    List<LearningProgress> findByIdAccountIdAndIdTermIdIn(Long accountId, Collection<Long> termIds);
    Optional<LearningProgress> findByIdAccountIdAndIdTermId(Long accountId, Long termId);
}