package com.cygnus.ipoten.quiz_daily.service;

import com.cygnus.ipoten.quiz_daily.entity.enums.DailyStartMode;
import com.cygnus.ipoten.quiz_session.service.response.StartQuizSessionResponse;

import java.time.LocalDate;

public interface DailyQuizService {
    DailyStarted startGeneralDaily(Long accountId, DailyStartMode mode);

    public record DailyStarted(
            LocalDate todayYmd,                 // 서버 기준 오늘(KST)
            LocalDate activeYmd,                // 실제로 내려준 데일리(어제일 수도 있음)
            StartQuizSessionResponse choice,
            StartQuizSessionResponse ox,
            StartQuizSessionResponse initials
    ) {}
}
