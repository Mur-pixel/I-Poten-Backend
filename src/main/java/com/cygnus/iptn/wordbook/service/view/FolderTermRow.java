package com.cygnus.iptn.wordbook.service.view;

import com.cygnus.iptn.wordbook_learning.entity.enums.LearningStatus;

import java.time.LocalDateTime;

public record FolderTermRow(
        Long userWordbookTermId,
        Long termId,
        String title,
        String description,
        LocalDateTime createdAt,
        LearningStatus status
) {}
