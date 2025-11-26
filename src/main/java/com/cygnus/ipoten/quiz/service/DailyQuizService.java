package com.cygnus.ipoten.quiz.service;

import com.cygnus.ipoten.quiz.entity.enums.JobRole;
import com.cygnus.ipoten.quiz.entity.enums.QuizSetType;
import com.cygnus.ipoten.quiz.service.response.BuiltQuizSetResponse;
import com.cygnus.ipoten.quiz.service.response.InitialsQuestionRead;

import java.time.LocalDate;
import java.util.List;

public interface DailyQuizService {
    BuiltQuizSetResponse resolve(LocalDate date, QuizSetType part, JobRole role);
    List<InitialsQuestionRead> loadInitialsQuestions(List<Long> questionIds);
}
