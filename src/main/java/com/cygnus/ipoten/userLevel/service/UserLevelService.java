package com.cygnus.ipoten.userLevel.service;

import com.cygnus.ipoten.userLevel.controller.response.UserLevelResponse;

public interface UserLevelService {
//    void initLevel(Long accountId);
    UserLevelResponse getUserLevel(Long accountId);
    UserLevelResponse addExp(Long accountId, int amount);
}
