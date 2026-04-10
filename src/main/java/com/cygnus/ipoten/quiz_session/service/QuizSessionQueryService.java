package com.cygnus.ipoten.quiz_session.service;

import com.cygnus.ipoten.quiz_session.service.response.InitialsQuestionsResponse;
import com.cygnus.ipoten.quiz_set.entity.enums.QuizSetType;
import com.cygnus.ipoten.quiz_analytics.controller.response_form.QuizTimelineResponseForm;
import com.cygnus.ipoten.quiz_session.controller.response_form.SessionItemsPageResponseForm;
import com.cygnus.ipoten.quiz_session.controller.response_form.SessionListResponseForm;
import com.cygnus.ipoten.quiz_session.controller.response_form.SessionReviewResponseForm;
import com.cygnus.ipoten.quiz_session.controller.response_form.SessionSummaryResponseForm;

public interface QuizSessionQueryService {
    SessionSummaryResponseForm getSummary(Long sessionId, Long accountId);
    SessionItemsPageResponseForm getSessionItems(Long sessionId, Long accountId, int offset, int limit, boolean includeAnswers);
    SessionListResponseForm listMySessions(Long accountId, int limit, String statusFilter);
    SessionReviewResponseForm getReview(Long sessionId, Long accountId);
    QuizTimelineResponseForm getTimeline(Long accountId, String q, QuizSetType part, int page, int size);
    InitialsQuestionsResponse getDailyInitialsQuestions(Long sessionId, Long accountId);
}
