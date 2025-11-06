package com.cygnus.ipoten.user_term.service.response;

import java.util.List;

public record AttachTermsBulkResponse(
        Long folderId,
        int requested,
        int attached,
        int skipped,
        int failed,
        List<Long> invalidIds
) {
}
