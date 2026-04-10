package com.cygnus.iptn.term.support;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class TagTextColumnResolver {

    private final JdbcTemplate jdbc;

    // 태그 텍스트로 쓰였을 법한 컬럼 후보
    private static final List<String> TAG_COL_CANDIDATES = List.of(
            "tags_text", "tags", "hashtags", "related_keywords", "related_keywords_text"
    );

    // 별도 테이블 후보(우선순위)
    private static final List<String> TABLE_CANDIDATES = List.of(
            "term_extra", "term_meta", "term_tags_text", "term_hashtags", "tags"
    );

    @Value
    public static class Location {
        String table;     // 실제 테이블명: term 또는 별도 테이블
        String keyColumn; // term이면 id, 별도 테이블이면 term_id
        String tagColumn; // 위 후보 중 실제 존재 컬럼
    }

    private volatile Location cached;

    public Optional<Location> resolve() {
        if (cached != null) return Optional.of(cached);

        // 1) term 테이블에 후보 컬럼이 있는지부터 확인
        List<String> termCols = listColumns("term");
        for (String cand : TAG_COL_CANDIDATES) {
            if (containsIgnoreCase(termCols, cand)) {
                cached = new Location("term", "id", realName(termCols, cand));
                log.info("[tags-text] resolved at table=`term`, column=`{}`", cached.tagColumn);
                return Optional.of(cached);
            }
        }

        // 2) 별도 테이블(우선순위) 중 "term_id + 후보 컬럼"이 같이 있는 테이블 찾기
        for (String table : TABLE_CANDIDATES) {
            List<String> cols = listColumns(table);
            if (cols.isEmpty()) continue;
            String key = hasAnyIgnoreCase(cols, List.of("term_id")) ? realName(cols, "term_id") : null;
            if (key == null) continue;

            for (String cand : TAG_COL_CANDIDATES) {
                if (containsIgnoreCase(cols, cand)) {
                    cached = new Location(table, key, realName(cols, cand));
                    log.info("[tags-text] resolved at table=`{}`, key=`{}`, column=`{}`", table, key, cached.tagColumn);
                    return Optional.of(cached);
                }
            }
        }

        log.warn("[tags-text] No tag-text location found. Checked term + {}", TABLE_CANDIDATES);
        return Optional.empty();
    }

    private List<String> listColumns(String table) {
        try {
            return jdbc.query("""
                SELECT COLUMN_NAME
                  FROM INFORMATION_SCHEMA.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = ?
                """, (rs, i) -> rs.getString(1), table);
        } catch (Exception e) {
            return List.of();
        }
    }

    private static boolean containsIgnoreCase(List<String> cols, String name) {
        return cols.stream().anyMatch(c -> c.equalsIgnoreCase(name));
    }
    private static String realName(List<String> cols, String name) {
        return cols.stream().filter(c -> c.equalsIgnoreCase(name)).findFirst().orElse(name);
    }
    private static boolean hasAnyIgnoreCase(List<String> cols, List<String> names) {
        return names.stream().anyMatch(n -> containsIgnoreCase(cols, n));
    }
}
