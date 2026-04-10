package com.cygnus.iptn.account.service;

import com.cygnus.iptn.account.entity.Account;
import com.cygnus.iptn.account.entity.AccountRoleType;
import com.cygnus.iptn.account.entity.LoginType;
import com.cygnus.iptn.account.service.register_request.RegisterAccountRequest;

import java.util.Optional;

public interface AccountService {

    Optional<Account> createAccount(RegisterAccountRequest requestForm);
    Optional<Account> createAccountWithRoleType(AccountRoleType accountRoleType, LoginType loginType);
    Optional<Account> findById(Long id);

    void withdraw(String userToken);

}
