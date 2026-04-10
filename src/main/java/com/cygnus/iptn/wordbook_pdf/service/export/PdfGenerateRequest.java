package com.cygnus.iptn.wordbook_pdf.service.export;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class PdfGenerateRequest {
    private final Long accountId;
    private final List<Long> termIds;
    private final Long wordbookId;
    private final String title;

    public boolean isFolderMode() {
        return wordbookId != null && (termIds == null || termIds.isEmpty());
    }

    public boolean isTermIdsMode() {
        return termIds != null && !termIds.isEmpty();
    }
}