package com.cygnus.iptn.wordbook.service.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.cygnus.iptn.wordbook_term.entity.WordbookTerm;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListWordbookTermResponse {

    /**
     * ⚠️ 호환성 유지:
     * - 프론트가 d.userWordbookTermList || d.items ... 식으로 읽으므로
     *   필드명은 userWordbookTermList를 유지하면서,
     *   items 별칭도 함께 노출한다(@JsonProperty("items")).
     */
    private final List<Row> userWordbookTermList;
    private final Long totalItems;
    private final Integer totalPages;

    /** items 별칭(프론트의 대체 파서 대응) */
    @JsonProperty("items")
    public List<Row> getItems() {
        return userWordbookTermList;
    }

    /**
     * 기존 transformResponseForm 유지 + termId 포함
     * (컨트롤러가 이 메서드를 쓰고 있다면 그대로 동작)
     */
    public List<Map<String, Object>> transformResponseForm() {
        return userWordbookTermList.stream()
                .map(row -> {
                    Map<String, Object> userTermMap = new HashMap<>();
                    userTermMap.put("id", row.getUserWordbookTermId()); // UWT id
                    userTermMap.put("termId", row.getTermId());         // ★ 추가: termId
                    userTermMap.put("word", row.getTitle());
                    userTermMap.put("description", row.getDescription());
                    userTermMap.put("createdAt", row.getCreatedAt());
                    return userTermMap;
                })
                .collect(Collectors.toList());
    }

    public static ListWordbookTermResponse from(final Page<WordbookTerm> paginatedUserWordbookTerm) {
        List<Row> rows = paginatedUserWordbookTerm.getContent().stream()
                .map(uwt -> {
                    var term = uwt.getTerm(); // HQL에서 join fetch 함
                    Long termId       = (term != null ? term.getId() : null);
                    String title      = (term != null ? term.getTitle() : null);
                    String desc       = (term != null ? term.getDescription() : null);
                    Instant at  = uwt.getCreatedAt();
                    return new Row(uwt.getId(), termId, title, desc, at);
                })
                .toList();

        return new ListWordbookTermResponse(
                rows,
                paginatedUserWordbookTerm.getTotalElements(),
                paginatedUserWordbookTerm.getTotalPages()
        );
    }

    @Getter
    @AllArgsConstructor
    public static class Row {
        /** wordbook_term.id */
        private Long userWordbookTermId;

        /** term.id — 프론트의 move, memorization에서 필수 */
        private Long termId;

        /** term.title */
        private String title;

        /** term.description */
        private String description;

        /** uwt.createdAt */
        private Instant createdAt;
    }
}
