package com.cygnus.ipoten.inquiry.controller;

import com.cygnus.ipoten.inquiry.controller.request_form.CreateInquiryRequestForm;
import com.cygnus.ipoten.inquiry.controller.response_form.InquiryDetailResponseForm;
import com.cygnus.ipoten.inquiry.controller.response_form.InquirySummaryResponseForm;
import com.cygnus.ipoten.inquiry.service.InquiryService;
import com.cygnus.ipoten.inquiry.service.request.CreateInquiryRequest;
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
@RequestMapping("/api")
public class InquiryController {

    private final RedisCacheService redisCacheService;
    private final InquiryService inquiryService;

    @PostMapping("/inquiries")
    public ResponseEntity<Long> createInquiry(
            @Valid @RequestBody CreateInquiryRequestForm requestForm,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("문의 등록 인증 실패: accountId 확인 불가");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long inquiryId = inquiryService.createInquiry(
                CreateInquiryRequest.of(
                        accountId,
                        requestForm.type(),
                        requestForm.title(),
                        requestForm.content()
                )
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(inquiryId);
    }

    @GetMapping("/inquiries/me")
    public ResponseEntity<List<InquirySummaryResponseForm>> getMyInquiries(
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("문의 목록 조회 인증 실패: accountId 확인 불가");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<InquirySummaryResponseForm> response = inquiryService.getMyInquiries(accountId)
                .stream()
                .map(InquirySummaryResponseForm::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/inquiries/{id}")
    public ResponseEntity<InquiryDetailResponseForm> getMyInquiryDetail(
            @PathVariable Long id,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("문의 상세 조회 인증 실패: accountId 확인 불가");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            InquiryDetailResponseForm response = InquiryDetailResponseForm.from(
                    inquiryService.getMyInquiryDetail(accountId, id)
            );
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            log.warn("문의 상세 조회 접근 거부: inquiryId={}", id, e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    private Long resolveAccountId(String userToken) {
        if (userToken == null || userToken.isBlank()) {
            return null;
        }
        return redisCacheService.getValueByKey(userToken, Long.class);
    }
}