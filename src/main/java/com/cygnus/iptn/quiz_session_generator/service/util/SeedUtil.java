package com.cygnus.iptn.quiz_session_generator.service.util;

import com.cygnus.iptn.quiz_session.entity.enums.SeedMode;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

public class SeedUtil {
    private final Clock clock;

    public SeedUtil() {
        this(Clock.systemDefaultZone());
    }

    public SeedUtil(Clock clock) {
        this.clock = clock;
    }

    /** 모드/사용자/고정시드로 최종 seed 계산 */
    public long resolveSeed(SeedMode mode, Long accountId, Long fixedSeed) {
        if (mode == null) mode = SeedMode.AUTO;

        switch (mode) {
            case AUTO:
                // 매번 문제가 달라지게 - System.nanoTime 기반 + random salt
                return System.nanoTime() ^ ThreadLocalRandom.current().nextLong();

            case DAILY:
                // 같은 유저, 같은 날짜 -> 같은 시드
                LocalDate today = LocalDate.now(clock);
                String key = (accountId == null ? "0" : accountId.toString()) + "|" + today.toString();
                return hashToLong(key);

            case FIXED:
                // 클라이언트가 준 시드 쓰기
                if (fixedSeed == null) {
                    throw new IllegalArgumentException("FIXED 모드는 fixedSeed가 필요합니다.");
                }
                return fixedSeed;

            default:
                return System.nanoTime();
        }
    }

    /** SHA-256 → 상위 8바이트를 long으로 */
    private static long hashToLong(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.wrap(d, 0, 8).getLong();
        } catch (Exception e) {
            return s.hashCode();
        }
    }
}
