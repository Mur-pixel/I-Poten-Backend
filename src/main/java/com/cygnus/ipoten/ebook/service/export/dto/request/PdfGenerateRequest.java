package com.cygnus.ipoten.ebook.service.export.dto.request;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class PdfGenerateRequest {
    Long accountId;     // 인증 주체
    List<Long> termIds; // 요청 ID들
    String title;       // 문서 제목
}
