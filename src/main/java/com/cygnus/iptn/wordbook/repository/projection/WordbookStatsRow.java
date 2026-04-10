package com.cygnus.iptn.wordbook.repository.projection;

import java.time.Instant;

public interface WordbookStatsRow {
    Long getId();
    String getWordbookName();
    Long getTermCount();
    Long getLearnedCount();
    Instant getUpdatedAt();
    Instant getLastStudiedAt();
}
