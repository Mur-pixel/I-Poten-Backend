package com.cygnus.ipoten.wordbook.controller.response_form;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cygnus.ipoten.wordbook.service.response.ListWordbookTermResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@RequiredArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListWordbookTermResponseForm {

    private final List<Map<String, Object>> userWordbookTermList;
    private final Long totalItems;
    private final Integer totalPages;

    private final Integer page;
    private final Integer size;
    private final String sort;

    @JsonProperty("items")
    public List<Map<String, Object>> getItemsAlias() {
        return userWordbookTermList;
    }

    @JsonProperty("total")
    public Long getTotalAlias() {
        return totalItems;
    }

    public static ListWordbookTermResponseForm from(
            final ListWordbookTermResponse response,
            int page, int size, String sort
    ) {
        List<Map<String, Object>> combined = response.transformResponseForm();
        return new ListWordbookTermResponseForm(
                combined,
                response.getTotalItems(),
                response.getTotalPages(),
                page,
                size,
                sort
        );
    }

    public static ListWordbookTermResponseForm from(final ListWordbookTermResponse response) {
        List<Map<String, Object>> combinedUserWordbookTermList = response.transformResponseForm();
        return new ListWordbookTermResponseForm(
                combinedUserWordbookTermList,
                response.getTotalItems(),
                response.getTotalPages(),
                null, null, null
        );
    }

    public static ListWordbookTermResponseForm fromFolderTermRows(
            final List<?> rows,
            final long total,
            final int page,
            final int size,
            final String sort
    ) {
        final int safeSize = Math.max(1, size);
        final int totalPages = (int) Math.ceil((double) total / safeSize);

        if (rows == null || rows.isEmpty()) {
            return new ListWordbookTermResponseForm(
                    List.of(), total, totalPages, page, safeSize, sort
            );
        }

        final ObjectMapper om = new ObjectMapper();
        final List<Map<String, Object>> items = rows.stream()
                .map(row -> {
                    if (row instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) row;
                        return map;
                    }
                    return om.convertValue(row, new TypeReference<Map<String, Object>>() {});
                })
                .toList();

        return new ListWordbookTermResponseForm(items, total, totalPages, page, safeSize, sort);
    }
}
