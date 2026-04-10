package com.cygnus.ipoten.account.repository;

import com.cygnus.ipoten.account.entity.AccountLoginType;
import com.cygnus.ipoten.account.entity.LoginType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountLoginTypeRepository extends JpaRepository<AccountLoginType, Long> {
    Optional<AccountLoginType> findByLoginType(LoginType loginType);
}
