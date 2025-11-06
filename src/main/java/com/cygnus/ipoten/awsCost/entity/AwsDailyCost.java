package com.cygnus.ipoten.awsCost.entity;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class AwsDailyCost {
    private LocalDate date;
    private BigDecimal amount;
    private String unit; // "USD"
}