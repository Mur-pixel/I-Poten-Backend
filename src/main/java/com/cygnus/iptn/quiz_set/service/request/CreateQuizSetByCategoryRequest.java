package com.cygnus.iptn.quiz_set.service.request;

import com.cygnus.iptn.quiz_question.entity.enums.DifficultyLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@RequiredArgsConstructor
public class CreateQuizSetByCategoryRequest {

    private final String title;
    private final Long categoryId;

    private final int count;
    private final QuestionType questionType;
    private final DifficultyLevel difficulty;

    public enum QuestionType {
        MIX, CHOICE, OX, INITIALS;

        public static QuestionType from(String v) {
            if (v == null) {
                return MIX;
            }
            return switch (v.trim().toUpperCase()) {
                case "CHOICE" -> CHOICE;
                case "OX" -> OX;
                case "INITIALS" -> INITIALS;
                default -> MIX;
            };
        }
    }
}
