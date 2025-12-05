package com.cygnus.ipoten.wordbook.repository.projection;

import java.time.Instant;

public interface FolderStatsRow {
    Long getId();
    String getFolderName();
    Long getTermCount();
    Long getLearnedCount();
    Instant getUpdatedAt();
    Instant getLastStudiedAt();
}
