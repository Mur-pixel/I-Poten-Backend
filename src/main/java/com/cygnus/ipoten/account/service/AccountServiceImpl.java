package com.cygnus.ipoten.account.service;

import com.cygnus.ipoten.account.entity.*;
import com.cygnus.ipoten.account.exception.NotLoggedInException;
import com.cygnus.ipoten.account.exception.UserNotFoundException;
import com.cygnus.ipoten.account.repository.AccountLoginTypeRepository;
import com.cygnus.ipoten.account.repository.AccountRepository;
import com.cygnus.ipoten.account.repository.AccountRoleTypeRepository;
import com.cygnus.ipoten.account.service.register_request.RegisterAccountRequest;
import com.cygnus.ipoten.accountProfile.repository.AccountProfileRepository;
import com.cygnus.ipoten.accountProfile.service.AccountProfileService;
import com.cygnus.ipoten.authentication.service.AuthenticationService;
import com.cygnus.ipoten.profileAppearance.Service.ProfileAppearanceService;
import com.cygnus.ipoten.redis_cache.RedisCacheService;
import com.cygnus.ipoten.userLevel.service.UserLevelService;
import com.cygnus.ipoten.userTitle.service.UserTitleService;
import com.cygnus.ipoten.userTrustscore.service.TrustScoreService;
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
    private final ProfileAppearanceService profileAppearanceService;
    private final AccountProfileRepository accountProfileRepository;

    private final AccountProfileService accountProfileService;
    private final TrustScoreService trustScoreService;
    private final UserLevelService userLevelService;
    private final UserTitleService userTitleService;


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

        // 2️⃣ 연관 데이터 초기화
        profileAppearanceService.create(accountId);   // 프로필 외형 row 생성
        trustScoreService.initTrustScore(accountId);  // 신뢰점수 0 생성
        userTitleService.initTitle(accountId);        // 기본 칭호 “초심자” 생성 및 장착

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
    public void withdraw(String userToken) {
        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);

        // 로그인 상태가 아닐 때
        if (accountId == null) {
            throw new NotLoggedInException("회원이 로그인 상태가 아닙니다.");
        }

        authenticationService.deleteToken(userToken);

        profileAppearanceService.delete(accountId);

        // 계정을 찾고 삭제
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new UserNotFoundException("해당하는 계정을 찾을 수 없습니다."));

        // 먼저 accountProfile 삭제
        accountProfileRepository.findByAccountId(accountId)
                .ifPresent(accountProfileRepository::delete);

        // 그 다음 account 삭제
        accountRepository.delete(account);

    }






}
