package com.cygnus.ipoten.interest.entity;

import com.cygnus.ipoten.account.entity.Account;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "account_interest_tag",
        indexes = {
                @Index(name = "idx_account_interest_tag_account_id", columnList = "account_id"),
                @Index(name = "idx_account_interest_tag_interest_tag_id", columnList = "interest_tag_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_account_interest_tag_account_tag",
                        columnNames = {"account_id", "interest_tag_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountInterestTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interest_tag_id", nullable = false)
    private InterestTag interestTag;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public AccountInterestTag(Account account, InterestTag interestTag) {
        this.account = account;
        this.interestTag = interestTag;
        this.createdAt = LocalDateTime.now();
    }
}