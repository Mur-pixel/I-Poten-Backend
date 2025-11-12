package com.cygnus.ipoten.quiz.controller.response_form;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TimelineResponseForm {

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Summary {
        private long totalSets;     // 제출 완료 세션 수
        private double accuracy;    // Σcorrect / Σtotal * 100
        private double retryRate;   // 재도전 세션 수 / 제출 완료 세션 수 * 100
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Item {
        private Long id;            // sessionId
        private String title;       // 세트 제목
        private String partType;    // CHOICE / OX / INITIALS
        private Instant date;       // submittedAt
        private int correct;        // 페이지 대상 세션의 정답 수
        private int total;          // 세션 총 문항 수
        private String category;    // 카테고리 이름
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Recent {
        private String when;        // yyyy-MM-dd
        private String label;       // 라벨(세트/카테고리/타입)
    }

    private Summary summary;
    private List<Item> items;
    private List<Recent> recent;
    private long total;             // 전체 개수(페이지네이션 총합)
}
