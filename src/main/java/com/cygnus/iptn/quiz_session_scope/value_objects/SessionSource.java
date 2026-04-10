package com.cygnus.iptn.quiz_session_scope.value_objects;

import com.cygnus.iptn.quiz_session.entity.enums.SessionSourceType;
import com.cygnus.iptn.quiz_set.entity.enums.QuizSetType;
import lombok.Value;

@Value
public class SessionSource {
    SessionSourceType sourceType;
    Long sourceId;
    String sourceKey;
    QuizSetType partType;

    public static SessionSource of(SessionSourceType type, Long id, QuizSetType partType) {
        Long safeId = (id == null) ? 0L : id;

        String key = switch (type) {
            case SET -> "set:" + safeId;
            case TERM_CATEGORY -> "termCategory:" + safeId;
            case WORDBOOK -> "wordbook:" + safeId;
            case WRONG_NOTE -> "wrongNote:" + safeId;
            case LABELS -> "labels:" + safeId;
        };

        return new SessionSource(type, id, key, partType);
    }

    public static SessionSource wrongNote(Long accountId) {
        return of(SessionSourceType.WRONG_NOTE, accountId, null);
    }
}