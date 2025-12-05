package com.cygnus.ipoten.wordbook.controller.request_form;

import com.cygnus.ipoten.wordbook.service.request.AttachTermsBulkRequest;

import java.util.List;
import java.util.Objects;

public record AttachTermsBulkRequestForm(
        List<Long> termIds,
        String dedupeMode
) {
    public AttachTermsBulkRequest toRequest(Long accountId, Long folderId) {
        return new AttachTermsBulkRequest(
                accountId,
                folderId,
                termIds == null ? List.of() : termIds.stream().filter(Objects::nonNull).distinct().toList(),
                AttachTermsBulkRequest.DedupeMode.of(dedupeMode)
        );
    }
}
