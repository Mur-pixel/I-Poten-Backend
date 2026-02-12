package com.cygnus.ipoten.term_trending.repository;

import com.cygnus.ipoten.term_trending.entity.TermSearchEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermSearchEventRepository extends JpaRepository<TermSearchEvent, Long> {
}
