package com.cygnus.ipoten.user_term.controller.response_form;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cygnus.ipoten.user_term.service.response.ListUserWordbookTermResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@RequiredArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListUserWordbookTermResponseForm {

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

    public static ListUserWordbookTermResponseForm from(
            final ListUserWordbookTermResponse response,
            int page, int size, String sort
    ) {
        List<Map<String, Object>> combined = response.transformResponseForm();
        return new ListUserWordbookTermResponseForm(
                combined,
                response.getTotalItems(),
                response.getTotalPages(),
                page,
                size,
                sort
        );
    }

    public static ListUserWordbookTermResponseForm from(final ListUserWordbookTermResponse response) {
        List<Map<String, Object>> combinedUserWordbookTermList = response.transformResponseForm();
        return new ListUserWordbookTermResponseForm(
                combinedUserWordbookTermList,
                response.getTotalItems(),
                response.getTotalPages(),
                null, null, null
        );
    }

    public static ListUserWordbookTermResponseForm fromFolderTermRows(
            final List<?> rows,
            final long total,
            final int page,
            final int size,
            final String sort
    ) {
        final int safeSize = Math.max(1, size);
        final int totalPages = (int) Math.ceil((double) total / safeSize);

        if (rows == null || rows.isEmpty()) {
            return new ListUserWordbookTermResponseForm(
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

        return new ListUserWordbookTermResponseForm(items, total, totalPages, page, safeSize, sort);
    }
}
