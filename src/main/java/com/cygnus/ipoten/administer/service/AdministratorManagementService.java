package com.cygnus.ipoten.administer.service;

import com.cygnus.ipoten.administer.controller.dto.AdministratorUserInfoRequest;
import com.cygnus.ipoten.administer.service.dto.AdministratorUserListResponse;

public interface AdministratorManagementService {
    AdministratorUserListResponse getUserInfo(AdministratorUserInfoRequest request);

}
