package com.cygnus.iptn.credit.entity;


import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Entity
public class CreditTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "credit_wallet_id")
    private CreditWallet creditWallet;

    @Enumerated(EnumType.STRING)
    private CreditTransactionType type;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private Long balanceAfter;

    private String reason;

    private LocalDateTime createdAt;

    public CreditTransaction() {
    }

    public CreditTransaction(CreditWallet creditWallet, CreditTransactionType type, Long amount, Long balanceAfter, String reason) {
        this.creditWallet = creditWallet;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
    }
}
