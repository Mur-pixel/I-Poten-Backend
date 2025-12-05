package com.cygnus.ipoten.wordbook.entity;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.term.entity.Term;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;

@Getter
@Entity
@Table(
        name = "wordbook_term",
        uniqueConstraints = @UniqueConstraint(name="uk_owner_folder_term", columnNames = {"account_id","folder_id","term_id"}),
        indexes = {
                @Index(name="idx_uwt_folder", columnList="folder_id"),
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
    @JoinColumn(name = "folder_id", nullable = false, foreignKey = @ForeignKey(name = "FK_uwt_folder_cascade"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private WordbookFolder folder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id", nullable = false, foreignKey = @ForeignKey(name = "FK_uwt_term_restrict"))
    private Term term;
    
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0; // 정렬 순서

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @LastModifiedDate
    @Column(
            name = "updated_at",
            nullable = false,
            columnDefinition = "DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)"
    )
    private Instant updatedAt;

    public WordbookTerm(Account account, WordbookFolder folder, Term term) {
        this.account = account;
        this.folder = folder;
        this.term = term;
    }

    // 정렬 순서를 포함하는 생성자
    public WordbookTerm(Account account, WordbookFolder folder, Term term, Integer sortOrder) {
        this.account = account;
        this.folder = folder;
        this.term = term;
        this.sortOrder = (sortOrder == null ? 0 : sortOrder);
    }

    // 편의 팩토리: folder에서 account를 자동으로 가져옴
    public static WordbookTerm of(WordbookFolder folder, Term term, int sortOrder) {
        return new WordbookTerm(folder.getAccount(), folder, term, sortOrder);
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        syncAccountFromFolder();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
        syncAccountFromFolder();
    }

    void syncAccountFromFolder() {
        if (this.folder != null) this.account = this.folder.getAccount();
    }
}
