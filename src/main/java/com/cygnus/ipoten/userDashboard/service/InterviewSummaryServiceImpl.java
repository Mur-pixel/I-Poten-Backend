package com.cygnus.ipoten.userDashboard.service;

import com.cygnus.ipoten.interview.repository.InterviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class InterviewSummaryServiceImpl implements InterviewSummaryService {

    private final InterviewRepository interviewRepository;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Override
    @Transactional(readOnly = true)
    public long getMonthlyFinishedCount(Long accountId) {
        YearMonth now = YearMonth.now(KST);
        LocalDateTime start = now.atDay(1).atStartOfDay();
        LocalDateTime end = now.atEndOfMonth().atTime(23, 59, 59);
        return interviewRepository.countFinishedInterviewsThisMonth(accountId, start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalFinishedCount(Long accountId) {
        return interviewRepository.countTotalFinishedInterviews(accountId);
    }
}
