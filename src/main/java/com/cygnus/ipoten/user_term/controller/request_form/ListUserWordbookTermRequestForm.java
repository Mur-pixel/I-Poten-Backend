package com.cygnus.ipoten.user_term.controller.request_form;

import com.cygnus.ipoten.user_term.service.request.ListUserWordbookTermRequest;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class ListUserWordbookTermRequestForm {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final String DEFAULT_SORT = "createdAt, desc";

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "updatedAt", "termName", "status");

    @Min(0)
    private Integer page = DEFAULT_PAGE;

    // size 우선, 없으면 perPage 사용
    @Min(1)
    private Integer size = DEFAULT_SIZE;

    @Min(1)
    private Integer perPage;

    // "field,asc|desc" 또는 "field" 허용
    @Pattern(regexp = "^[A-Za-z0-9_]+\\s*,\\s*(?i)(asc|desc)$|^[A-Za-z0-9_]+$", message = "sort는 'field,asc|desc' 형식이어야 합니다.")
    private String sort = DEFAULT_SORT;

    private int resolvePage() {
        int p = (page == null || page < 0) ? DEFAULT_PAGE : page;
        return Math.max(0, p);
    }

    private int resolvePageSize() {
        Integer base = (size != null && size >= 1) ? size
                : (perPage != null && perPage >= 1) ? perPage : DEFAULT_SIZE;
        return Math.min(base, MAX_SIZE);
    }

    private String sanitizeSort(String raw) {
        String s = (raw == null || raw.isBlank()) ? DEFAULT_SORT : raw.trim();

        // 공백 제거
        s = s.replaceAll("\\s+", "");
        String field;
        String dir;

        if (s.contains(",")) {
            String[] parts = s.split(",", 2);
            field = parts[0];
            dir = parts[1].toLowerCase();
        } else {
            field = s;
            dir = "desc";
        }

        if (!ALLOWED_SORT_FIELDS.contains(field)) {
            return DEFAULT_SORT;
        }
        if (!dir.equals("asc") && !dir.equals("desc")) {
            dir = "desc";
        }
        return field + "," + dir;
    }

    public int pageOrDefault() {
        return resolvePage();
    }
    public int perPageOrDefault() {
        return resolvePageSize();
    }

    public String sortOrDefault() {
        return sanitizeSort(sort);
    }

    public ListUserWordbookTermRequest toListUserTermRequest(Long accountId, Long folderId) {
        return new ListUserWordbookTermRequest(accountId, folderId, resolvePage(), resolvePageSize(), sanitizeSort(sort));
    }
}
