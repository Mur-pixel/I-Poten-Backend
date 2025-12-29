package com.cygnus.ipoten.quiz_daily.controller.response_form;

import com.cygnus.ipoten.quiz_question.entity.enums.QuestionType;
import com.cygnus.ipoten.quiz_session.service.response.StartQuizSessionResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class DailyQuizStartResponseForm {

    private LocalDate ymd;                // KST 기준
    private String issueType;           // "GENERAL"
    private String seedMode;            // "DAILY" OR "FIXED"
    private List<Item> sessions;        // 3개 세션(CHOICE/OX/INITIALS)

    @Getter
    @AllArgsConstructor
    public static class Item {
        private QuestionType questionType;
        private Long sessionId;
        private int count;
        private String title;
        private List<Long> questionIds;
        private List<StartQuizSessionResponse.Item> items;
    }
}
