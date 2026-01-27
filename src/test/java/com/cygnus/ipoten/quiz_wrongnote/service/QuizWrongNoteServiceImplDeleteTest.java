package com.cygnus.ipoten.quiz_wrongnote.service;

import com.cygnus.ipoten.quiz_question.repository.QuizChoiceRepository;
import com.cygnus.ipoten.quiz_question.repository.QuizTextAnswerRepository;
import com.cygnus.ipoten.quiz_wrongnote.repository.QuizWrongNoteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizWrongNoteServiceImplDeleteTest {

    @Mock QuizWrongNoteRepository quizWrongNoteRepository;
    @Mock QuizChoiceRepository quizChoiceRepository;
    @Mock QuizTextAnswerRepository quizTextAnswerRepository;

    @Test
    @DisplayName("오답노트 단건 삭제 성공: delete count=1")
    void deleteWrongNote_success() {
        Long accountId = 7L;
        Long wrongNoteId = 10L;

        when(quizWrongNoteRepository.deleteByIdAndAccount_Id(wrongNoteId, accountId)).thenReturn(1L);

        QuizWrongNoteServiceImpl service =
                new QuizWrongNoteServiceImpl(quizWrongNoteRepository, quizChoiceRepository, quizTextAnswerRepository);

        service.deleteWrongNote(accountId, wrongNoteId);

        verify(quizWrongNoteRepository, times(1)).deleteByIdAndAccount_Id(wrongNoteId, accountId);
        verifyNoMoreInteractions(quizWrongNoteRepository);
    }

    @Test
    @DisplayName("오답노트 단건 삭제 실패(내것 아님/없음): delete count=0 -> NoSuchElementException")
    void deleteWrongNote_notFound() {
        Long accountId = 7L;
        Long wrongNoteId = 10L;

        when(quizWrongNoteRepository.deleteByIdAndAccount_Id(wrongNoteId, accountId)).thenReturn(0L);

        QuizWrongNoteServiceImpl service =
                new QuizWrongNoteServiceImpl(quizWrongNoteRepository, quizChoiceRepository, quizTextAnswerRepository);

        assertThatThrownBy(() -> service.deleteWrongNote(accountId, wrongNoteId))
                .isInstanceOf(NoSuchElementException.class);

        verify(quizWrongNoteRepository, times(1)).deleteByIdAndAccount_Id(wrongNoteId, accountId);
        verifyNoMoreInteractions(quizWrongNoteRepository);
    }
}
