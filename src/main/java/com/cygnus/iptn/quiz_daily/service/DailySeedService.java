package com.cygnus.iptn.quiz_daily.service;

import com.cygnus.iptn.quiz_session.entity.enums.SeedMode;
import java.time.LocalDate;

public interface DailySeedService {
    long resolveSeed(SeedMode seedMode, Long accountId, LocalDate ymd, Long fixedSeed, String salt);
}
