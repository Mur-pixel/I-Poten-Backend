package com.cygnus.ipoten.user_term.entity;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.term.entity.Term;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "user_recent_term",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_urt_account_term", columnNames = {"account_id", "term_id"})
        },
        indexes = {
            @Index(name = "idx_urt_account_lastseen", columnList="account_id, last_seen_at"),
            @Index(name = "idx_urt_term", columnList = "term_id")
        }
)
@NoArgsConstructor
@AllArgsConstructor
public class UserRecentTerm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "FK_urt_account_cascade"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Account accountRef;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    @Column(name = "first_seen_at", nullable = false)
    private LocalDateTime firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    @Column(name = "view_count", nullable = false)
    private Long viewCount;

    // 생성 팩토리 : 최초 1회만 올바른 상태로 만들기
    public static UserRecentTerm create(Long accountId, Term term, LocalDateTime now) {
        UserRecentTerm userRecentTerm = new UserRecentTerm();
        userRecentTerm.accountId = accountId;
        userRecentTerm.term = term;
        userRecentTerm.firstSeenAt = now;
        userRecentTerm.lastSeenAt = now;
        userRecentTerm.viewCount = 1L;
        return userRecentTerm;
    }

    // 열람 시 갱신 : 마지막 본 시각 + 카운트 증가
    public void touch(LocalDateTime now) {
        this.lastSeenAt = now;
        this.viewCount = (this.viewCount == null ? 1L : this.viewCount + 1);
    }

}
