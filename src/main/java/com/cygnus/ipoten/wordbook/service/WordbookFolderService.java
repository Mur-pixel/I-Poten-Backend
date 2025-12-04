package com.cygnus.ipoten.wordbook.service;

import com.cygnus.ipoten.wordbook.service.request.*;
import com.cygnus.ipoten.wordbook.service.response.*;

import java.util.List;

public interface WordbookFolderService {
    CreateWordbookFolderResponse registerWordbookFolder(CreateWordbookFolderRequest request);
    ListWordbookTermResponse list(ListWordbookTermRequest request);
    void reorder(ReorderWordbookFoldersRequest request);
    CreateWordbookTermResponse attachTerm(CreateWordbookTermRequest request);
    MoveFolderTermsResponse moveTerms(Long accountId, Long sourceFolderId, Long targetFolderId, List<Long> termIds);
    RenameWordbookFolderResponse rename(RenameWordbookFolderRequest request);

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
    void deleteOne(Long accountId, DeleteMode mode, Long folderId, Long targetFolderId);
    void deleteBulk(Long accountId, DeleteMode mode, List<Long> folderIds, Long targetFolderIds);
    TermIdsResult getAllTermIds(Long accountId, Long folderId);
    record TermIdsResult(Long folderId, List<Long> termIds, boolean limitExceeded, int limit, int total) {}
    ExportTermIdsResult collectExportTermIds(Long accountId, Long folderId, String memorization, List<String> includeTags, List<String> excludeTags, String sort, int hardLimit);
    record ExportTermIdsResult(Long folderId, List<Long> termIds, int totalBeforeFilter, int filteredOutCount, boolean limitExceeded, int limit, int totalAfterFilter) {}
    AttachTermsBulkResponse attachTermsBulk(AttachTermsBulkRequest request);
}
