package com.cygnus.ipoten.quiz_session.service;

import com.cygnus.ipoten.quiz_session.entity.QuizSession;
import com.cygnus.ipoten.quiz_session.repository.QuizSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class QuizSessionRenameServiceImpl implements QuizSessionRenameService {

    private final QuizSessionRepository quizSessionRepository;

    @Override
    @Transactional
    public QuizSession renameTitle(Long sessionId, Long accountId, String rawTitle) {

        String title = (rawTitle == null) ? "" : rawTitle.trim();

        if (title.isBlank()) {
            throw new IllegalArgumentException("title은 비어 있을 수 없습니다");
        }

        if (title.length() > 60) {
            throw new IllegalArgumentException("title은 60자를 넘을 수 없습니다.");
        }

        QuizSession session = quizSessionRepository
                .findByIdAndAccount_IdAndDeletedAtIsNull(sessionId, accountId)
                .orElseThrow(NoSuchElementException::new);

        session.changeTitle(title);
        return session;
    }
}
