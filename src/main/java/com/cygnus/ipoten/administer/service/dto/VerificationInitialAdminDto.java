package com.cygnus.ipoten.administer.service.dto;

import com.cygnus.ipoten.account.entity.LoginType;
import com.cygnus.ipoten.account.entity.RoleType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor

public class VerificationInitialAdminDto {
    private final Long adminAccountId;
    private final LoginType adminLoginType;
    private final RoleType adminRoleType;

}
