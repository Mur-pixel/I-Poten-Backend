package com.cygnus.iptn.quiz_wrongnote.repository.projection;

import java.time.Instant;

public interface WrongNoteQuestionSummaryView {
    Long getRepresentativeWrongNoteId();
    Long getQuestionId();
    Long getWrongCount();
    Long getUnresolvedCount();
    Instant getLatestWrongAt();
}
