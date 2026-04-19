package com.cygnus.ipoten.interview.controller;

import com.cygnus.ipoten.common.annotation.PublicEndpoint;
import com.cygnus.ipoten.inquiry.service.InquiryAdminAuthService;
import com.cygnus.ipoten.interview.controller.request_form.AdminInterviewHistoryRequestForm;
import com.cygnus.ipoten.interview.controller.request_form.AdminInterviewUsersRequestForm;
import com.cygnus.ipoten.interview.controller.response_form.AdminInterviewDetailResponseForm;
import com.cygnus.ipoten.interview.controller.response_form.AdminInterviewHistoryResponseForm;
import com.cygnus.ipoten.interview.controller.response_form.AdminInterviewUsersResponseForm;
import com.cygnus.ipoten.interview.service.AdminInterviewManagementService;
import com.cygnus.ipoten.redis_cache.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 어드민 - 면접 결과 조회
 *
 * <p>프론트 (vue-account-app) 경로와 1:1 매칭:</p>
 * <ul>
 *   <li>POST /administrator/management/interview/users — 면접 기록 보유 회원 리스트</li>
 *   <li>POST /administrator/management/interview/users/{userId}/history — 특정 회원의 면접 이력</li>
 *   <li>GET  /administrator/management/interview/{interviewId} — 면접 상세</li>
 * </ul>
 *
 * <p>전역 AuthenticationInterceptor 가 /api/** 만 커버하므로 본 컨트롤러는
 * 쿠키(userToken)를 직접 꺼내 admin 권한을 확인한다. {@link PublicEndpoint} 를 붙여
 * 인터셉터 대상 경로에 들어오더라도 검증을 건너뛰도록 명시.</p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/administrator/management/interview")
@PublicEndpoint
public class AdminInterviewManagementController {

    private final AdminInterviewManagementService adminInterviewManagementService;
    private final InquiryAdminAuthService adminAuthService;
    private final RedisCacheService redisCacheService;

    @PostMapping("/users")
    public ResponseEntity<AdminInterviewUsersResponseForm> getUserList(
            @RequestBody AdminInterviewUsersRequestForm requestForm,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        ResponseEntity<AdminInterviewUsersResponseForm> guard = ensureAdmin(userToken, "회원 리스트");
        if (guard != null) return guard;

        return ResponseEntity.ok(adminInterviewManagementService.getInterviewUserList(requestForm));
    }

    @PostMapping("/users/{userId}/history")
    public ResponseEntity<AdminInterviewHistoryResponseForm> getHistory(
            @PathVariable Long userId,
            @RequestBody AdminInterviewHistoryRequestForm requestForm,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        ResponseEntity<AdminInterviewHistoryResponseForm> guard = ensureAdmin(userToken, "면접 이력");
        if (guard != null) return guard;

        return ResponseEntity.ok(adminInterviewManagementService.getInterviewHistory(userId, requestForm));
    }

    @GetMapping("/{interviewId}")
    public ResponseEntity<AdminInterviewDetailResponseForm> getDetail(
            @PathVariable Long interviewId,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        ResponseEntity<AdminInterviewDetailResponseForm> guard = ensureAdmin(userToken, "면접 상세");
        if (guard != null) return guard;

        AdminInterviewDetailResponseForm response = adminInterviewManagementService.getInterviewDetail(interviewId);
        if (response == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(response);
    }

    private <T> ResponseEntity<T> ensureAdmin(String userToken, String action) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("[admin-interview] {} 인증 실패", action);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!adminAuthService.isAdmin(accountId)) {
            log.warn("[admin-interview] {} 권한 없음 accountId={}", action, accountId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return null;
    }

    private Long resolveAccountId(String userToken) {
        if (userToken == null || userToken.isBlank()) return null;
        return redisCacheService.getValueByKey(userToken, Long.class);
    }
}
