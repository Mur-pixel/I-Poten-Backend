package com.cygnus.ipoten.user_term.repository.projection;

import java.time.LocalDateTime;

public interface FolderStatsRow {
    Long getId();
    String getFolderName();
    Long getTermCount();
    Long getLearnedCount();
    LocalDateTime getUpdatedAt();
    LocalDateTime getLastStudiedAt();
}
