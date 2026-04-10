package com.cygnus.ipoten.wordbook_pdf.controller.export.request_form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

@Data
public class TermsPdfGenerateByWordbookRequestForm {

    @NotNull(message = "wordbookId는 필수입니다.")
    private Long wordbookId;

    /** PDF 문서 제목(선택) */
    private String title;

    /** 필터(선택) */
    @Valid
    private Filters filters;

    /** 정렬(선택) : "termId, ASC", | "termId, DESC" (추후 확장 가능) */
    @Pattern(regexp = "^(?i)(termId, (ASC|DESC))$", message = "sort 형식은 termId, ASC|DESC만 지원합니다.")
    private String sort;

    @Data
    public static class Filters {
        /** 암기 상태 필터 : LEARNING | DONE(둘 중 하나) */
        @Pattern(regexp = "^(LEARNING|DONE)?$", message = "memorization은 LEARNING 또는 DONE만 허용됩니다.")
        private String memorization;
        
        /** 포함 태그(AND 매칭이 기본 - 필요시 OR 정책으로 바꿀 수 있음 */
        private List<String> includeTags;
        
        /** 제외 태그 */
        private List<String> excludeTags;
        
    }
}
