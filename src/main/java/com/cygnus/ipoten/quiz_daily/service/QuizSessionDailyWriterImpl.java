package com.cygnus.ipoten.quiz_daily.service;

import com.cygnus.ipoten.quiz_session.entity.QuizSession;
import com.cygnus.ipoten.quiz_session.repository.QuizSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class QuizSessionDailyWriterImpl implements QuizSessionDailyWriter {

    private final QuizSessionRepository quizSessionRepository;

    @Transactional
    public void markDaily(Long sessionId, Long accountId, LocalDate ymd, String issueType, String questionType) {
        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("session not found: " + sessionId));

        Long ownerId = (session.getAccount() == null) ? null : session.getAccount().getId();
        if (ownerId == null || !ownerId.equals(accountId)) {
            throw new IllegalStateException("account not found: " + accountId);
        }

        session.markDaily(ymd, issueType, questionType);
    }
}
