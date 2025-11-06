package com.cygnus.ipoten.administer.service;


import com.cygnus.ipoten.account.entity.LoginType;
import com.cygnus.ipoten.administer.service.dto.VerificationInitialAdminDto;

import java.util.Optional;

public interface AdministratorService {
    boolean validateKey(String id,String password);
    void createAdminIfNotExists(String adminEmail, String adminNickname, LoginType adminLoginType);
    boolean isAdminByUserToken(String userToken);
    String createTemporaryAdminToken();
    boolean isTempTokenValid(String tempToken);
    Optional<VerificationInitialAdminDto> getInitialAdminInfo(String adminEmail);
}
