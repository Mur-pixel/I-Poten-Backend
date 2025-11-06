package com.cygnus.ipoten.quiz.service.util;

import com.cygnus.ipoten.quiz.entity.enums.SeedMode;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class SeedUtilTest {

    // DAILY 모드: 같은 사용자(userId)가 같은 날짜에 호출하면 동일한 시드값이 반환되는지 테스트
    @Test
    void daily_sameUserSameDay_sameSeed() {
        SeedUtil util = new SeedUtil(Clock.fixed(Instant.parse("2025-09-30T00:00:00Z"), ZoneId.of("UTC")));
        long s1 = util.resolveSeed(SeedMode.DAILY, 123L, null);
        long s2 = util.resolveSeed(SeedMode.DAILY, 123L, null);
        assertEquals(s1, s2);
    }

    // FIXED 모드: fixedSeed가 같으면 userId가 달라도 동일한 시드값이 반환되는지 테스트
    @Test
    void fixed_sameFixedSeed_sameResult() {
        SeedUtil util = new SeedUtil();
        long s1 = util.resolveSeed(SeedMode.FIXED, 999L, 42L);
        long s2 = util.resolveSeed(SeedMode.FIXED, 111L, 42L);
        assertEquals(s1, s2); // fixedSeed만 중요
    }

    // AUTO 모드: 매번 다른 랜덤 시드값이 생성되는지 테스트 (매우 드물게 같을 수 있음)
    @Test
    void auto_oftenDifferent() {
        SeedUtil util = new SeedUtil();
        long s1 = util.resolveSeed(SeedMode.AUTO, 1L, null);
        long s2 = util.resolveSeed(SeedMode.AUTO, 1L, null);
        assertNotEquals(s1, s2); // 매우 드물게 같을 수 있으니 필요하면 여러 번 시도
    }
}
