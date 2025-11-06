package com.cygnus.ipoten.quiz.service;

import com.cygnus.ipoten.quiz.repository.SessionAnswerRepository;
import com.cygnus.ipoten.quiz.service.util.OptionQualityChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecentUsageServiceImpl implements RecentUsageService {

    private final SessionAnswerRepository sessionAnswerRepository;

    @Override
    public Set<Long> findRecentTermIds(Long accountId, int lastNDays) {
        LocalDateTime from = LocalDateTime.now().minusDays(Math.max(1, lastNDays));
        return sessionAnswerRepository.findRecentTermIdsByAccountSince(accountId, from)
                .stream().collect(Collectors.toSet());
    }

    @Override
    public Set<String> findRecentChoiceNorms(Long accountId, int lastNDays) {
        LocalDateTime from = LocalDateTime.now().minusDays(Math.max(1, lastNDays));
        return sessionAnswerRepository.findRecentChoiceTextsByAccountSince(accountId, from)
                .stream()
                .map(OptionQualityChecker::normalize)
                .collect(Collectors.toSet());
    }
}
