package com.cygnus.ipoten.quiz.service;

import com.cygnus.ipoten.quiz.controller.response_form.*;
import com.cygnus.ipoten.quiz.entity.enums.QuizSetType;

public interface UserQuizSessionQueryService {
    SessionSummaryResponseForm getSummary(Long sessionId, Long accountId);
    SessionItemsPageResponseForm getSessionItems(Long sessionId, Long accountId, int offset, int limit, boolean includeAnswers);
    SessionListResponseForm listMySessions(Long accountId, int limit, String statusFilter);
    SessionReviewResponseForm getReview(Long sessionId, Long accountId);
    TimelineResponseForm getTimeline(Long accountId, String q, QuizSetType part, int page, int size);
}
