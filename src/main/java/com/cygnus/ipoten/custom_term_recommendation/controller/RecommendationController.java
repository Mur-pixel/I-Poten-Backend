package com.cygnus.ipoten.custom_term_recommendation.controller;

import com.cygnus.ipoten.custom_term_recommendation.service.JobGroupWordbookService;
import com.cygnus.ipoten.custom_term_recommendation.controller.request_form.AttachJobGroupRequestForm;
import com.cygnus.ipoten.wordbook.controller.response_form.AttachTermsBulkResponseForm;
import com.cygnus.ipoten.redis_cache.RedisCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Recommendation", description = "추천 단어를 사용자 단어장에 추가하는 API")
public class RecommendationController {

    private final RedisCacheService redisCacheService;
    private final JobGroupWordbookService jobGroupWordbookService;

    @Operation(
            summary = "직무별 추천 단어를 단어장에 일괄 저장",
            description = "jobKey(예: FRONTEND, BACKEND)에 해당하는 추천 단어들을 지정한 폴더에 한 번에 담습니다."
    )
    @PostMapping("/me/folders/{folderId}/terms:bulk-by-job")
    public ResponseEntity<AttachTermsBulkResponseForm> attachJobGroupToFolder(
            @CookieValue(name = "userToken", required = false) String userToken,
            @PathVariable Long folderId,
            @RequestBody @Valid AttachJobGroupRequestForm requestForm
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "로그인이 필요합니다.");
        }
        var response = jobGroupWordbookService.attachJobGroupToFolder(accountId, folderId, requestForm.getJobKey());
        return ResponseEntity.status(HttpStatus.CREATED).body(AttachTermsBulkResponseForm.from(response));
    }

    /**
     * 공통: userToken 쿠키에서 계정 ID를 조회한다.
     * - 토큰이 없거나 공백이면 null
     * - Redis에 없거나 TTL 만료된 경우도 null
     * → null이면 컨트롤러에서 UNAUTHORIZED 처리
     */
    private Long resolveAccountId(String userToken) {
        if (userToken == null || userToken.isBlank()) return null;
        return redisCacheService.getValueByKey(userToken, Long.class);
    }
}
