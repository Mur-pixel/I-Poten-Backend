package com.cygnus.ipoten.accountProfile.repository;

import com.cygnus.ipoten.account.entity.LoginType;
import com.cygnus.ipoten.accountProfile.entity.AccountProfile;
import com.cygnus.ipoten.administer.service.dto.AccountProfileRow;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AccountProfileRepository extends JpaRepository<AccountProfile, Long> {

    Optional<AccountProfile> findByAccountId(Long accountId);

    @Query("SELECT ap FROM AccountProfile ap JOIN FETCH ap.account a WHERE ap.email = :email AND a.accountLoginType.loginType = :loginType")
    Optional<AccountProfile> findWithAccountByEmailAndLoginType(@Param("email") String email, @Param("loginType") LoginType loginType);

    //2025.09.13 발키리 추가
    @Query("SELECT ap FROM AccountProfile ap WHERE ap.email = :email")
    Optional<AccountProfile> findWithAccountByEmail(@Param("email") String email);

    // 닉네임 중복 체크
    boolean existsByNickname(String nickname);

    @Query(value = """
    SELECT ap.account_id AS accountId,
           ap.nickname   AS nickname,
           ap.email      AS email
    FROM account_profile ap
    WHERE ap.account_id > ?1
    ORDER BY ap.account_id ASC
    LIMIT ?2
""", nativeQuery = true)
    List<AccountProfileRow> findNextProfilesAfterId(long lastId, int limit);
}