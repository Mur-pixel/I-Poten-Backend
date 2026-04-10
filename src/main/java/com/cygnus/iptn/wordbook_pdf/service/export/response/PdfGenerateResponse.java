package com.cygnus.iptn.wordbook_pdf.service.export.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PdfGenerateResponse {
    Long wordbookPdfId; // 저장하지 않으면 null
    String filename;    // 예: I-Ptn_terms_2025-08-28.pdf
    int count;          // 용어 수
}
