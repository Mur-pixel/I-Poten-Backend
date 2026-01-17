package com.cygnus.ipoten.term.service.request;

import lombok.*;
import org.springframework.data.domain.Sort;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class SearchTermRequest {
    public enum SortKey { RELEVANCE, TITLE, UPDATED_AT }

    // 검색어 (없을 수 있음 : prefix 모드일 땐 무시 가능)
    private String q;

    // 상호 배타 prefix 파라미터 (하나만 세팅됨)
    private String initial;   // ㄱ~ㅎ (더블초성은 폼에서 ㄱ/ㄷ/ㅂ/ㅅ/ㅈ 로 매핑됨)
    private String alpha;     // A~Z (대소문자 무관)
    private String symbol;    // ~@#$%&*/?-_+=.,!

    private int page;
    private int size;
    private SortKey sortKey;
    private Sort.Direction direction;
    private boolean includeTags;
    private List<Long> catPathIds;      // 프론트에서 전달한 경로 id들(대/중/소)
    private Long selectedCategoryId;    // 사용자가 최종 선택한 카테고리 id(경로의 마지막 요소)

    private String actorKey;

    /** 편의 : prefix 모드 여부 */
    public boolean isPrefixMode() {
        int c = 0;
        if (initial != null) c++;
        if (alpha != null) c++;
        if (symbol != null) c++;
        return c == 1;
    }
}
