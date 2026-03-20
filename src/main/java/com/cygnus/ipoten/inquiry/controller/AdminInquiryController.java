package com.cygnus.ipoten.inquiry.controller;

import com.cygnus.ipoten.inquiry.controller.request_form.AnswerInquiryRequestForm;
import com.cygnus.ipoten.inquiry.controller.request_form.UpdateInquiryStatusRequestForm;
import com.cygnus.ipoten.inquiry.controller.response_form.AdminInquirySummaryResponseForm;
import com.cygnus.ipoten.inquiry.service.InquiryAdminAuthService;
import com.cygnus.ipoten.inquiry.service.InquiryService;
import com.cygnus.ipoten.inquiry.service.request.AnswerInquiryRequest;
import com.cygnus.ipoten.inquiry.service.request.UpdateInquiryStatusRequest;
import com.cygnus.ipoten.redis_cache.RedisCacheService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/inquiries")
public class AdminInquiryController {

    private final RedisCacheService redisCacheService;
    private final InquiryService inquiryService;
    private final InquiryAdminAuthService inquiryAdminAuthService;

    @GetMapping
    public ResponseEntity<List<AdminInquirySummaryResponseForm>> getAllInquiries(
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("관리자 문의 목록 조회 인증 실패");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!inquiryAdminAuthService.isAdmin(accountId)) {
            log.warn("관리자 문의 목록 조회 권한 없음: accountId={}", accountId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<AdminInquirySummaryResponseForm> response = inquiryService.getAllInquiriesForAdmin()
                .stream()
                .map(AdminInquirySummaryResponseForm::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateInquiryStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateInquiryStatusRequestForm requestForm,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("관리자 문의 상태 변경 인증 실패");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!inquiryAdminAuthService.isAdmin(accountId)) {
            log.warn("관리자 문의 상태 변경 권한 없음: accountId={}", accountId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        inquiryService.updateInquiryStatus(
                UpdateInquiryStatusRequest.of(id, requestForm.status())
        );
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/answer")
    public ResponseEntity<Void> answerInquiry(
            @PathVariable Long id,
            @Valid @RequestBody AnswerInquiryRequestForm requestForm,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("관리자 문의 답변 등록 인증 실패");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!inquiryAdminAuthService.isAdmin(accountId)) {
            log.warn("관리자 문의 답변 등록 권한 없음: accountId={}", accountId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        inquiryService.answerInquiry(
                AnswerInquiryRequest.of(id, requestForm.answerContent())
        );
        return ResponseEntity.ok().build();
    }

    private Long resolveAccountId(String userToken) {
        if (userToken == null || userToken.isBlank()) {
            return null;
        }
        return redisCacheService.getValueByKey(userToken, Long.class);
    }
}