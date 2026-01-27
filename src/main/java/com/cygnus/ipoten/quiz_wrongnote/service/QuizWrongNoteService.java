package com.cygnus.ipoten.quiz_wrongnote.service;

import com.cygnus.ipoten.quiz_session_answer.entity.QuizSessionAnswer;
import com.cygnus.ipoten.quiz_wrongnote.controller.response_form.WrongNoteListResponseForm;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface QuizWrongNoteService {
    void saveWrongNotes(List<QuizSessionAnswer> answers, Long accountId);
    WrongNoteListResponseForm listWrongNotes(Long accountId, int page, int size, WrongNoteSearchCondition condition, boolean includeAnswers);
    void updateResolved(Long accountId, Long wrongNoteId, boolean resolved);
    void deleteWrongNote(Long accountId, Long wrongNoteId);
}
