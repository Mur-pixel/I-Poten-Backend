package com.cygnus.ipoten.quiz_wrongnote.service;

import com.cygnus.ipoten.quiz_question.entity.enums.DifficultyLevel;

import java.time.LocalDate;

public record WrongNoteSearchCondition(
        String q,
        String typeUpper,
        DifficultyLevel difficultyLevel,
        boolean unresolvedOnly,
        SortKey sortKey,
        Long sessionId,
        LocalDate from,
        LocalDate to
) {
    public enum SortKey { RECENT, OLDEST, MOST_WRONG }

    public static WrongNoteSearchCondition of(
            String q,
            String type,
            String difficultyLevel,
            boolean unresolvedOnly,
            String sort,
            Long sessionId,
            LocalDate from,
            LocalDate to
    ) {
        String nq = (q == null) ? null : q.trim();
        if (nq != null && nq.isBlank()) nq = null;

        String nt = (type == null) ? null : type.trim().toUpperCase();
        if (nt != null && nt.isBlank()) nt = null;

        DifficultyLevel nd = null;
        if (difficultyLevel != null && !difficultyLevel.trim().isBlank()) {
            nd = DifficultyLevel.valueOf(difficultyLevel.trim().toUpperCase());
        }

        SortKey sk = SortKey.RECENT;
        if (sort != null && !sort.trim().isBlank()) {
            sk = SortKey.valueOf(sort.trim().toUpperCase());
        }

        return new WrongNoteSearchCondition(nq, nt, nd, unresolvedOnly, sk, sessionId, from, to);
    }
}
