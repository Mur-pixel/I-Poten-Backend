package com.cygnus.ipoten.quiz.controller.response_form;

import com.cygnus.ipoten.quiz.entity.enums.SessionMode;
import com.cygnus.ipoten.quiz.entity.enums.SessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 퀴즈 세션 목록 응답 폼
 * 사용자의 퀴즈 세션 이력을 목록 형태로 반환
 * 진행 중, 제출 완료, 만료된 세션 등 모든 상태의 세션 정보 포함
 */
@Data
@AllArgsConstructor
@Builder
public class SessionListResponseForm {
    private List<Item> items; // 세션 목록

    /**
     * 개별 세션 정보
     * 세션의 진행 상태, 점수, 소요 시간 등 요약 정보 제공
     */
    @Data
    @AllArgsConstructor
    @Builder
    public static class Item {
        private Long sessionId;             // 세션 ID
        private SessionStatus status;       // 세션 상태(IN_PROGRESS, SUBMITTED, EXPIRED)
        private SessionMode mode;           // 세션 모드(FULL, WRONG_ONLY)
        private Integer total;              // 총 문항 수
        private Integer correct;            // 맞은 개수(제출 전이면 null)
        private Long elapsedMs;             // 소요시간(제출 전이면 null)
        private LocalDateTime startedAt;    // 퀴즈 시작 시간
        private LocalDateTime submittedAt;  // 퀴즈 제출 시간
        private Double score;               // 점수
        private Integer scorePercent;       // (correct/total)*100
        private String title;               // 퀴즈 세트 타이틀이 있는 경우

        /**
         * 백분율 점수 계산
         * DB에 저장된 값이 있으면 반환, 없으면 (맞은 개수/총 문항 수)*100 계산
         * @return 백분율 점수 (0~100), 계산 불가 시 null
         */
        public Integer getScorePercent() {
            if (scorePercent != null) return scorePercent;
            if (correct != null && total != null && total >0) {
                return (int) Math.round(correct * 100.0 / total);
            }
            return null;
        }

        /**
         * 점수 계산
         * DB에 저장된 값이 있으면 반환, 없으면 (맞은 개수/총 문항 수)*100 계산
         * @return 점수 (0.0~100.0), 계산 불가 시 null
         */
        public Double getScore() {
            if (score != null) return score;
            if (correct != null && total != null && total >0) {
                return correct * 100.0 / total;
            }
            return null;
        }
    }

}
