package com.cygnus.ipoten.user_term.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cygnus.ipoten.user_term.controller.response_form.ListUserWordbookTermResponseForm;
import com.cygnus.ipoten.user_term.service.view.FolderTermRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class UserWordbookTermMapper {

    private final ObjectMapper om;

    public ListUserWordbookTermResponseForm toResponse(
            List<FolderTermRow> rows, long total, int page, int size, String sort
    ) {
        int safeSize = Math.max(1, size);
        int totalPages = (int) Math.ceil((double) total / safeSize);

        List<Map<String, Object>> items = (rows == null) ? List.of()
                : rows.stream()
                .map(r -> om.convertValue(r, new TypeReference<Map<String, Object>>() {}))
                .toList();

        return new ListUserWordbookTermResponseForm(items, total, totalPages, page, safeSize, sort);
    }
}
