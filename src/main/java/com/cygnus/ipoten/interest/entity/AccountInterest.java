package com.cygnus.ipoten.interest.entity;

import com.cygnus.ipoten.account.entity.Account;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(
        name = "account_interest",
        indexes = {
                @Index(name = "idx_account_interest_account_id", columnList = "account_id"),
                @Index(name = "idx_account_interest_interest_id", columnList = "interest_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_account_interest_account_interest",
                        columnNames = {"account_id", "interest_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountInterest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interest_id", nullable = false)
    private Interest interest;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public AccountInterest(Account account, Interest interest) {
        this.account = account;
        this.interest = interest;
        this.createdAt = Instant.now();
    }
}
