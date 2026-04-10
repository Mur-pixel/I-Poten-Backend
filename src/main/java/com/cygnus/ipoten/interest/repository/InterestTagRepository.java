package com.cygnus.ipoten.interest.repository;

import com.cygnus.ipoten.interest.entity.InterestTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterestTagRepository extends JpaRepository<InterestTag, Long> {

    List<InterestTag> findByInterestIdInAndActiveTrueOrderBySortOrderAscIdAsc(List<Long> interestIds);

    List<InterestTag> findByIdInAndActiveTrue(List<Long> ids);
}
