package com.cygnus.ipoten.wordbook_learning.entity;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.term.entity.Term;
import com.cygnus.ipoten.wordbook_learning.entity.enums.LearningStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Getter
@Entity
@Table(name = "learning_progress")
@NoArgsConstructor
public class LearningProgress {

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
            foreignKey = @ForeignKey(name = "FK_lp_account_cascade"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Account account;

    @MapsId("termId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_lp_term_restrict"))
    private Term term;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LearningStatus status = LearningStatus.LEARNING;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_studied_at")
    private Instant lastStudiedAt;

    @PrePersist
    public void onCreate() {
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void changeStatus(LearningStatus status) {
        if (this.status == status) return;
        this.status = status;
        this.completedAt = (status == LearningStatus.DONE) ? Instant.now() : null;
    }

    public void markStudiedNow() {
        this.lastStudiedAt = Instant.now();
    }

    public static LearningProgress newOf(Account accountRef, Term termRef) {
        LearningProgress p = new LearningProgress();
        p.account = accountRef;
        p.term = termRef;
        p.status = LearningStatus.LEARNING;
        p.completedAt = null;
        return p;
    }
}
