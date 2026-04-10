package com.cygnus.iptn.wordbook.controller.response_form;

import com.cygnus.iptn.wordbook.service.response.MoveWordbookTermsResponse;
import lombok.Getter;

import java.util.List;

@Getter
public class MoveWordbookTermsResponseForm {

    private final Long sourceWordbookId;
    private final Long targetWordbookId;
    private final int movedCount;
    private final int skippedCount;
    private final List<SkippedForm> skipped;
    private final List<Long> movedTermIds;

    @Getter
    public static class SkippedForm {
        private final Long termId;
        private final String reason;

        public SkippedForm(MoveWordbookTermsResponse.Skipped skipped) {
            this.termId = skipped.getTermId();
            this.reason = skipped.getReason().name();
        }
    }

    private MoveWordbookTermsResponseForm(MoveWordbookTermsResponse src) {
        this.sourceWordbookId = src.getSourceWordbookId();
        this.targetWordbookId = src.getTargetWordbookId();
        this.movedCount = src.getMovedCount();
        this.skipped = src.getSkipped().stream().map(SkippedForm::new).toList();
        this.skippedCount = this.skipped.size();
        this.movedTermIds = src.getMovedTermIds();
    }

    public static MoveWordbookTermsResponseForm from(MoveWordbookTermsResponse src) {
        return new MoveWordbookTermsResponseForm(src);
    }


}
