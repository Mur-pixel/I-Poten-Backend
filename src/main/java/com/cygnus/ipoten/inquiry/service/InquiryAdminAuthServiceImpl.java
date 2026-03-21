package com.cygnus.ipoten.inquiry.service;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.account.entity.AccountRoleType;
import com.cygnus.ipoten.account.entity.RoleType;
import com.cygnus.ipoten.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryAdminAuthServiceImpl implements InquiryAdminAuthService {

    private final AccountRepository accountRepository;

    @Override
    public boolean isAdmin(Long accountId) {
        if (accountId == null) {
            return false;
        }

        return accountRepository.findById(accountId)
                .map(Account::getAccountRoleType)
                .map(AccountRoleType::getRoleType)
                .map(roleType -> roleType == RoleType.ADMIN)
                .orElse(false);
    }
}