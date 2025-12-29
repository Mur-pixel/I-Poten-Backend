package com.cygnus.ipoten.quiz_session.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SeedMode {
    /**
     * AUTO : 매번 다른 시드로 셔플합니다.
     * 재현성보다 다양성이 중요할 때 사용합니다.
     * 서버 시간/난수원을 섞어 결과가 호출마다 달라집니다.
     */
    AUTO,

    /**
     * DAILY : 같은 유저, 같은 날짜(accountId + yyyy-MM-dd)를 기준으로 시드를 고정합니다.
     * 같은 사용자/같은 날짜/같은 입력이면 동일한 세트가 생성됩니다.
     * 다음 날이 되면 자동으로 다른 세트가 만들어집니다.
     */
    DAILY,

    /**
     * FIXED : 외부에서 주입한 고정 시드를 그대로 사용합니다.
     * 항상 동일한 결과가 필요할 때(테스트, 리그레이션) 사용합니다.
     * fixedSeed가 없으면 예외를 던지는 것이 안전합니다.
     */
    FIXED;

    @JsonCreator
    public static SeedMode from(String value) {
        if (value == null || value.isBlank()) return AUTO;
        try {
            return SeedMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return AUTO; // 알 수 없는 값은 AUTO로 폴백
        }
    }

    @JsonValue
    public String toJson() {
        return name();
    }
}
