package com.cygnus.ipoten.awsCost.service;

import com.cygnus.ipoten.awsCost.entity.AwsDailyCost;

import java.time.LocalDate;
import java.util.List;

public interface AwsCostService {
    List<AwsDailyCost> getDailyTotalCost(LocalDate start, LocalDate end);
}
