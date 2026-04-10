package com.cygnus.iptn.batch.recommendation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobRecommendedTermImportRow {

    /** FRONTEND / BACKEND ... */
    private String jobKey;

    /** 1~100 */
    private int rankNo;

    /** term_category_id */
    private Long categoryId;

    /** term.title 매칭 키 */
    private String termTitle;

    /** 비고 (DB 저장은 안 하고 로깅/디버깅용) */
    private String memo;

    /** 파일 라인 번호(에러 로그용) */
    private int lineNo;

    public void validate() {
        if (jobKey == null || jobKey.trim().isEmpty())
            throw new IllegalArgumentException("jobKey는 필수입니다. (line=" + lineNo + ")");
        if (categoryId == null) throw new IllegalArgumentException("categoryId는 필수입니다. (line=" + lineNo + ")");
        if (termTitle == null || termTitle.trim().isEmpty())
            throw new IllegalArgumentException("termTitle은 필수입니다. (line=" + lineNo + ")");
        if (rankNo <= 0 || rankNo > 100)
            throw new IllegalArgumentException("rankNo는 1~100이어야 합니다. rankNo=" + rankNo + " (line=" + lineNo + ")");
    }
}
