package com.cygnus.iptn.interest.repository;

import com.cygnus.iptn.interest.entity.Interest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterestRepository extends JpaRepository<Interest, Long> {

    List<Interest> findByActiveTrueOrderBySortOrderAscIdAsc();
    List<Interest> findByIdInAndActiveTrue(List<Long> ids);
}
