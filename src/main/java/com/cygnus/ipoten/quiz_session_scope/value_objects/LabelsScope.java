package com.cygnus.ipoten.quiz_session_scope.value_objects;

import java.util.List;

public record LabelsScope(
        Long accountId,
        Integer count,
        QuestionTypeScope questionTypeScope,
        DifficultyScope difficultyScope,
        List<String> labelKeys
) {
}
