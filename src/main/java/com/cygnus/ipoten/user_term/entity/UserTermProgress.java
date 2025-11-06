package com.cygnus.ipoten.user_term.entity;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.term.entity.Term;
import com.cygnus.ipoten.user_term.entity.enums.MemorizationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Entity
@Table(name = "user_term_progress")
@NoArgsConstructor
public class UserTermProgress {

    @Embeddable
    @Getter
    @NoArgsConstructor
    public static class Id implements Serializable {
        @Column(name = "account_id", nullable = false)
        private Long accountId;

        @Column(name = "term_id", nullable = false)
        private Long termId;

        public Id(Long accountId, Long termId) {
            this.accountId = accountId;
            this.termId = termId;
        }

        @Override
        public boolean equals(Object o) {
            if(this == o) return true;
            if(!(o instanceof Id that)) return false;
            return Objects.equals(accountId, that.accountId) && Objects.equals(termId, that.termId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(accountId, termId);
        }
    }

    @EmbeddedId
    private Id id = new Id();

    @MapsId("accountId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_utp_account_cascade"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Account account;

    @MapsId("termId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_utp_term_restrict"))
    private Term term;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MemorizationStatus status = MemorizationStatus.LEARNING;

    @Column(name = "memorized_at")
    private LocalDateTime memorizedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_studied_at")
    private LocalDateTime lastStudiedAt;

    @PrePersist
    public void onCreate() {
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void changeStatus(MemorizationStatus status) {
        if (this.status == status) return;
        this.status = status;
        this.memorizedAt = (status == MemorizationStatus.MEMORIZED) ? LocalDateTime.now() : null;
    }

    public void markStudiedNow() {
        this.lastStudiedAt = LocalDateTime.now();
    }

    public static UserTermProgress newOf(Account accountRef, Term termRef) {
        UserTermProgress p = new UserTermProgress();
        p.account = accountRef;
        p.term = termRef;
        p.status = MemorizationStatus.LEARNING;
        p.memorizedAt = null;
        return p;
    }
}
