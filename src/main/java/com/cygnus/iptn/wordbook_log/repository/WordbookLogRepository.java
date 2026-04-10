package com.cygnus.iptn.wordbook_log.repository;

import com.cygnus.iptn.wordbook_log.entity.WordbookLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WordbookLogRepository extends JpaRepository<WordbookLog, Long> {
}
