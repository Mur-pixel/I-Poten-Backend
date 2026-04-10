package com.cygnus.iptn.account.repository;

import com.cygnus.iptn.account.entity.AccountRoleType;
import com.cygnus.iptn.account.entity.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRoleTypeRepository extends JpaRepository<AccountRoleType, Long> {
    Optional<AccountRoleType> findByRoleType(RoleType roleType);
}
