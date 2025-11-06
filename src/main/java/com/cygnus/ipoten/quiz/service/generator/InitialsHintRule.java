package com.cygnus.ipoten.quiz.service.generator;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record InitialsHintRule(
        @Min(0) int minRevealedChars,               // 최소 글자수 힌트
        @Min(0) @Max(10) int revealInitialCount     // 공개할 초성 개수(글자 위치는 생성기에서 정책 적용)
) { }
