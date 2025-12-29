package com.cygnus.ipoten.quiz_daily.service;

import com.cygnus.ipoten.quiz_session.service.response.StartQuizSessionResponse;

import java.time.LocalDate;

public interface DailyQuizService {
    DailyStarted startGeneralDaily(Long accountId);

    public record DailyStarted(
            LocalDate ymd,
            StartQuizSessionResponse choice,
            StartQuizSessionResponse ox,
            StartQuizSessionResponse initials
    ) {}
}
