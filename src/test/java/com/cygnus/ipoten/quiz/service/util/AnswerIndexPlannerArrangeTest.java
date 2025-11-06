package com.cygnus.ipoten.quiz.service.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class AnswerIndexPlannerArrangeTest {

    @Test
    void arrangeWithPinnedAnswer_places_answer_at_target_index() {
        AnswerIndexPlanner p = new AnswerIndexPlanner(4, 20240229L);

        List<String> options = List.of("정답", "오답1", "오답2", "오답3");
        int targetIdx = 2; // 0-based: 세 번째 칸

        List<String> arranged = p.arrangeWithPinnedAnswer(options, "정답", targetIdx);

        assertThat(arranged).hasSize(4);
        assertThat(arranged.get(targetIdx)).isEqualTo("정답");
        // 나머지 요소들도 셔플되어 순서가 바뀔 수 있으나, 구성원은 동일해야 함
        assertThat(arranged).containsExactlyInAnyOrder("정답", "오답1", "오답2", "오답3");
    }

    @Test
    void arrangeWithPinnedAnswer_injects_answer_if_missing() {
        AnswerIndexPlanner p = new AnswerIndexPlanner(4, 7L);

        // 의도적으로 정답 텍스트를 options에 넣지 않음
        List<String> options = List.of("가", "나", "다", "라");
        int targetIdx = 0;

        List<String> arranged = p.arrangeWithPinnedAnswer(options, "정답", targetIdx);

        assertThat(arranged).hasSize(4);
        assertThat(arranged.get(targetIdx)).isEqualTo("정답");
        // 하나는 정답으로 치환되므로, 기존 4개 중 1개가 교체되는 것은 허용
        assertThat(arranged).contains("정답");
    }
}
