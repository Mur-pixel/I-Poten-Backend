package com.cygnus.iptn.inquiry.service;

import com.cygnus.iptn.inquiry.service.request.AnswerInquiryRequest;
import com.cygnus.iptn.inquiry.service.request.CreateInquiryRequest;
import com.cygnus.iptn.inquiry.service.request.UpdateInquiryStatusRequest;
import com.cygnus.iptn.inquiry.service.response.AdminInquirySummaryResponse;
import com.cygnus.iptn.inquiry.service.response.InquiryDetailResponse;
import com.cygnus.iptn.inquiry.service.response.InquirySummaryResponse;

import java.util.List;

public interface InquiryService {

    Long createInquiry(CreateInquiryRequest request);

    List<InquirySummaryResponse> getMyInquiries(Long accountId);

    InquiryDetailResponse getMyInquiryDetail(Long accountId, Long inquiryId);

    List<AdminInquirySummaryResponse> getAllInquiriesForAdmin();

    void updateInquiryStatus(UpdateInquiryStatusRequest request);

    void answerInquiry(AnswerInquiryRequest request);
}