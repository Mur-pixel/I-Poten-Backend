package com.cygnus.iptn.quiz_daily.service;

import com.cygnus.iptn.quiz_session.entity.enums.SeedMode;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Service
public class DailySeedServiceImpl implements DailySeedService {

    public long resolveSeed(SeedMode seedMode, Long accountId, LocalDate ymd, Long fixedSeed, String salt) {

        if (seedMode == null) {
            seedMode = SeedMode.AUTO;
        }

        return switch (seedMode) {
            case AUTO -> System.nanoTime() ^ System.currentTimeMillis();
            case DAILY -> fnv1a64(accountId + ":" + ymd + ":" + safe(salt));
            case FIXED -> {
                if (fixedSeed == null) throw new IllegalArgumentException("FIXED seedMode에는 fixedSeed가 필요합니다.");
                yield fixedSeed;
            }
        };
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static long fnv1a64(String input) {
        byte[] data = input.getBytes(StandardCharsets.UTF_8);
        long hash = 0xcbf29ce484222325L;
        for (byte b : data) {
            hash ^= (b & 0xff);
            hash *= 0x100000001b3L;
        }
        return hash;
    }
}
