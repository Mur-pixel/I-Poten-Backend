package com.cygnus.ipoten.administer.service;

import com.cygnus.ipoten.account.entity.*;
import com.cygnus.ipoten.account.repository.AccountLoginTypeRepository;
import com.cygnus.ipoten.account.repository.AccountRepository;
import com.cygnus.ipoten.account.repository.AccountRoleTypeRepository;
import com.cygnus.ipoten.account.service.AccountService;
import com.cygnus.ipoten.accountProfile.controller.request.RegisterAccountProfileRequest;
import com.cygnus.ipoten.accountProfile.repository.AccountProfileRepository;
import com.cygnus.ipoten.accountProfile.service.AccountProfileService;

import com.cygnus.ipoten.administer.service.dto.VerificationInitialAdminDto;
import com.cygnus.ipoten.redis_cache.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdministratorServiceImpl implements AdministratorService {

    private final AccountProfileService accountProfileService;
    private final AccountProfileRepository accountProfileRepository;
    private final AccountRoleTypeRepository accountRoleTypeRepository;
    private final AccountLoginTypeRepository accountLoginTypeRepository;
    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final RedisCacheService redisCacheService;
    private final RoleType adminRoleType =RoleType.ADMIN;


    @Value("${admin.secret-id-key}")
    private String secretIdKey;
    @Value("${admin.secret-password-key}")
    private String secretPasswordKey;


    @Override
    public boolean validateKey(String id, String password) {
        log.info("validateKey is working");
        return secretIdKey.equals(id) && secretPasswordKey.equals(password);
    }

    @Transactional
    @Override
    public void createAdminIfNotExists(String adminEmail, String adminNickname, LoginType adminLoginType) {
        log.info("[AdministratorServiceImpl] createAdminIfNotExists called. email={}, nickname={}, loginType={}",
                adminEmail, adminNickname, adminLoginType);
        // 1.단일 조회
        Optional<VerificationInitialAdminDto> adminInfoOpt =getInitialAdminInfo(adminEmail);
        if(adminInfoOpt.isEmpty()){
            // 2.조회 안될시 생성
            createInitialAdmin(adminEmail,adminNickname,adminLoginType);
            return;
        }
        // 3. 존재할시 정합 여부 판단
        var adminInfo = adminInfoOpt.get();
        log.info("[AdministratorServiceImpl] FOUND accountId={}, currentLoginType={}, currentRoleType={}",
                adminInfo.getAdminAccountId(), adminInfo.getAdminLoginType(), adminInfo.getAdminRoleType());

        boolean adminLoginMatch = adminInfo.getAdminLoginType() == adminLoginType;
        boolean adminRoleMatch = adminInfo.getAdminRoleType() == adminRoleType;

        if(adminLoginMatch && adminRoleMatch){
            // 3-1 .env 세팅과 매칭되면 스킵
            log.info("[AdministratorServiceImpl] InitialAdmin Login Setting Match");
            return;
        }
        alignInitialAdminByUpdate(adminInfo.getAdminAccountId(),adminLoginType,adminRoleType);
        log.info("[AdministratorServiceImpl] InitialAdmin aligned by UPDATE.");
//        alignInitialAdminByUpdate(adminInfo.getAdminAccountId(),adminInfo.getAdminLoginType(),adminInfo.getAdminRoleType());
//        log.info("[AdministratorServiceImpl] Admin Login has been Updated. loginType = {} , RoleType = {}", adminLoginType, adminRoleType);

    }

    private void createInitialAdmin(String adminEmail, String adminNickname, LoginType adminLoginType) {
        try {
            log.info("[AdministratorService] Creating admin account. email={}, nickname={}, loginType={}",
                    adminEmail, adminNickname, adminLoginType);

            AccountRoleType adminRole = accountRoleTypeRepository.findByRoleType(RoleType.ADMIN)
                    .orElseThrow(() -> new IllegalStateException("RoleType.ADMIN not initialized"));

            Account account = accountService.createAccountWithRoleType(adminRole, adminLoginType)
                    .orElseThrow(() -> new IllegalStateException("Account 생성 실패"));

            RegisterAccountProfileRequest profileReq = new RegisterAccountProfileRequest(adminNickname, adminEmail);
            accountProfileService.createAccountProfile(account, profileReq)
                    .orElseThrow(() -> new IllegalStateException("AccountProfile 생성 실패"));

            log.info("[AdministratorService] Admin created successfully. email={}", adminEmail);

        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // 동시성 상황에서 UNIQUE(email) 충돌 시 재조회로 수습 → idempotent
            log.warn("[AdministratorService] race detected on admin creation. fallback to lookup. email={}", adminEmail);
            var info = getInitialAdminInfo(adminEmail).orElseThrow(() -> e);
            alignInitialAdminByUpdate(info.getAdminAccountId(), adminLoginType, RoleType.ADMIN);
        }
    }

    @Override
    public Optional<VerificationInitialAdminDto> getInitialAdminInfo(String adminEmail) {
        return accountProfileService.loadProfileByEmail(adminEmail)
                .map(profile -> {
                    Account account = profile.getAccount();
                    return new VerificationInitialAdminDto(
                            account.getId(),
                            account.getAccountLoginType().getLoginType(),
                            account.getAccountRoleType().getRoleType()
                    );
                });
    }
//    public void alignInitialAdminByDelete(Long accountId,LoginType adminLoginType, RoleType adminRoleType) {
//        Account account = accountRepository.findById(accountId)
//                .orElseThrow(() -> new IllegalStateException("Account vanished during alignment : " + accountId));
//        AccountLoginType login = accountLoginTypeRepository.findByLoginType(adminLoginType)
//                .orElseThrow(() -> new IllegalStateException("LoginType.ADMIN not initialized"));
//        AccountRoleType role=accountRoleTypeRepository.findByRoleType(adminRoleType)
//                .orElseThrow(() -> new IllegalStateException("RoleType.ADMIN not initialized"));
//
//        accountRepository.deleteById(accountId);
//        accountProfileRepository.deleteBy();
//
//    }
    @Transactional
    public void alignInitialAdminByUpdate(Long accountId,LoginType expectedAdminLoginType, RoleType expectedAdminRoleType) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalStateException("Account vanished during alignment : " + accountId));
        AccountLoginType loginType = accountLoginTypeRepository.findByLoginType(expectedAdminLoginType)
                .orElseThrow(() -> new IllegalStateException("LoginType.ADMIN not initialized"));
        AccountRoleType roleType = accountRoleTypeRepository.findByRoleType(expectedAdminRoleType)
                .orElseThrow(() -> new IllegalStateException("RoleType.ADMIN not initialized"));

        // 로그인 타입 교정
        if (account.getAccountLoginType() == null ||
                account.getAccountLoginType().getLoginType() != expectedAdminLoginType) {
            account.changeLoginType(loginType);
        }
        //관리자 권한 역할 교정
        if (account.getAccountRoleType() == null ||
                account.getAccountRoleType().getRoleType() != expectedAdminRoleType) {
            // expectedRoleType이 ADMIN이면 grantAdmin, 아니면 일반 변경
            if (expectedAdminRoleType == RoleType.ADMIN) {
                account.grantAdmin(roleType);
            }
        }
        log.info("[AdministratorService] Alignment updated successfully. accountId={} loginType={} RoleType={}",
                accountId,
                account.getAccountLoginType() !=null ?account.getAccountLoginType().getLoginType() : null,
                account.getAccountRoleType() !=null ? account.getAccountRoleType().getRoleType() : null);

    }
    @Override
    public boolean isAdminByUserToken(String userToken) {
        boolean isBlank = (userToken == null) || userToken.isBlank();
        boolean isNullLiteral = !isBlank && (
                "null".equalsIgnoreCase(userToken) || "undefined".equalsIgnoreCase(userToken)
        );

        log.warn("[isAdminByUserToken] arg='{}' (isNull={}, isBlank={}, isNullLiteral={})",
                userToken, userToken == null, !isBlank && userToken.isBlank(), isNullLiteral);

        Long accountId;
        try {
            accountId = redisCacheService.getValueByKey(userToken, Long.class);
            log.info("[AdministratorServiceImpl] AccountId={}", accountId);
        } catch (Exception e) {
            log.warn("[isAdminByUserToken] Redis read failed: {}", e.getMessage());
            return false;
        }
        return accountRepository.findById(accountId)
                .map(a -> a.getAccountRoleType() != null
                        && a.getAccountRoleType().getRoleType() == RoleType.ADMIN)
                .orElse(false);
    }

    @Override
    public String createTemporaryAdminToken() {
        try{
            final String createdTemporaryAdminToken= "temporaryAdminToken";
            final String tempToken= UUID.randomUUID().toString();

            redisCacheService.setKeyAndValue(tempToken,createdTemporaryAdminToken, Duration.ofMinutes(5));
            return tempToken;
        } catch (Exception e) {
            throw new RuntimeException("TemporaryAdminToken 발행중 오류 발생 " +  e.getMessage());
        }
    }

    @Override
    public boolean isTempTokenValid(String tempToken) {
        String isValid=redisCacheService.getValueByKey(tempToken, String.class);
        if(isValid==null) {
            log.info("[AdministratorService] TempToken is null or empty");
            return false;
        }
        log.info("[AdministratorService] TempToken is valid");
        return true;
    }
}
