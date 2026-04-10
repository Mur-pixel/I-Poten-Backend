package com.cygnus.iptn.quiz_analytics.service;

import com.cygnus.iptn.quiz_analytics.controller.response_form.QuizTrendResponseForm;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

public interface QuizAnalyticsQueryService {
    QuizTrendResponseForm getTrend(Long accountId, String metric, String span);
    Map<LocalDate, Double> querySets(Long aid, Instant from, Instant to);
    Map<LocalDate, Double> queryRetryRate(Long aid, Instant from, Instant to);
    Map<LocalDate, Double> queryAccuracy(Long aid, Instant from, Instant to);
    long getTotalSets(Long accountId);
}
