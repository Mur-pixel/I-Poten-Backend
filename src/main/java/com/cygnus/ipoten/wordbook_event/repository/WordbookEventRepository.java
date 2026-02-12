package com.cygnus.ipoten.wordbook_event.repository;

import com.cygnus.ipoten.wordbook_event.entity.WordbookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WordbookEventRepository extends JpaRepository<WordbookEvent, Long> {
}
