package com.cygnus.ipoten.quiz.service.util;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class AnswerIndexPlannerTest {
    @Test
    void roundRobin_isBalanced_overManyTrials() {
        AnswerIndexPlanner p = new AnswerIndexPlanner(4, 12345L);
        int optionCount = 4;
        int trials = 4000;

        int[] cnt = new int[optionCount];
        for (int i = 0; i < trials; i++) {
            int idx = p.nextIndex();
            cnt[idx]++;
        }

        // 각 인덱스가 25% ±5%p 안에 들어오는지 (대략적 균등성)
        for (int c : cnt) {
            double ratio = c / (double) trials;
            assertThat(ratio).isBetween(0.20, 0.30);
        }
    }

    @Test
    void sameSeed_sameSequence() {
        AnswerIndexPlanner p1 = new AnswerIndexPlanner(4, 777L);
        AnswerIndexPlanner p2 = new AnswerIndexPlanner(4, 777L);

        for (int i = 0; i < 100; i++) {
            assertThat(p1.nextIndex()).isEqualTo(p2.nextIndex());
        }
    }
}
