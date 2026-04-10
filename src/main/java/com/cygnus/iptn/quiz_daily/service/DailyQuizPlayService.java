package com.cygnus.iptn.quiz_daily.service;

import com.cygnus.iptn.quiz_daily.controller.request_form.CheckDailyQuestionRequestForm;
import com.cygnus.iptn.quiz_daily.controller.response_form.CheckDailyQuestionResponseForm;

public interface DailyQuizPlayService {
    CheckDailyQuestionResponseForm checkQuestion(
            Long sessionId,
            Long questionId,
            Long accountId,
            CheckDailyQuestionRequestForm requestForm
    );
}
