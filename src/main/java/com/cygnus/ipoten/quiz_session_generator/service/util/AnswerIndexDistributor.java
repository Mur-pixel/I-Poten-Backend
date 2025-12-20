package com.cygnus.ipoten.quiz_session_generator.service.util;

import java.util.Random;

public final class AnswerIndexDistributor {
    private final int optionCount;
    private final int[] ring;
    private int idx;

    public AnswerIndexDistributor(int optionCount, long seed) {
        this.optionCount = optionCount;
        this.ring = new int[optionCount];
        for (int i = 0; i < optionCount; i++) ring[i] = i;
        Random r = new Random(seed);
        for (int i = optionCount - 1; i > 0; i--) {
            int j = r.nextInt(i + 1);
            int tmp = ring[i]; ring[i] = ring[j]; ring[j] = tmp;
        }
        this.idx = 0;
    }

    /** 0-based 정답 인덱스를 하나씩 라운드로빈으로 반환 */
    public int next() {
        int v = ring[idx];
        idx = (idx + 1) % optionCount;
        return v;
    }
}
