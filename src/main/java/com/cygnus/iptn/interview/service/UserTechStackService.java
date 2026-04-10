package com.cygnus.iptn.interview.service;

import com.cygnus.iptn.interview.controller.response_form.UserTechStackResponse;

public interface UserTechStackService {
    UserTechStackResponse getUserTechStack(Long accountId);
}
