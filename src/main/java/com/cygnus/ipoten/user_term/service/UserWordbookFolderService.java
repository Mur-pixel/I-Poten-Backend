package com.cygnus.ipoten.user_term.service;

import com.cygnus.ipoten.user_term.service.request.*;
import com.cygnus.ipoten.user_term.service.response.*;

import java.util.List;

public interface UserWordbookFolderService {
    CreateUserWordbookFolderResponse registerWordbookFolder(CreateUserWordbookFolderRequest request);
    ListUserWordbookTermResponse list(ListUserWordbookTermRequest request);
    void reorder(ReorderUserWordbookFoldersRequest request);
    CreateUserWordbookTermResponse attachTerm(CreateUserWordbookTermRequest request);
    MoveFolderTermsResponse moveTerms(Long accountId, Long sourceFolderId, Long targetFolderId, List<Long> termIds);
    RenameUserWordbookFolderResponse rename(RenameUserWordbookFolderRequest request);

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
