package com.cygnus.iptn.quiz_session.controller.response_form;

import com.cygnus.iptn.quiz_session.entity.QuizSession;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RenameQuizSessionTitleResponseForm {

    private Long sessionId;
    private String title;

    public static RenameQuizSessionTitleResponseForm from(QuizSession quizSession) {
        return new RenameQuizSessionTitleResponseForm(quizSession.getId(), quizSession.getTitle());
    }
}
