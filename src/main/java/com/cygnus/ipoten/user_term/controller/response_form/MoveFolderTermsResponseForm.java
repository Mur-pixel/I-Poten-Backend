package com.cygnus.ipoten.user_term.controller.response_form;

import com.cygnus.ipoten.user_term.service.response.MoveFolderTermsResponse;
import lombok.Getter;

import java.util.List;

@Getter
public class MoveFolderTermsResponseForm {

    private final Long sourceFolderId;
    private final Long targetFolderId;
    private final int movedCount;
    private final int skippedCount;
    private final List<SkippedForm> skipped;
    private final List<Long> movedTermIds;

    @Getter
    public static class SkippedForm {
        private final Long termId;
        private final String reason;

        public SkippedForm(MoveFolderTermsResponse.Skipped skipped) {
            this.termId = skipped.getTermId();
            this.reason = skipped.getReason().name();
        }
    }

    private MoveFolderTermsResponseForm(MoveFolderTermsResponse src) {
        this.sourceFolderId = src.getSourceFolderId();
        this.targetFolderId = src.getTargetFolderId();
        this.movedCount = src.getMovedCount();
        this.skipped = src.getSkipped().stream().map(SkippedForm::new).toList();
        this.skippedCount = this.skipped.size();
        this.movedTermIds = src.getMovedTermIds();
    }

    public static MoveFolderTermsResponseForm from(MoveFolderTermsResponse src) {
        return new MoveFolderTermsResponseForm(src);
    }


}
