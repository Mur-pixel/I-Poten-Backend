package com.cygnus.iptn.inquiry.service;

import com.cygnus.iptn.account.entity.Account;
import com.cygnus.iptn.account.repository.AccountRepository;
import com.cygnus.iptn.inquiry.entity.Inquiry;
import com.cygnus.iptn.inquiry.repository.InquiryRepository;
import com.cygnus.iptn.inquiry.service.request.AnswerInquiryRequest;
import com.cygnus.iptn.inquiry.service.request.CreateInquiryRequest;
import com.cygnus.iptn.inquiry.service.request.UpdateInquiryStatusRequest;
import com.cygnus.iptn.inquiry.service.response.AdminInquirySummaryResponse;
import com.cygnus.iptn.inquiry.service.response.InquiryDetailResponse;
import com.cygnus.iptn.inquiry.service.response.InquirySummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryServiceImpl implements InquiryService {

    private final InquiryRepository inquiryRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public Long createInquiry(CreateInquiryRequest request) {
        validateCreateRequest(request);

        Account account = accountRepository.findById(request.accountId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계정입니다."));

        Inquiry inquiry = Inquiry.create(
                account,
                request.type(),
                request.title(),
                request.content()
        );

        return inquiryRepository.save(inquiry).getId();
    }

    @Override
    public List<InquirySummaryResponse> getMyInquiries(Long accountId) {
        return inquiryRepository.findByAccount_Id(
                        accountId,
                        Sort.by(Sort.Direction.DESC, "createdAt")
                ).stream()
                .map(InquirySummaryResponse::from)
                .toList();
    }

    @Override
    public InquiryDetailResponse getMyInquiryDetail(Long accountId, Long inquiryId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문의입니다."));

        if (!inquiry.isOwner(accountId)) {
            throw new SecurityException("본인의 문의만 조회할 수 있습니다.");
        }
        return InquiryDetailResponse.from(inquiry);
    }

    @Override
    public List<AdminInquirySummaryResponse> getAllInquiriesForAdmin() {
        return inquiryRepository.findAll(
                        Sort.by(Sort.Direction.DESC, "createdAt")
                ).stream()
                .map(AdminInquirySummaryResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void updateInquiryStatus(UpdateInquiryStatusRequest request) {
        Inquiry inquiry = inquiryRepository.findById(request.inquiryId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문의입니다."));

        inquiry.updateStatus(request.status());
    }

    @Override
    @Transactional
    public void answerInquiry(AnswerInquiryRequest request) {
        Inquiry inquiry = inquiryRepository.findById(request.inquiryId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문의입니다."));

        inquiry.answer(request.answerContent());
    }

    private void validateCreateRequest(CreateInquiryRequest request) {
        if (request.accountId() == null) {
            throw new IllegalArgumentException("계정 정보가 없습니다.");
        }
        if (request.type() == null) {
            throw new IllegalArgumentException("문의 유형은 필수입니다.");
        }
        if (isBlank(request.title())) {
            throw new IllegalArgumentException("제목은 필수입니다.");
        }
        if (isBlank(request.content())) {
            throw new IllegalArgumentException("문의 내용은 필수입니다.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}