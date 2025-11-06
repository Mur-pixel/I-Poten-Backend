package com.cygnus.ipoten.quiz.service.util;

import java.util.*;

/**
 * 옵션 개수(예: 4지선다) 단위로 라운드로빈 정답 인덱스를 분배하는 유틸.
 * - 세션 단위 시드(seed)로 초기 순서를 결정 → 이후 nextIndex() 호출마다 순환
 * - 문제 단위의 "마이크로 셔플"은 외부에서 수행하고, 얻은 인덱스에 정답을 고정 배치하면 됨
 */
public final class AnswerIndexPlanner {
    private final int optionCount;
    private final Deque<Integer> rrQueue;
    private final Random random;

    /**
     * @param optionCount 보기 개수(예: 4)
     * @param seed        세션 단위의 결정적 seed (FIXED/DAILY/AutoSeed 등)
     */
    public AnswerIndexPlanner(int optionCount, long seed) {
        if (optionCount < 2) throw new IllegalArgumentException("보기 개수는 2개 이상이어야 합니다.");
        this.optionCount = optionCount;
        this.random = new Random(seed);

        List<Integer> base = new ArrayList<>();
        for (int i = 0; i < optionCount; i++) base.add(i);
        Collections.shuffle(base, random); // 세션별 Base 순서
        this.rrQueue = new ArrayDeque<>(base);
    }

    /** 문제 1개마다 호출 → 다음 정답 인덱스 */
    public int nextIndex() {
        Integer head = rrQueue.pollFirst();
        rrQueue.offerLast(head);
        return head;
    }

    /**
     * (선택) 보기 문자열 배열을 셔플한 뒤, 정답을 지정 인덱스로 고정하는 헬퍼.
     * 엔티티(QuizChoice) 목록을 다루는 쪽은 서비스에서 직접 이동시키면 됩니다.
     */
    public List<String> arrangeWithPinnedAnswer(List<String> options, String answer, int answerIdx) {
        if (options == null || options.size() != optionCount)
            throw new IllegalArgumentException("보기 목록 크기는 " + optionCount + "개여야 합니다.");
        if (answerIdx < 0 || answerIdx >= optionCount)
            throw new IllegalArgumentException("정답 인덱스가 범위를 벗어났습니다.");

        List<String> shuffled = new ArrayList<>(options);
        Collections.shuffle(shuffled, random);

        int currentAnswerPos = -1;
        for (int i = 0; i < shuffled.size(); i++) {
            if (Objects.equals(shuffled.get(i), answer)) {
                currentAnswerPos = i; break;
            }
        }
        if (currentAnswerPos < 0) {
            // 정답 텍스트가 목록 안에 없다면, 첫 요소를 정답으로 치환
            currentAnswerPos = 0;
            shuffled.set(0, answer);
        }
        if (currentAnswerPos != answerIdx) {
            String tmp = shuffled.get(answerIdx);
            shuffled.set(answerIdx, shuffled.get(currentAnswerPos));
            shuffled.set(currentAnswerPos, tmp);
        }
        return shuffled;
    }
}
