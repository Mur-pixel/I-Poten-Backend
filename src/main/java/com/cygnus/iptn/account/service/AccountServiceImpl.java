package com.cygnus.iptn.account.service;

import com.cygnus.iptn.account.entity.*;
import com.cygnus.iptn.account.exception.NotLoggedInException;
import com.cygnus.iptn.account.exception.UserNotFoundException;
import com.cygnus.iptn.account.repository.AccountLoginTypeRepository;
import com.cygnus.iptn.account.repository.AccountRepository;
import com.cygnus.iptn.account.repository.AccountRoleTypeRepository;
import com.cygnus.iptn.account.service.register_request.RegisterAccountRequest;
import com.cygnus.iptn.accountProfile.repository.AccountProfileRepository;
import com.cygnus.iptn.accountProfile.service.AccountProfileService;
import com.cygnus.iptn.authentication.service.AuthenticationService;
import com.cygnus.iptn.mobile_auth.service.RefreshTokenService;
import com.cygnus.iptn.redis_cache.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final AccountLoginTypeRepository accountLoginTypeRepository;
    private final AccountRoleTypeRepository accountRoleTypeRepository;
    private final RedisCacheService redisCacheService;
    private final AuthenticationService authenticationService;
    private final RefreshTokenService refreshTokenService;
    private final AccountProfileRepository accountProfileRepository;

    private final AccountProfileService accountProfileService;



    @Override
    @Transactional
    public Optional<Account> createAccount(RegisterAccountRequest requestForm) {

        AccountRoleType accountRoleType = accountRoleTypeRepository.findByRoleType(RoleType.USER)
                .orElseThrow(() -> new IllegalArgumentException("RoleType.USER 가 DB에 존재하지 않습니다"));

        LoginType loginType = requestForm.getLoginType();
        AccountLoginType accountLoginType = accountLoginTypeRepository.findByLoginType(loginType)
                .orElseThrow(() -> new IllegalArgumentException("LoginType.%s 가 DB에 존재하지 않습니다.".formatted(loginType)));
//        return createAccountWithRoleType(accountRoleType,loginType);

        // 1️⃣ 계정 생성
        Account account = accountRepository.save(new Account(accountRoleType, accountLoginType));
        Long accountId = account.getId();


        // 3️⃣ 결과 반환
        return Optional.of(account);
    }

    public Optional<Account> createAccountWithRoleType(AccountRoleType accountRoleType, LoginType loginType) {


        log.info("로그인 타입 : {}", loginType);
        AccountLoginType accountLoginType = accountLoginTypeRepository.findByLoginType(loginType)
                .orElseThrow(() -> new IllegalArgumentException("LoginType.%s 가 DB에 존재하지 않습니다".formatted(loginType)));

        Account account = new Account(accountRoleType, accountLoginType);
        accountRepository.save(account);
        return Optional.of(account);
    }

    @Override
    public Optional<Account> findById(Long id) {
        return accountRepository.findById(id);
    }


    @Override
    @Transactional
    public void withdraw(String userToken) {
        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);

        // 로그인 상태가 아닐 때
        if (accountId == null) {
            throw new NotLoggedInException("회원이 로그인 상태가 아닙니다.");
        }

        // 계정을 찾고 삭제
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new UserNotFoundException("해당하는 계정을 찾을 수 없습니다."));

        // 탈퇴한 회원인지 체크
        if (account.isWithdrawn()) {
            throw new IllegalStateException("이미 탈퇴한 회원입니다.");
        }

        // 계정은 남기고 상태/탈퇴 시각만 기록
        account.markWithdrawn();

        // 현재 로그인 토큰 제거
        authenticationService.deleteToken(userToken);

        // 해당 계정의 refresh token 전체 폐기
        refreshTokenService.revokeByAccountId(accountId);

        log.info("회원 탈퇴 상태 변경 완료. accountId={}, 연관 데이터는 배치 삭제 예정", accountId);
    }
}
