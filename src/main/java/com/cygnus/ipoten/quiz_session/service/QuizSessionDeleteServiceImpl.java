package com.cygnus.ipoten.quiz_session.service;

import com.cygnus.ipoten.quiz_session.entity.QuizSession;
import com.cygnus.ipoten.quiz_session.repository.QuizSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class QuizSessionDeleteServiceImpl implements QuizSessionDeleteService {

    private final QuizSessionRepository quizSessionRepository;

    @Override
    @Transactional
    public void deleteMySession(Long accountId, Long sessionId) {

        // 자신의 세션인지 및 아직 삭제 안 됐는지 검증
        QuizSession session = quizSessionRepository.findByIdAndAccount_IdAndDeletedAtIsNull(sessionId, accountId)
                .orElseThrow(NoSuchElementException::new);

        // 세션 소프트 삭제
        session.markDeleted(Instant.now());
    }
}
