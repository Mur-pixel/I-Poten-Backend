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

        QuizSession session = quizSessionRepository
                .findByIdAndAccount_IdAndDeletedAtIsNull(sessionId, accountId)
                .orElseThrow(() -> new IllegalArgumentException("session not found: " + sessionId));

        session.markDaily(ymd, issueType, questionType);
    }
}
