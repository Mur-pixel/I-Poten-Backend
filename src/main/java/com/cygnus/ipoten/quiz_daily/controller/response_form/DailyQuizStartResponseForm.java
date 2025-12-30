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

    private LocalDate todayYmd;         // 서버 기준 오늘(KST)
    private LocalDate activeYmd;        // 실제로 내려준 데일리(어제일 수도 있음)
    private boolean carryOver;          // todayYml != activeYmd
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
