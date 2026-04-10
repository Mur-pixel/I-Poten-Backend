package com.cygnus.iptn.wordbook_log.service;

public interface WordbookLogService {
    void record(Long accountId, String eventType, Long wordbookId, Long termId, String memoStatus, Integer amount, String extra);

    default void recordTermSaved(Long accountId, Long wordbookId, Long termId) {
        record(accountId, "TERM_SAVED", wordbookId, termId, null, 1, null);
    }

    default void recordTermsSavedBulk(Long accountId, Long wordbookId, int count) {
        record(accountId, "TERM_SAVED", wordbookId, null, null, count, "BULK");
    }

    default void recordMemoChanged(Long accountId, Long wordbookId, Long termId, String memoStatus) {
        record(accountId, "MEMO_STATUS_CHANGED", wordbookId, termId, memoStatus, 1, null);
    }

    default void recordPdfDownloaded(Long accountId, Long wordbookId, int termCount, Long ebookId) {
        record(accountId, "PDF_DOWNLOADED", wordbookId, null, null, termCount,
                ebookId == null ? null : ("ebookId=" + ebookId));
    }
}
