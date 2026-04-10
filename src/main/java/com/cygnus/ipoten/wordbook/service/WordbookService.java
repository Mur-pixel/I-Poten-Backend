package com.cygnus.ipoten.wordbook.service;

import com.cygnus.ipoten.wordbook.service.request.*;
import com.cygnus.ipoten.wordbook.service.response.*;

import java.util.List;

public interface WordbookService {
    CreateWordbookResponse registerWordbook(CreateWordbookRequest request);
    ListWordbookTermResponse list(ListWordbookTermRequest request);
    void reorder(ReorderWordbookRequest request);
    CreateWordbookTermResponse attachTerm(CreateWordbookTermRequest request);
    RenameWordbookResponse rename(RenameWordbookRequest request);

    // 삭제 관련
    enum DeleteMode { FORBID, DETACH, MOVE, PURGE;
        public static DeleteMode of(String raw) {
            if (raw == null) return PURGE;
            return switch (raw.toLowerCase()) {
                case "forbid" -> FORBID;
                case "detach" -> DETACH;
                case "move" -> MOVE;
                case "purge" -> PURGE;
                default -> PURGE;
            };
        }}
    void deleteOne(Long accountId, DeleteMode mode, Long wordbookId, Long targetWordbookId);
    void deleteBulk(Long accountId, DeleteMode mode, List<Long> wordbookIds, Long targetWordbookIds);
    TermIdsResult getAllTermIds(Long accountId, Long wordbookId);
    record TermIdsResult(Long wordbookId, List<Long> termIds, boolean limitExceeded, int limit, int total) {}
    ExportTermIdsResult collectExportTermIds(Long accountId, Long wordbookId, String memorization, List<String> includeTags, List<String> excludeTags, String sort, int hardLimit);
    record ExportTermIdsResult(Long wordbookId, List<Long> termIds, int totalBeforeFilter, int filteredOutCount, boolean limitExceeded, int limit, int totalAfterFilter) {}
    AttachTermsBulkResponse attachTermsBulk(AttachTermsBulkRequest request);
}
