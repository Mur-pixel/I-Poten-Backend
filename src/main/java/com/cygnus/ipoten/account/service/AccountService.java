package com.cygnus.ipoten.account.service;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.account.entity.AccountRoleType;
import com.cygnus.ipoten.account.entity.LoginType;
import com.cygnus.ipoten.account.service.register_request.RegisterAccountRequest;

import java.util.Optional;

public interface AccountService {

    Optional<Account> createAccount(RegisterAccountRequest requestForm);
    Optional<Account> createAccountWithRoleType(AccountRoleType accountRoleType, LoginType loginType);
    Optional<Account> findById(Long id);

    void withdraw(String userToken);

}
