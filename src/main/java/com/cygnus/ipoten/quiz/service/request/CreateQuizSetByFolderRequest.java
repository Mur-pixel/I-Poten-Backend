package com.cygnus.ipoten.quiz.service.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 폴더 기반 퀴즈 세트 생성 요청(서비스 내부용) */
@Getter
@RequiredArgsConstructor
public class CreateQuizSetByFolderRequest {
    private final Long accountId;
    private final Long folderId;
    private final int count;
    private final boolean isRandom;
    private final QuestionType questionType;
    private final String difficulty;
    private final String title;

    public enum QuestionType {
        MIX, CHOICE, OX, INITIALS
    }
}
