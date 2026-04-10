package com.cygnus.iptn.credit.entity;


import com.cygnus.iptn.account.entity.Account;
import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(
        uniqueConstraints = @UniqueConstraint(columnNames = "account_id")
)
public class CreditWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false)
    private Long balance;

    public CreditWallet() {
    }

    public void addCredit(Long amount) {
        this.balance += amount;
    }

    public CreditWallet(Account account, Long balance) {
        this.account = account;
        this.balance = balance;
    }
}
