package com.cygnus.ipoten.account.repository;

import com.cygnus.ipoten.account.entity.AccountRoleType;
import com.cygnus.ipoten.account.entity.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRoleTypeRepository extends JpaRepository<AccountRoleType, Long> {
    Optional<AccountRoleType> findByRoleType(RoleType roleType);
}
