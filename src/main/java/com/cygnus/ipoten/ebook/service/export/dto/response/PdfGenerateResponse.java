package com.cygnus.ipoten.ebook.service.export.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PdfGenerateResponse {
    Long ebookId;       // 저장하지 않으면 null
    String filename;    // 예: jobspoon_terms_2025-08-28.pdf
    int count;          // 용어 수
}
