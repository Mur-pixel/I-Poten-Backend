package com.cygnus.iptn.wordbook_log.service;

import com.cygnus.iptn.wordbook_log.entity.WordbookLog;
import com.cygnus.iptn.wordbook_log.repository.WordbookLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WordbookLogServiceImpl implements WordbookLogService {

    private final WordbookLogRepository wordbookLogRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long accountId, String eventType, Long wordbookId, Long termId, String memoStatus, Integer amount, String extra) {
        if (accountId == null || accountId <= 0) return;
        if (eventType == null || eventType.isBlank()) return;

        WordbookLog wordbookLog = WordbookLog.create(accountId, eventType, wordbookId, termId, memoStatus, amount, extra);
        wordbookLogRepository.save(wordbookLog);
    }
}
