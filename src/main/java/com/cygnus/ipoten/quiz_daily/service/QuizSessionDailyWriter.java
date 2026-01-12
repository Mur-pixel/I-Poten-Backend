package com.cygnus.ipoten.quiz_daily.service;

import java.time.LocalDate;

public interface QuizSessionDailyWriter {
    void markDaily(Long sessionId, Long accountId, LocalDate ymd, String issueType, String questionType);
}
