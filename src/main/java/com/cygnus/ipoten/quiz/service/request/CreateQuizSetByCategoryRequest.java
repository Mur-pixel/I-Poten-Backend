package com.cygnus.ipoten.quiz.service.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@RequiredArgsConstructor
public class CreateQuizSetByCategoryRequest {

    private final String title;
    private final Long categoryId;
    private final boolean isRandom;

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

    public enum DifficultyLevel {
        MIX, EASY, MEDIUM, HARD;

        public static DifficultyLevel from(String v) {
            if (v == null) {
                return MEDIUM;
            }
            String s = v.trim().toUpperCase();
            return switch (s) {
                case "MIX" -> MIX;
                case "EASY" -> EASY;
                case "MEDIUM"-> MEDIUM;
                case "HARD" -> HARD;
                default -> MIX;
            };
        }
    }
}
