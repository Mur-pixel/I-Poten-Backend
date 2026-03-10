package com.cygnus.ipoten.survey.controller;

import com.cygnus.ipoten.redis_cache.RedisCacheService;
import com.cygnus.ipoten.survey.controller.request_form.SubmitSurveyResponseRequestForm;
import com.cygnus.ipoten.survey.controller.response_form.SubmitSurveyResponseForm;
import com.cygnus.ipoten.survey.service.SurveySubmitService;
import com.cygnus.ipoten.survey.service.response.GetActiveSurveyResponse;
import com.cygnus.ipoten.survey.service.SurveyQueryService;
import com.cygnus.ipoten.survey.service.response.SubmitSurveyResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class SurveyController {

    private final RedisCacheService redisCacheService;
    private final SurveyQueryService surveyQueryService;
    private final SurveySubmitService surveySubmitService;

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

    @Operation(
            summary = "현재 진행 중인 i-Poten 후기 설문 응답 제출",
            description = "인증된 사용자가 현재 활성화된 i-Poten 후기 설문에 응답을 제출합니다."
    )
    @PostMapping("/me/surveys/active/responses")
    public ResponseEntity<?> submitActiveSurvey(
            @Valid @RequestBody SubmitSurveyResponseRequestForm requestForm,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            SubmitSurveyResponse response = surveySubmitService.submitActiveSurvey(accountId, requestForm.toServiceRequest());
            return ResponseEntity.status(HttpStatus.CREATED).body(SubmitSurveyResponseForm.from(response));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("submitActiveSurvey failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }
}
