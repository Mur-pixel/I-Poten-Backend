package com.cygnus.iptn.wordbook_pdf.controller.export.response_form;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TermsPdfGenerateResponseForm {
    Long ebookId;    // 저장 안 하면 null
    String filename; // Content-Disposition에 사용
    Integer count;   // 포함 용어 수
}
