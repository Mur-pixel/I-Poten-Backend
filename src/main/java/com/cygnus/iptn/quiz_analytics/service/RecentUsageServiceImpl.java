package com.cygnus.iptn.quiz_analytics.service;

import com.cygnus.iptn.quiz_session_answer.repository.QuizSessionAnswerRepository;
import com.cygnus.iptn.quiz_session_generator.service.util.OptionQualityChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecentUsageServiceImpl implements RecentUsageService {

    private final QuizSessionAnswerRepository quizSessionAnswerRepository;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Override
    public Set<Long> findRecentTermIds(Long accountId, int lastNDays) {
        int days = Math.max(1, lastNDays);

        Instant from = LocalDate.now(KST)
                .minusDays(days)
                .atStartOfDay(KST)
                .toInstant();

        return quizSessionAnswerRepository.findRecentTermIdsByAccountSince(accountId, from)
                .stream().collect(Collectors.toSet());
    }

    @Override
    public Set<String> findRecentChoiceNorms(Long accountId, int lastNDays) {
        int days = Math.max(1, lastNDays);

        Instant from = LocalDate.now(KST)
                .minusDays(days)
                .atStartOfDay(KST)
                .toInstant();

        return quizSessionAnswerRepository.findRecentChoiceTextsByAccountSince(accountId, from)
                .stream()
                .map(OptionQualityChecker::normalize)
                .collect(Collectors.toSet());
    }
}
