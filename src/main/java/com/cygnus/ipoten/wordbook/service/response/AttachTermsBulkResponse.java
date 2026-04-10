package com.cygnus.ipoten.wordbook.service.response;

import java.util.List;

public record AttachTermsBulkResponse(
        Long wordbookId,
        int requested,
        int attached,
        int skipped,
        int failed,
        List<Long> invalidIds
) {
}
