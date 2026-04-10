package com.cygnus.iptn.quiz_set.service.request;

import com.cygnus.iptn.quiz_question.entity.enums.DifficultyLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 폴더 기반 퀴즈 세트 생성 요청(서비스 내부용) */
@Getter
@RequiredArgsConstructor
public class CreateQuizSetByWordbookRequest {
    private final Long accountId;
    private final Long wordbookId;
    private final int count;
    private final QuestionType questionType;
    private final DifficultyLevel difficulty;
    private final String title;

    public enum QuestionType {
        MIX, CHOICE, OX, INITIALS
    }
}
