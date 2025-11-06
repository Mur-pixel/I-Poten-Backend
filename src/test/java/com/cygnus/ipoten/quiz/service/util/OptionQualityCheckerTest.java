package com.cygnus.ipoten.quiz.service.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.cygnus.ipoten.quiz.service.util.OptionQualityChecker.*;
import static org.assertj.core.api.Assertions.assertThat;

public class OptionQualityCheckerTest {

    @Test
    void nearDuplicate_variations_collapsed() {
        // ABC의 다양한 표기 → 유사/중복 취급되어 true
        assertThat(nearDuplicate("ABC", "abc")).isTrue();
        assertThat(nearDuplicate("A B C", "abc")).isTrue();
        assertThat(nearDuplicate("AB C", "A  B   C")).isTrue();

        // 전혀 다른 문자열은 false
        assertThat(nearDuplicate("network", "database")).isFalse();
    }

    @Test
    void meaningless_and_length_rules() {
        // 의미 없음
        assertThat(isMeaningless("---")).isTrue();
        assertThat(isMeaningless("   ")).isTrue();

        // 숫자만
        assertThat(isMeaningless("12345")).isTrue();

        // 단문자
        assertThat(isMeaningless("A")).isTrue();
        assertThat(isTooShort("A", Difficulty.MEDIUM)).isTrue();

        // 유효
        assertThat(isMeaningless("정답")).isFalse();
        assertThat(isTooShort("정답", Difficulty.MEDIUM)).isFalse();
    }

    @Test
    void repairOptions_filters_duplicates_and_short_and_sameAsAnswer() {
        String answer = "정답";
        int optionCount = 4;

        // 고의로 중복/부분중복/짧은 것/정답과 동일한 것 섞기
        List<String> raw = new ArrayList<>(List.of(
                answer,            // 정답
                "A",               // 너무 짧음
                "abc",             // 유효 후보
                "AB C",            // "abc"와 부분중복으로 간주
                "정답 "             // 정답과 동일(정규화)
        ));
        // optionCount만큼만 사용한다고 가정
        raw = raw.subList(0, optionCount);

        // 재생성기: 고유한 대체 텍스트를 순서대로 공급
        OptionQualityChecker.DistractorSupplier supplier = new OptionQualityChecker.DistractorSupplier() {
            int counter = 0;
            @Override public String regenerate(String ans, List<String> current) {
                counter++;
                return "대체보기" + counter;
            }
        };

        List<String> fixed = OptionQualityChecker.repairOptions(
                answer,
                raw,
                Difficulty.MEDIUM,
                /*maxRegenerateTrials*/10,
                supplier
        );

        // 1) 개수 유지
        assertThat(fixed).hasSize(optionCount);

        // 2) 정답 반드시 포함
        assertThat(fixed.stream().anyMatch(s -> sameAsAnswer(s, answer))).isTrue();

        // 3) 의미 없음/너무 짧음/정답 동일한 오답 없음
        long nonAnswerInvalids = fixed.stream()
                .filter(s -> !sameAsAnswer(s, answer))
                .filter(s -> isMeaningless(s) || isTooShort(s, Difficulty.MEDIUM) || sameAsAnswer(s, answer))
                .count();
        assertThat(nonAnswerInvalids).isZero();

        // 4) 오답끼리 중복/부분중복 없음
        List<String> nonAnswers = fixed.stream().filter(s -> !sameAsAnswer(s, answer)).toList();
        for (int i = 0; i < nonAnswers.size(); i++) {
            for (int j = i + 1; j < nonAnswers.size(); j++) {
                assertThat(nearDuplicate(nonAnswers.get(i), nonAnswers.get(j)))
                        .as("오답끼리 유사/중복이면 안 됨: %s vs %s", nonAnswers.get(i), nonAnswers.get(j))
                        .isFalse();
            }
        }
    }

    @Test
    void sameAsAnswer_normalized_equality() {
        assertThat(sameAsAnswer(" 정답 ", "정답")).isTrue();
        assertThat(sameAsAnswer("정  답", "정답")).isTrue();
        assertThat(sameAsAnswer("정답입니다", "정답")).isFalse();
    }
}
