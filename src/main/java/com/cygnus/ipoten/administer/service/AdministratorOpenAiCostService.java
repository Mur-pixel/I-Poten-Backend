package com.cygnus.ipoten.administer.service;

import java.time.Instant;

public interface AdministratorOpenAiCostService {
    String getDailyCosts(Instant startInclusiveUtc, Instant endExclusiveUtc);
}
