package com.cygnus.iptn.accountProfile.repository;

import com.cygnus.iptn.account.entity.LoginType;
import com.cygnus.iptn.accountProfile.entity.AccountProfile;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AccountProfileRepository extends JpaRepository<AccountProfile, Long> {

    @Query("SELECT ap FROM AccountProfile ap JOIN FETCH ap.account WHERE ap.account.id = :accountId")
    Optional<AccountProfile> findByAccountId(@Param("accountId") Long accountId);

    @Query("SELECT ap FROM AccountProfile ap JOIN FETCH ap.account a WHERE ap.email = :email AND a.accountLoginType.loginType = :loginType")
    Optional<AccountProfile> findWithAccountByEmailAndLoginType(@Param("email") String email, @Param("loginType") LoginType loginType);

    //2025.09.13 발키리 추가
    @Query("SELECT ap FROM AccountProfile ap JOIN FETCH ap.account WHERE ap.email = :email")
    Optional<AccountProfile> findWithAccountByEmail(@Param("email") String email);


}
