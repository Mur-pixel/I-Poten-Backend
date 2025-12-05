package com.cygnus.ipoten.wordbook.service.view;

import com.cygnus.ipoten.custom_term_learning.entity.enums.LearningStatus;

import java.time.LocalDateTime;

public record FolderTermRow(
        Long userWordbookTermId,
        Long termId,
        String title,
        String description,
        LocalDateTime createdAt,
        LearningStatus status
) {}
