package com.cygnus.ipoten.interview.service;

import com.cygnus.ipoten.interview.controller.response_form.UserTechStackResponse;

public interface UserTechStackService {
    UserTechStackResponse getUserTechStack(Long accountId);
}
