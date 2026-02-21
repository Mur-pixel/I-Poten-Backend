package com.cygnus.ipoten.quiz_daily.service;

import com.cygnus.ipoten.quiz_daily.entity.DailyQuizAnswer;
import com.cygnus.ipoten.quiz_daily.repository.DailyQuizAnswerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DailyQuizAnswerQueryService {

    private final DailyQuizAnswerRepository dailyQuizAnswerRepository;

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public DailyQuizAnswer getBySessionAndQuestion(Long sessionId, Long questionId) {
        return dailyQuizAnswerRepository.findBySessionIdAndQuestionId(sessionId, questionId)
                .orElseThrow(() -> new IllegalStateException("이미 채점된 답안을 찾을 수 없습니다."));
    }
}