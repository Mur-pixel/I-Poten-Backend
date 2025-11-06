package com.cygnus.ipoten.quiz.controller.response_form;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 세션 문항 페이지 응답 DTO
 * - 스냅샷 순서 기준으로 offset/limit 페이징된 문항 목록(items)을 담습니다.
 * - SUBMITTED 상태가 아니면 정답 노출(Boolean isAnswer)은 null로 내려옵니다.
 */
@Getter
@Builder
public class SessionItemsPageResponseForm {

    private final Long sessionId;  // 세션 ID
    private final int offset;      // 요청 오프셋
    private final int limit;       // 요청 리밋
    private final int total;       // 전체 문항 수
    private final List<Item> items;// 현재 페이지 문항들

    @Getter
    @Builder
    public static class Item {
        private final Long questionId;
        private final String questionText;
        private Long correctChoiceId;
        private String explanation;
        private final List<Choice> choices;
    }

    @Getter
    @Builder
    public static class Choice {
        private final Long id;
        private final String text;
        /**
         * SUBMITTED일 때만 true/false가 내려가고,
         * 그 외(IN_PROGRESS 등)에는 null로 내려가 정답이 숨겨집니다.
         */
        private final Boolean isAnswer;
    }
}
