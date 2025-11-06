package com.cygnus.ipoten.quiz.service;

import com.cygnus.ipoten.quiz.controller.response_form.SessionItemsPageResponseForm;
import com.cygnus.ipoten.quiz.controller.response_form.SessionListResponseForm;
import com.cygnus.ipoten.quiz.controller.response_form.SessionReviewResponseForm;
import com.cygnus.ipoten.quiz.controller.response_form.SessionSummaryResponseForm;

public interface UserQuizSessionQueryService {
    SessionSummaryResponseForm getSummary(Long sessionId, Long accountId);
    SessionItemsPageResponseForm getSessionItems(Long sessionId, Long accountId, int offset, int limit, boolean includeAnswers);
    SessionListResponseForm listMySessions(Long accountId, int limit, String statusFilter);
    SessionReviewResponseForm getReview(Long sessionId, Long accountId);
}
