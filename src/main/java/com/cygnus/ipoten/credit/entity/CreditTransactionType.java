package com.cygnus.ipoten.credit.entity;


public enum CreditTransactionType {
    CHARGE,   // 유료 충전
    USE,      // 사용
    REFUND,   // 환불
    BONUS,    // 이벤트 / 무료 지급
    EXPIRE    // 만료
}
