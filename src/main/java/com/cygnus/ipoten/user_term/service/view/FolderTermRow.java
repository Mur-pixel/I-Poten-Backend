package com.cygnus.ipoten.user_term.service.view;

import com.cygnus.ipoten.user_term.entity.enums.MemorizationStatus;

import java.time.LocalDateTime;

public record FolderTermRow(
        Long userWordbookTermId,
        Long termId,
        String title,
        String description,
        LocalDateTime createdAt,
        MemorizationStatus status
) {}
