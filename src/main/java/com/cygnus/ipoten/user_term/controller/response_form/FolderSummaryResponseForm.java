package com.cygnus.ipoten.user_term.controller.response_form;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FolderSummaryResponseForm {
    private Long id;
    private String name;
    private Long termCount;       // 폴더 내 즐겨찾기 용어 수
    private Long learnedCount;          // null 가능 → 프론트에서 0 처리해도 됨
    private LocalDateTime updatedAt;
    private LocalDateTime lastStudiedAt;
}
