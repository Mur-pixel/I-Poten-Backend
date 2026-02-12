package com.cygnus.ipoten.wordbook_event.service;

import com.cygnus.ipoten.wordbook_event.entity.WordbookEvent;
import com.cygnus.ipoten.wordbook_event.repository.WordbookEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WordbookEventServiceImpl implements WordbookEventService {

    private final WordbookEventRepository wordbookEventRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long accountId, String eventType, Long wordbookId, Long termId, String memoStatus, Integer amount, String extra) {
        if (accountId == null || accountId <= 0) return;
        if (eventType == null || eventType.isBlank()) return;

        WordbookEvent wordbookEvent = WordbookEvent.create(accountId, eventType, wordbookId, termId, memoStatus, amount, extra);
        wordbookEventRepository.save(wordbookEvent);
    }
}
