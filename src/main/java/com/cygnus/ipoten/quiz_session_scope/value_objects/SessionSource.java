package com.cygnus.ipoten.quiz_session_scope.value_objects;

import com.cygnus.ipoten.quiz_session.entity.enums.SessionSourceType;
import com.cygnus.ipoten.quiz_set.entity.enums.QuizSetType;
import lombok.Value;

@Value
public class SessionSource {
    SessionSourceType sourceType;
    Long sourceId;
    String sourceKey;
    QuizSetType partType;

    public static SessionSource of(SessionSourceType type, Long id, QuizSetType partType) {
        String key = switch (type) {
            case SET -> "set:" + id;
            case TERM_CATEGORY -> "termCategory:" + id;
            case WORDBOOK -> "wordbook:" + id;
        };
        return new SessionSource(type, id, key, partType);
    }
}