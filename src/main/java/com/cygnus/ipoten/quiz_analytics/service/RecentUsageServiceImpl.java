package com.cygnus.ipoten.quiz_analytics.service;

import com.cygnus.ipoten.quiz_session_answer.repository.QuizSessionAnswerRepository;
import com.cygnus.ipoten.quiz_session_generator.service.util.OptionQualityChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecentUsageServiceImpl implements RecentUsageService {

    private final QuizSessionAnswerRepository quizSessionAnswerRepository;

    @Override
    public Set<Long> findRecentTermIds(Long accountId, int lastNDays) {
        LocalDateTime from = LocalDateTime.now().minusDays(Math.max(1, lastNDays));
        return quizSessionAnswerRepository.findRecentTermIdsByAccountSince(accountId, from)
                .stream().collect(Collectors.toSet());
    }

    @Override
    public Set<String> findRecentChoiceNorms(Long accountId, int lastNDays) {
        LocalDateTime from = LocalDateTime.now().minusDays(Math.max(1, lastNDays));
        return quizSessionAnswerRepository.findRecentChoiceTextsByAccountSince(accountId, from)
                .stream()
                .map(OptionQualityChecker::normalize)
                .collect(Collectors.toSet());
    }
}
