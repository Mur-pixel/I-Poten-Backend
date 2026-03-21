package com.cygnus.ipoten.recommendation.controller;

import com.cygnus.ipoten.common.annotation.LoginUser;
import com.cygnus.ipoten.recommendation.service.JobRecommendedTermService;
import com.cygnus.ipoten.recommendation.controller.request_form.attachJobRecommendationsToWordbook;
import com.cygnus.ipoten.wordbook.controller.response_form.AttachTermsBulkResponseForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Recommendation", description = "추천 단어를 사용자 단어장에 추가하는 API")
public class JobRecommendedTermController {

    private final JobRecommendedTermService jobRecommendedTermService;

    @Operation(summary = "직무별 추천 단어를 단어장에 일괄 저장")
    @PostMapping("/me/folders/{wordbookId}/recommended-terms/by-job")
    public ResponseEntity<AttachTermsBulkResponseForm> attachJobRoleRecommendationsToFolder(
            @LoginUser Long accountId,
            @PathVariable Long wordbookId,
            @RequestBody @Valid attachJobRecommendationsToWordbook requestForm) {

        try {
            log.info("[attachJobRecommendations] wordbookId={}, jobKey={}",
                    wordbookId, requestForm.getJobKey());

            var response = jobRecommendedTermService.attachJobRecommendationsToWordbook(
                    accountId, wordbookId, requestForm.getJobKey());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(AttachTermsBulkResponseForm.from(response));
        } catch (Exception e) {
            log.error("[attachJobRecommendations] FAILED wordbookId={} jobKey={}",
                    wordbookId, requestForm.getJobKey(), e);
            throw e;
        }
    }
}
