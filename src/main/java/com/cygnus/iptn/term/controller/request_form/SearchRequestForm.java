package com.cygnus.iptn.term.controller.request_form;

import com.cygnus.iptn.term.service.request.SearchTermRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchRequestForm {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    /** 검색어(선택) */
    @Size(max = 100)
    private String q;

    /** 상호 배타 파라미터(initial | alpha | symbol 중 0 또는 1개만 사용) */
    private String initial;     // ㄱ ㄴ ㄷ ... ㅎ (더블초성 입력 시 매핑)
    private String alpha;       // A~Z (대소문자 무관)
    private String symbol;      // ~@#$%&*/?-_+=.,!

    /** 분류 경로: "대/중/소" 형태의 카테고리 id → 예: 1/4/12 (마지막 id를 기준으로 검색) */
    private String catPath;

    /** 페이지(0-base) */
    @Min(0)
    private Integer page;

    /** 페이지 크기 */
    @Min(1) @Max(MAX_SIZE)
    private Integer size;

    /**
     * 정렬 파라미터(화이트리스트 파싱)
     * 허용: relevance,desc | title,asc|desc | updatedAt,desc|asc
     * 기본: relevance,desc
     */
    private String sort;

    /** 태그 포함 검색 여부(선택) */
    private Boolean includeTags;

    public SearchTermRequest toRequest() {
        // ===== 1) 기본값/정규화 =====
        final String qTrim = normBlankToNull(q);

        // 더블초성 매핑 + 허용 초성만
        String ini = normBlankToNull(initial);
        if (ini != null) {
            ini = switch (ini.trim()) {
                case "ㄲ" -> "ㄱ";
                case "ㄸ" -> "ㄷ";
                case "ㅃ" -> "ㅂ";
                case "ㅆ" -> "ㅅ";
                case "ㅉ" -> "ㅈ";
                default -> ini.trim();
            };
            if (!ALLOWED_INITIAL.contains(ini)) ini = null;
        }

        // 알파벳 : 1글자 Upper, A~Z만 허용
        String alp = normBlankToNull(alpha);
        if (alp != null) {
            alp = alp.substring(0, 1).toUpperCase();
            if (alp.charAt(0) < 'A' || alp.charAt(0) > 'Z') alp = null;
        }

        // 기호 : 1글자, 허용 목록만
        String sym = normBlankToNull(symbol);
        if (sym != null) {
            sym = sym.substring(0, 1);
            if (ALLOWED_SYMBOLS.indexOf(sym.charAt(0)) < 0) sym = null;
        }

        final int prefixCnt = (ini != null ? 1 : 0) + (alp != null ? 1 : 0) + (sym != null ? 1 : 0);
        if (prefixCnt > 1) {
            throw new IllegalArgumentException("initial | alpha | symbol은 상호 배타입니다. 하나만 지정하세요.");
        }

        final int p = (page == null || page < 0) ? DEFAULT_PAGE : page;
        final int s = (size == null) ? DEFAULT_SIZE : Math.min(Math.max(size, 1), MAX_SIZE);
        final boolean withTags = Boolean.TRUE.equals(includeTags);

        // ===== 2) 화이트리스트 정렬 파싱 =====
        final String sortParam = (sort == null || sort.isBlank()) ? "relevance,desc" : sort.trim();
        String[] parts = sortParam.split("\\s*,\\s*");
        final String key = parts.length > 0 ? parts[0].toLowerCase() : "relevance";
        final String dir = parts.length > 1 ? parts[1].toLowerCase() : "desc";

        final SearchTermRequest.SortKey sortKey = switch (key) {
            case "relevance" -> SearchTermRequest.SortKey.RELEVANCE;
            case "title"     -> SearchTermRequest.SortKey.TITLE;
            case "updatedat" -> SearchTermRequest.SortKey.UPDATED_AT;
            default          -> throw new IllegalArgumentException("Invalid sort key: " + key);
        };

        final Sort.Direction direction = switch (dir) {
            case "asc" -> Sort.Direction.ASC;
            case "desc" -> Sort.Direction.DESC;
            default -> throw new IllegalStateException("Invalid sort direction: " + dir);
        };

        // ===== 3) catPath 파싱 → 선택된 경로/마지막 id 전달 =====
        final List<Long> pathIds = parseCatPath(normBlankToNull(catPath));
        final Long selectedCategoryId = pathIds.isEmpty() ? null : pathIds.get(pathIds.size() - 1);

        // ===== 4) 서비스 DTO로 변환 =====
        return SearchTermRequest.builder()
                .q(qTrim)
                .initial(ini)
                .alpha(alp)
                .symbol(sym)
                .page(p)
                .size(s)
                .sortKey(sortKey)
                .direction(direction)
                .includeTags(withTags)
                .catPathIds(pathIds)
                .selectedCategoryId(selectedCategoryId)
                .build();
    }

    // ===== 내부 유틸/상수 =====
    private static String normBlankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static List<Long> parseCatPath(String catPath) {
        List<Long> out = new ArrayList<>();
        if (catPath == null) return out;
        for (String tok : catPath.split("/")) {
            try {
                if (!tok.isBlank()) out.add(Long.parseLong(tok.trim()));
            } catch (NumberFormatException ignore) {}
        }
        return out;
    }

    private static final java.util.Set<String> ALLOWED_INITIAL = java.util.Set.of(
            "ㄱ", "ㄴ", "ㄷ", "ㄹ", "ㅁ", "ㅂ", "ㅅ", "ㅇ", "ㅈ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"
    );
    private static final String ALLOWED_SYMBOLS = "~@#$%&*/?-_+=.,!";
}
