package com.cygnus.ipoten.quiz_session.controller.response_form;

import com.cygnus.ipoten.quiz_question.entity.enums.QuestionType;
import com.cygnus.ipoten.quiz_session.entity.enums.SessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 퀴즈 세션 복습/리뷰 응답 폼
 * 제출된 퀴즈의 전체 결과와 각 문제별 상세 정보를 담아 반환
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionReviewResponseForm {

    private Long sessionId;         // 세션 ID
    private SessionStatus status;   // 세션 상태(IN_PROGRESS, SUBMITTED, EXPIRED)
    private Integer total;          // 총 문항 수
    private Integer correct;        // 맞은 개수
    private List<Item> items;       // 문제별 상세 정보 목록

    /**
     * 개별 문제 상세 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private Long quizQuestionId;        // 문제 ID
        private QuestionType questionType;  // 문제 유형
        private String questionText;        // 문제 텍스트

        // 선택형(CHOICE/OX)
        private Long myChoiceId;

        // 텍스트형(INITIALS/주관식)
        private String mySubmittedText;
        private String expectedText;

        private boolean correct;            // 정답 여부
        private Long answerChoiceId;        // 정답 보기 ID
        private String explanation;         // 해설
        private Long termId;                // 연관된 용어 ID
        private String termTitle;           // 용어 제목
        private Long categoryId;            // 카테고리 ID
        private String categoryName;        // 카테고리 이름
        private List<Choice> choices;       // 보기 목록
    }

    /**
     * 개별 보기 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Choice {
        private Long id;        // 보기 ID
        private String text;    // 보기 텍스트
        private boolean answer; // 정답 여부
    }
}
