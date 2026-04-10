package com.cygnus.iptn.term.controller.response_form;

import com.cygnus.iptn.term.service.response.SearchTermResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 검색 결과 항목(요약) 응답 폼
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TermSummaryForm {
    private Long id;
    private String title;
    private String description;

    public static TermSummaryForm from(SearchTermResponse.TermSummary dto) {
        return TermSummaryForm.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .build();
    }
}
