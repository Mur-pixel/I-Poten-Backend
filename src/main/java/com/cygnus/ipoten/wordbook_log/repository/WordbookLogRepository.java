package com.cygnus.ipoten.wordbook_log.repository;

import com.cygnus.ipoten.wordbook_log.entity.WordbookLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WordbookLogRepository extends JpaRepository<WordbookLog, Long> {
}
