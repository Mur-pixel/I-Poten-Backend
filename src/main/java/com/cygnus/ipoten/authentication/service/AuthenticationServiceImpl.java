package com.cygnus.ipoten.authentication.service;

import com.cygnus.ipoten.redis_cache.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final RedisCacheService redisCacheService;


    @Override
    public String createUserTokenWithAccessToken(Long accountId, String accessToken) {

        try {
            log.info("토큰 저장하려고함 {}", accountId);
            String userToken = UUID.randomUUID().toString();
            redisCacheService.setKeyAndValue(accountId, accessToken);
            redisCacheService.setKeyAndValue(userToken, accountId);
            return userToken;
        }catch (Exception e) {
            throw new RuntimeException("UserToken 발행중 오류 발생 " + e.getMessage());
        }
    }

    @Override
    public String createTemporaryUserTokenWithAccessToken(String accessToken) {

        try {
            String tempToken = "Temporary_"+UUID.randomUUID().toString();
            redisCacheService.setKeyAndValue(tempToken, accessToken, Duration.ofMinutes(5));
            return tempToken;
        }catch (Exception e) {
            throw new RuntimeException("TemporaryUserToken 발행중 오류 발생 " +  e.getMessage());
        }


    }



    @Override
    public boolean deleteToken(String userToken) {
        try {
            log.info("유저토큰 삭제 시도");
            String accountId = redisCacheService.getValueByKey(userToken, String.class);
            redisCacheService.deleteByKey(accountId);
            redisCacheService.deleteByKey(userToken);
            return true;
        }catch (Exception e) {
            log.info("유저토큰 삭제시 문제 발생");
            return false;

        }

    }



    @Override
    public boolean logout(String userToken) {
        try {
            log.info("로그아웃 시도");
            boolean deleteTokenResult = deleteToken(userToken);
            if (deleteTokenResult) {
                return true;
            }else {
                return false;
            }
        }catch (Exception e) {
            log.info("로그아웃 문제 발생");
            return false;
        }

    }

    @Override
    public boolean verification(String currentUserToken) {
        String accessToken = redisCacheService.getValueByKey(currentUserToken, String.class);
        return accessToken != null;
    }

    @Override
    public Long getAccountIdByUserToken(String userToken) {
        return redisCacheService.getValueByKey(userToken, Long.class);
    }


}
