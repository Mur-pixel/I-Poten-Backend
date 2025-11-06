package com.cygnus.ipoten.quiz.controller.request_form;

import com.cygnus.ipoten.quiz.service.request.CreateQuizSetByCategoryRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카테고리 기반 퀴즈 "세트" 생성 폼
 * 최신 CreateQuizSetByCategoryRequest 시그니처에 맞게 count/type/difficulty 포함
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateQuizSetByCategoryRequestForm {

    /** 세트 제목(선택). null/blank면 서비스에서 기본 타이틀 생성 */
    private String title;

    /** 카테고리 ID(필수) */
    @NotNull
    private Long categoryId;

    /** 랜덤 선별 여부(선택) */
    private boolean isRandom = true;

    /** 문항 수(필수) */
    @Min(1) @Max(100)
    private int count;

    /** 문제 유형: MIX/MCQ/OX/INITIAL (대소문자 무시) */
    @Pattern(regexp = "(?i)mix|choice|ox|initials",
            message = "questionType은 MIX/CHOICE/OX/INITIALS 중 하나여야 합니다.")
    private String questionType;

    /** 난이도: MIX/EASY/MEDIUM/HARD */
    @Pattern(regexp = "(?i)mix|easy|normal|hard|medium",
            message = "difficulty는 MIX/EASY/MEDIUM/HARD 중 하나여야 합니다.")
    private String difficulty;

    /** 서비스 레이어 요청으로 변환 (열거형 매핑 포함) */
    public CreateQuizSetByCategoryRequest toCategoryBasedRequest() {
        return new CreateQuizSetByCategoryRequest(
                title,
                categoryId,
                isRandom,
                count,
                parseQuestionType(questionType),
                parseDifficultyLevel(difficulty)
        );
    }

    /* ---------- 문자열 → 요청 DTO 내부 enum 매핑 ---------- */

    private CreateQuizSetByCategoryRequest.QuestionType parseQuestionType(String s) {
        return CreateQuizSetByCategoryRequest.QuestionType.from(s);
    }

    private CreateQuizSetByCategoryRequest.DifficultyLevel parseDifficultyLevel(String s) {
        // null이면 MIX 기본
        String key = (s == null || s.isBlank()) ? "MIX" : s.trim().toUpperCase();
        // DTO 내부 enum: MIX / EASY / NORMAL / HARD 가정
        try {
            return CreateQuizSetByCategoryRequest.DifficultyLevel.valueOf(key);
        } catch (IllegalArgumentException e) {
            return CreateQuizSetByCategoryRequest.DifficultyLevel.MIX;
        }
    }
}
