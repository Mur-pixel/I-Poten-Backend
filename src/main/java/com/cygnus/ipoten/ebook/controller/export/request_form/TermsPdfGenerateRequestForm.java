package com.cygnus.ipoten.ebook.controller.export.request_form;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class TermsPdfGenerateRequestForm {
    @NotEmpty(message = "termIds must not be empty")
    private List<Long> termIds;
    
    private String title; // 파일 / 문서 제목

}
