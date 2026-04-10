package com.cygnus.iptn.quiz_session_scope.value_objects;

import com.cygnus.iptn.quiz_question.entity.enums.DifficultyLevel;
import lombok.Value;

/**
 * 퀴즈 세션에서 사용할 문제 난이도 범위를 표현하는 Value Object.
 * 
 * 이 클래스는 DifficultyLevel enum 값을 그대로 노출하지 않고,
 * 퀴즈 출제 관점에서의 의미와 판단 로직을 함께 캡슐화함
 *
 * 도메인 의미:
 * - EASY / MEDIUM / HARD는 각각 단일 난이도 수준 의미
 * - MIX는 특정 난이도 수준이 아닌, 여러 난이도를 혼합하여 출제하는 범위 정책 의미
 * 
 * 책임과 역할:
 * - 외부 입력 값을 DifficultyLevel로 변환하여 표현
 * - 난이도가 혼합 출제인지 여부를 도메인 메서드로 제공
 * - 서비스 계층에서 난이도 분기 로직을 제거하고 의미 중심 판단을 가능하게 함
 *
 *  설계 의도:
 *  - Lombok @Value를 사용한 불변(Value Object) 설계
 *  - 난이도 "수준(level)"과 출제 "정책(scope)"을 구분하여 모델링
 *
 */

@Value
public class DifficultyScope {

    DifficultyLevel level;

    public static DifficultyScope of(DifficultyLevel level) {
        return new DifficultyScope(level != null ? level : DifficultyLevel.MIX);
    }

    public static DifficultyScope from(DifficultyLevel level) {
        return of(level);
    }

    public DifficultyLevel toDifficultyLevel() {
        return level;
    }

    public boolean isEasy() {
        return level == DifficultyLevel.EASY;
    }

    public boolean isMedium() {
        return level == DifficultyLevel.MEDIUM;
    }

    public boolean isHard() {
        return level == DifficultyLevel.HARD;
    }

    public boolean isMix() {
        return level == DifficultyLevel.MIX;
    }

    public DifficultyLevel forRepoOrNull() {
        return isMix() ? null : level;
    }
}