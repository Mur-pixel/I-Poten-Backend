package com.cygnus.ipoten.interview.service;

import com.cygnus.ipoten.interview.controller.request_form.AdminInterviewHistoryRequestForm;
import com.cygnus.ipoten.interview.controller.request_form.AdminInterviewUsersRequestForm;
import com.cygnus.ipoten.interview.controller.response_form.AdminInterviewDetailResponseForm;
import com.cygnus.ipoten.interview.controller.response_form.AdminInterviewHistoryResponseForm;
import com.cygnus.ipoten.interview.controller.response_form.AdminInterviewUsersResponseForm;

public interface AdminInterviewManagementService {

    AdminInterviewUsersResponseForm getInterviewUserList(AdminInterviewUsersRequestForm request);

    AdminInterviewHistoryResponseForm getInterviewHistory(Long userId, AdminInterviewHistoryRequestForm request);

    AdminInterviewDetailResponseForm getInterviewDetail(Long interviewId);
}
