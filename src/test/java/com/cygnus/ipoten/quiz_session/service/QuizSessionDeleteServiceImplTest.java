package com.cygnus.ipoten.quiz_session.service;

import com.cygnus.ipoten.quiz_session.entity.QuizSession;
import com.cygnus.ipoten.quiz_session.repository.QuizSessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizSessionDeleteServiceImplTest {

    @Mock
    QuizSessionRepository quizSessionRepository;

    @Test
    @DisplayName("내 세션 삭제(soft delete) 성공: deletedAt이 찍힌다")
    void deleteMySession_success_marksDeletedAt() {
        // given
        Long accountId = 7L;
        Long sessionId = 10L;

        QuizSession session = new QuizSession(); // 엔티티 public no-args 생성자 OK

        when(quizSessionRepository.findByIdAndAccount_IdAndDeletedAtIsNull(sessionId, accountId))
                .thenReturn(Optional.of(session));

        QuizSessionDeleteServiceImpl service = new QuizSessionDeleteServiceImpl(quizSessionRepository);

        Instant before = Instant.now();

        // when
        service.deleteMySession(accountId, sessionId);

        Instant after = Instant.now();

        // then
        assertThat(session.getDeletedAt()).isNotNull();
        assertThat(session.getDeletedAt()).isBetween(before.minusSeconds(1), after.plusSeconds(1));

        verify(quizSessionRepository, times(1))
                .findByIdAndAccount_IdAndDeletedAtIsNull(sessionId, accountId);

        // 서비스는 save를 호출하지 않고 dirty checking에 맡기는 구조(소프트 삭제)
        verify(quizSessionRepository, never()).save(any());
        verifyNoMoreInteractions(quizSessionRepository);
    }

    @Test
    @DisplayName("남의 세션/없는 세션/이미 삭제된 세션이면: NoSuchElementException")
    void deleteMySession_notFound_throws() {
        // given
        Long accountId = 7L;
        Long sessionId = 10L;

        when(quizSessionRepository.findByIdAndAccount_IdAndDeletedAtIsNull(sessionId, accountId))
                .thenReturn(Optional.empty());

        QuizSessionDeleteServiceImpl service = new QuizSessionDeleteServiceImpl(quizSessionRepository);

        // when & then
        assertThatThrownBy(() -> service.deleteMySession(accountId, sessionId))
                .isInstanceOf(NoSuchElementException.class);

        verify(quizSessionRepository, times(1))
                .findByIdAndAccount_IdAndDeletedAtIsNull(sessionId, accountId);

        verifyNoMoreInteractions(quizSessionRepository);
    }
}
