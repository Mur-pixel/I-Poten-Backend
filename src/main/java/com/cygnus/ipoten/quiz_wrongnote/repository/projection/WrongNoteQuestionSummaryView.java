package com.cygnus.ipoten.quiz_wrongnote.repository.projection;

import java.time.Instant;

public interface WrongNoteQuestionSummaryView {
    Long getRepresentativeWrongNoteId();
    Long getQuestionId();
    Long getWrongCount();
    Long getUnresolvedCount();
    Instant getLatestWrongAt();
}
