package com.cygnus.ipoten.user_term.service.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
public class MoveFolderTermsResponse {

    private final Long sourceFolderId;
    private final Long targetFolderId;
    private final int movedCount;
    private final List<Skipped> skipped;
    private final List<Long> movedTermIds;

    @Getter
    @AllArgsConstructor
    public static class Skipped {
        private final Long termId;
        private final Reason reason;

        public enum Reason {
            DUPLICATE_IN_TARGET, // 대상 폴더에 이미 있음
            NOT_IN_SOURCE,       // 소스 폴더에 없음
            TERM_NOT_FOUND,      // Term이 존재하지 않음
            SAME_FOLDER          // 소스 = 타겟
        }
    }

    /** 소스와 타겟이 같은 경우 */
    public static MoveFolderTermsResponse sameFolder(Long folderId, List<Long> termIds) {
        List<Skipped> skipped = new ArrayList<>();
        for (Long termId : termIds) {
            skipped.add(new Skipped(termId, Skipped.Reason.SAME_FOLDER));
        }
        return new MoveFolderTermsResponse(folderId, folderId, 0, skipped, List.of());
    }
}