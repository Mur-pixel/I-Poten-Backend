package com.cygnus.ipoten.quiz_daily.service;

import com.cygnus.ipoten.quiz_session.entity.enums.SeedMode;
import java.time.LocalDate;

public interface DailySeedService {
    long resolveSeed(SeedMode seedMode, Long accountId, LocalDate ymd, Long fixedSeed, String salt);
}
