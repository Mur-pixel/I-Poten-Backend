package com.cygnus.ipoten.wordbook_term.entity;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.term.entity.Term;
import com.cygnus.ipoten.wordbook.entity.Wordbook;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Entity
@Table(
        name = "wordbook_term",
        uniqueConstraints = @UniqueConstraint(name="uk_owner_wordbook_term", columnNames = {"account_id","wordbook_id","term_id"}),
        indexes = {
                @Index(name="idx_uwt_wordbook", columnList="wordbook_id"),
                @Index(name="idx_uwt_term", columnList="term_id")
        }
)
@NoArgsConstructor
public class WordbookTerm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wordbook_id", nullable = false, foreignKey = @ForeignKey(name = "FK_uwt_wordbook_cascade"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Wordbook wordbook;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id", nullable = false, foreignKey = @ForeignKey(name = "FK_uwt_term_restrict"))
    private Term term;
    
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0; // 정렬 순서

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(
            name = "updated_at",
            nullable = false,
            columnDefinition = "DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)"
    )
    private Instant updatedAt;

    public WordbookTerm(Account account, Wordbook wordbook, Term term) {
        this.account = account;
        this.wordbook = wordbook;
        this.term = term;
    }

    // 정렬 순서를 포함하는 생성자
    public WordbookTerm(Account account, Wordbook wordbook, Term term, Integer sortOrder) {
        this.account = account;
        this.wordbook = wordbook;
        this.term = term;
        this.sortOrder = (sortOrder == null ? 0 : sortOrder);
    }

    // 편의 팩토리: wordbook에서 account를 자동으로 가져옴
    public static WordbookTerm of(Wordbook wordbook, Term term, int sortOrder) {
        return new WordbookTerm(wordbook.getAccount(), wordbook, term, sortOrder);
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        syncAccountFromWordbook();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
        syncAccountFromWordbook();
    }

    void syncAccountFromWordbook() {
        if (this.wordbook != null) this.account = this.wordbook.getAccount();
    }
}
