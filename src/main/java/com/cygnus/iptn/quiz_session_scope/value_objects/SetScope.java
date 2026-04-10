package com.cygnus.iptn.quiz_session_scope.value_objects;

import com.cygnus.iptn.quiz_question.entity.enums.DifficultyLevel;
import lombok.Value;

import java.util.List;

@Value
public class SetScope {
    Long setId;

    Integer count;          // null이면 서비스 default
    String typeRaw;         // null이면 서비스 default
    DifficultyLevel level;  // null이면 서비스 default

    public static SetScope ofId(Long setId, Integer count, String typeRaw, DifficultyLevel level) {
        if (setId == null) throw new IllegalArgumentException("setId는 필수입니다.");
        return new SetScope(setId, count, typeRaw, level);
    }

    private static List<String> normalize(List<String> keys) {
        if (keys == null) return List.of();
        return keys.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.trim().toLowerCase())
                .distinct()
                .toList();
    }
}