package com.cygnus.ipoten.survey.controller;

import com.cygnus.ipoten.redis_cache.RedisCacheService;
import com.cygnus.ipoten.survey.service.response.GetActiveSurveyResponse;
import com.cygnus.ipoten.survey.service.SurveyQueryService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class SurveyController {

    private final RedisCacheService redisCacheService;
    private final SurveyQueryService surveyQueryService;

    @Operation(
            summary = "현재 진행 중인 i-Poten 후기 설문 조회",
            description = "인증된 사용자가 현재 활성화된  i-Poten 후기 설문을 조회합니다."
    )
    @GetMapping("/me/surveys/active")
    public ResponseEntity<?> getActiveSurvey(
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            GetActiveSurveyResponse response = surveyQueryService.getActiveSurvey(accountId);
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("getActiveSurvey failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Long resolveAccountId(String userToken) {

        if (userToken == null || userToken.isBlank()) {
            return null;
        }
        return redisCacheService.getValueByKey(userToken, Long.class);
    }

}
