package com.cygnus.ipoten.wordbook.entity;

import com.cygnus.ipoten.account.entity.Account;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.Locale;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "wordbook",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_wordbook_owner_normalized",
                columnNames = {"account_id", "normalized_wordbook_name"}
        )
)
public class Wordbook {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, foreignKey = @ForeignKey(name = "FK_swf_account_cascade"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Account account;

    @Setter
    @Column(name = "wordbook_name", nullable = false, length = 50)
    private String wordbookName;

    @Column(name = "normalized_wordbook_name", nullable = false, length = 50)
    private String normalizedWordbookName;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = true)
    private Instant updatedAt;

    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    @PrePersist
    protected void onCreate() {
        final Instant now = Instant.now();
        if (this.createdAt == null) this.createdAt = now;
        this.updatedAt = now;
        if (this.sortOrder == null) this.sortOrder = 0;
        ensureNormalized();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
        ensureNormalized();
    }

    private void ensureNormalized() {
        if (this.wordbookName != null) {
            this.normalizedWordbookName = normalize(this.wordbookName);
        }
    }

    private static String normalize(String s) {
        return s.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    public Wordbook(Account account, String wordbookName, Integer sortOrder) {
        this.account = account;
        this.wordbookName = wordbookName;
        this.sortOrder = sortOrder;
    }

    public Wordbook(Account account, String wordbookName, Integer sortOrder, String normalized) {
        this.account = account;
        this.wordbookName = wordbookName;
        this.normalizedWordbookName = normalized;
        this.sortOrder = sortOrder;
    }
}
