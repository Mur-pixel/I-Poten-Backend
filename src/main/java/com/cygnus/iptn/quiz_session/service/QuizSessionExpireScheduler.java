package com.cygnus.iptn.quiz_session.service;

import com.cygnus.iptn.quiz_session.repository.QuizSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizSessionExpireScheduler {

    private final QuizSessionRepository quizSessionRepository;

    // "이탈 3시간" 기준
    private static final Duration EXPIRE_AFTER = Duration.ofHours(3);

    // "3시간마다" 실행
    private static final long RUN_EVERY_MS = 3 * 60 * 60 * 1000L;

    @Scheduled(fixedDelay = RUN_EVERY_MS, initialDelay = 60_000L) // 앱 뜨고 1분 뒤부터
    @Transactional
    public void expireStaleSessions() {
        Instant cutoff = Instant.now().minus(EXPIRE_AFTER);

        int updated = quizSessionRepository.expireStaleInProgress(cutoff);

        if (updated > 0) {
            log.info("[quiz-session-expire] expired {} sessions (cutoff={})", updated, cutoff);
        }
    }
}
