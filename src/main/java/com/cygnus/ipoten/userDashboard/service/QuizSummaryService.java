package com.cygnus.ipoten.userDashboard.service;

public interface QuizSummaryService {
    long getTotalCount(Long accountId);
    long getMonthlyCount(Long accountId);
}
